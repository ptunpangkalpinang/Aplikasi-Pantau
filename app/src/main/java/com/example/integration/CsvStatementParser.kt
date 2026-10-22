package com.example.integration

import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedStatementItem(
  val dateMillis: Long,
  val description: String,
  val amount: Long,
  val rawType: String,
  val rawSource: String
)

object CsvStatementParser {

  private val dateFormatBca = SimpleDateFormat("dd/MM/yyyy", Locale.US)
  private val dateFormatIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val dateFormatSlash = SimpleDateFormat("dd/MM/yy", Locale.US)
  private val dateFormatDash = SimpleDateFormat("dd-MM-yyyy", Locale.US)

  fun parse(csvContent: String, providerId: String): List<ParsedStatementItem> {
    val results = mutableListOf<ParsedStatementItem>()
    val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }

    if (lines.isEmpty()) return emptyList()

    val provider = FinancialProviders.getProvider(providerId)
    val fallbackSource = provider.name

    for ((index, line) in lines.withIndex()) {
      // Skip obvious headers
      if (index == 0 && (line.contains("Tanggal", ignoreCase = true) || line.contains("Date", ignoreCase = true))) {
        continue
      }
      if (line.startsWith("#") || line.startsWith("//")) continue

      val columns = parseCsvLine(line)
      if (columns.size < 3) continue

      val parsed = when (providerId.lowercase()) {
        "bca" -> parseBcaLine(columns, fallbackSource)
        "mandiri" -> parseMandiriLine(columns, fallbackSource)
        "dana" -> parseDanaLine(columns, fallbackSource)
        "gopay" -> parseGoPayLine(columns, fallbackSource)
        else -> parseGenericLine(columns, fallbackSource)
      }

      if (parsed != null && parsed.amount > 0) {
        results.add(parsed)
      }
    }

    return results
  }

  private fun parseBcaLine(cols: List<String>, source: String): ParsedStatementItem? {
    // Format: Tanggal, Keterangan, Cabang, Jumlah, Tipe (CR/DB)
    // or: Tanggal, Keterangan, Mutasi, Saldo
    if (cols.size < 3) return null
    val date = parseDate(cols[0])
    val desc = cols[1]
    
    return if (cols.size >= 5) {
      val rawAmount = cleanAmount(cols[3])
      val typeStr = cols[4].trim().uppercase()
      val type = if (typeStr.contains("CR") || typeStr.contains("KREDIT")) "INCOME" else "EXPENSE"
      ParsedStatementItem(date, desc, rawAmount, type, source)
    } else if (cols.size >= 4) {
      val rawAmount = cleanAmount(cols[2])
      val type = if (cols[3].contains("CR", ignoreCase = true)) "INCOME" else "EXPENSE"
      ParsedStatementItem(date, desc, rawAmount, type, source)
    } else {
      val (amt, type) = extractAmountAndType(cols[2])
      ParsedStatementItem(date, desc, amt, type, source)
    }
  }

  private fun parseMandiriLine(cols: List<String>, source: String): ParsedStatementItem? {
    // Format: Tanggal, Deskripsi, Tipe (CR/DB), Nominal
    if (cols.size < 3) return null
    val date = parseDate(cols[0])
    val desc = cols[1]

    return if (cols.size >= 4) {
      val typeStr = cols[2].trim().uppercase()
      val type = if (typeStr.contains("CR") || typeStr.contains("KREDIT")) "INCOME" else "EXPENSE"
      val amt = cleanAmount(cols[3])
      ParsedStatementItem(date, desc, amt, type, source)
    } else {
      val (amt, type) = extractAmountAndType(cols[2])
      ParsedStatementItem(date, desc, amt, type, source)
    }
  }

  private fun parseDanaLine(cols: List<String>, source: String): ParsedStatementItem? {
    if (cols.size < 3) return null
    val date = parseDate(cols[0])
    val desc = cols[1]
    val amt2 = cleanAmount(cols[2])
    val amt3 = if (cols.size >= 4) cleanAmount(cols[3]) else 0L
    val amt = if (amt3 > 0L && amt2 == 0L) amt3 else amt2
    val fullLine = cols.joinToString(" ").uppercase()
    val isCredit = cols.any { it.contains("+") } || fullLine.contains("INCOME") || fullLine.contains("CR") || fullLine.contains("MASUK") || fullLine.contains("TOP UP")
    val type = if (isCredit) "INCOME" else "EXPENSE"
    return ParsedStatementItem(date, desc, amt, type, source)
  }

  private fun parseGoPayLine(cols: List<String>, source: String): ParsedStatementItem? {
    // Format: Tanggal, Deskripsi, Nominal (+/-)
    if (cols.size < 2) return null
    val date = parseDate(cols[0])
    val desc = cols[1]
    val amtStr = if (cols.size >= 3) cols[2] else "0"
    val isCredit = amtStr.contains("+") || amtStr.contains("Top Up", ignoreCase = true)
    val amt = cleanAmount(amtStr)
    val type = if (isCredit) "INCOME" else "EXPENSE"
    return ParsedStatementItem(date, desc, amt, type, source)
  }

  private fun parseGenericLine(cols: List<String>, source: String): ParsedStatementItem? {
    // Tanggal, Keterangan, Nominal, Tipe
    if (cols.size < 3) return null
    val date = parseDate(cols[0])
    val desc = cols[1]
    val amt = cleanAmount(cols[2])
    val type = if (cols.size >= 4) {
      val t = cols[3].trim().uppercase()
      if (t.contains("CR") || t.contains("MASUK") || t.contains("INCOME")) "INCOME" else "EXPENSE"
    } else {
      if (cols[2].contains("+")) "INCOME" else "EXPENSE"
    }
    return ParsedStatementItem(date, desc, amt, type, source)
  }

  private fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false

    for (c in line) {
      when (c) {
        '"' -> inQuotes = !inQuotes
        ',' -> {
          if (inQuotes) {
            sb.append(c)
          } else {
            tokens.add(sb.toString().trim().removeSurrounding("\""))
            sb.clear()
          }
        }
        ';' -> {
          // Also support semicolon delimiter
          if (inQuotes) {
            sb.append(c)
          } else {
            tokens.add(sb.toString().trim().removeSurrounding("\""))
            sb.clear()
          }
        }
        else -> sb.append(c)
      }
    }
    tokens.add(sb.toString().trim().removeSurrounding("\""))
    return tokens
  }

  private fun parseDate(dateStr: String): Long {
    val clean = dateStr.trim().removeSurrounding("\"")
    val formats = listOf(dateFormatBca, dateFormatIso, dateFormatDash, dateFormatSlash)
    for (fmt in formats) {
      try {
        val parsed = fmt.parse(clean)
        if (parsed != null) return parsed.time
      } catch (_: Exception) {
      }
    }
    return System.currentTimeMillis()
  }

  private fun cleanAmount(amountStr: String): Long {
    val clean = amountStr.replace("Rp", "", ignoreCase = true)
      .replace(".", "")
      .replace(",", "")
      .replace("+", "")
      .replace("-", "")
      .replace(" ", "")
      .trim()
    return clean.toLongOrNull() ?: 0L
  }

  private fun extractAmountAndType(str: String): Pair<Long, String> {
    val upper = str.uppercase()
    val isIncome = upper.contains("CR") || upper.contains("+") || upper.contains("MASUK")
    val amt = cleanAmount(str)
    return Pair(amt, if (isIncome) "INCOME" else "EXPENSE")
  }

  fun getSampleCsv(providerId: String): String {
    return when (providerId.lowercase()) {
      "bca" -> """
Tanggal,Keterangan,Cabang,Jumlah,Tipe
20/10/2026,TRSF E-BANKING CR GAJI PT TEKNOLOGI,0001,8500000,CR
21/10/2026,QRIS INDOMARET JAKARTA,0001,45000,DB
22/10/2026,TRANSFER KE DANA TOP UP,0001,200000,DB
23/10/2026,TAGIHAN LISTRIK PLN PRABAYAR,0001,150000,DB
24/10/2026,SETORAN TUNAI MANDIRI CR,0001,500000,CR
""".trimIndent()

      "dana" -> """
Tanggal,Keterangan,Nominal,Tipe
2026-10-21,Top Up dari BCA,+200000,INCOME
2026-10-21,Pembayaran Merchant Kopi Kenangan,-28000,EXPENSE
2026-10-22,Beli Pulsa Telkomsel 50k,-52000,EXPENSE
2026-10-23,Kirim Uang ke Teman OVO,-100000,EXPENSE
""".trimIndent()

      "gopay" -> """
Tanggal,Keterangan,Nominal
2026-10-21,GoFood Nasi Padang Sederhana,-35000
2026-10-22,GoCar Menuju Kantor Mega Kuningan,-42000
2026-10-23,Top Up via Virtual Account BCA,+300000
2026-10-24,Voucher Game Google Play,-25000
""".trimIndent()

      "mandiri" -> """
Tanggal,Deskripsi,Tipe,Nominal
2026-10-20,GAJI BULANAN OKTOBER,CR,7500000
2026-10-21,SPBU PERTAMINA KUNINGAN,DB,100000
2026-10-22,TOKOPEDIA PEMBELIAN BUKU,DB,85000
2026-10-23,TRANSFER ANTAR REKENING TABUNGAN,DB,500000
""".trimIndent()

      else -> """
Tanggal,Keterangan,Nominal,Tipe
2026-10-21,Pemasukan Honor Proyek,1500000,CR
2026-10-22,Belanja Bulanan Supermarket,320000,DB
2026-10-23,Transfer Antar Akun E-Wallet,100000,DB
2026-10-24,Bayar WiFi IndiHome,375000,DB
""".trimIndent()
    }
  }
}
