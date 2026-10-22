package com.example.integration

import com.example.data.model.CategoryRuleEntity
import com.example.data.model.ReviewStatus
import com.example.data.model.TransactionType

data class CategorizationResult(
  val suggestedType: TransactionType,
  val suggestedCategory: String,
  val isInternalTransfer: Boolean,
  val isSalary: Boolean,
  val reviewStatus: ReviewStatus,
  val confidenceScore: Float // 0.0 to 1.0
)

object TransactionCategorizer {

  private val defaultRules = listOf(
    // Food & Dining
    RulePattern(
      keywords = listOf("gofood", "grabfood", "shopeefood", "mcdonald", "kfc", "hokben", "kopi", "cafe", "warung", "resto", "burger", "bakso", "starbucks", "indomaret", "alfamart", "alfamidi", "superindo", "hypermart"),
      category = "Makanan dan Kebutuhan Pokok",
      type = TransactionType.EXPENSE
    ),
    // Transport & Fuel
    RulePattern(
      keywords = listOf("pertamina", "shell", "spbu", "gojek", "goride", "gocar", "grab", "maxim", "bluebird", "kereta", "kai", "commuter", "mrt", "transjakarta", "parkir", "toll", "jasamarga"),
      category = "Transportasi",
      type = TransactionType.EXPENSE
    ),
    // Groceries & Shopping
    RulePattern(
      keywords = listOf("tokopedia", "shopee", "lazada", "blibli", "tiktok shop", "zalora", "uniqlo", "zara", "h&m", "gramedia", "ikea", "ace hardware", "pembelian"),
      category = "Belanja Bulanan",
      type = TransactionType.EXPENSE
    ),
    // Utilities & Bills
    RulePattern(
      keywords = listOf("pln", "listrik", "token pln", "pdam", "air bersih", "bpjs", "pajak"),
      category = "Tagihan Listrik dan Air",
      type = TransactionType.EXPENSE
    ),
    // Internet & Mobile
    RulePattern(
      keywords = listOf("telkomsel", "indosat", "xl axiata", "tri", "smartfren", "indihome", "myrepublic", "biznet", "first media", "pulsa", "paket data"),
      category = "Internet dan Pulsa",
      type = TransactionType.EXPENSE
    ),
    // Entertainment & Subscriptions
    RulePattern(
      keywords = listOf("netflix", "spotify", "youtube", "disney", "steam", "playstation", "cinema", "xxi", "cgv", "google play", "apple store"),
      category = "Hiburan",
      type = TransactionType.EXPENSE
    ),
    // Installments & Loans
    RulePattern(
      keywords = listOf("kredivo", "akulaku", "spaylater", "gopaylater", "kartu kredit", "cicilan", "angsuran", "leasing", "adira", "fif"),
      category = "Cicilan",
      type = TransactionType.EXPENSE
    ),
    // Health
    RulePattern(
      keywords = listOf("apotek", "kimia farma", "halodoc", "alodokter", "klinik", "rumah sakit", "lab", "prodia", "k24"),
      category = "Kesehatan",
      type = TransactionType.EXPENSE
    ),
    // Salary & Income
    RulePattern(
      keywords = listOf("gaji", "payroll", "salary", "upah", "honor", "tunjangan", "thr", "dividen"),
      category = "Gaji Pokok",
      type = TransactionType.INCOME
    ),
    // Freelance & Business
    RulePattern(
      keywords = listOf("klien", "freelance", "proyek", "penjualan", "hasil usaha", "omzet", "invoice"),
      category = "Freelance",
      type = TransactionType.INCOME
    )
  )

  private val internalTransferKeywords = listOf(
    "top up", "topup", "transfer ke rekening sendiri", "rekening sendiri", "antar rekening",
    "bca to dana", "mandiri to gopay", "bri to ovo", "pindah dana", "isi saldo"
  )

  fun categorize(
    rawDescription: String,
    merchantName: String,
    rawType: String? = null, // "CR", "DB", "INCOME", "EXPENSE", etc.
    customRules: List<CategoryRuleEntity> = emptyList()
  ): CategorizationResult {
    val textToAnalyze = "$rawDescription $merchantName".lowercase()

    // 1. Check if user explicitly defined custom rule
    for (rule in customRules) {
      if (textToAnalyze.contains(rule.keyword.lowercase())) {
        val type = if (rule.transactionType == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        return CategorizationResult(
          suggestedType = type,
          suggestedCategory = rule.targetCategory,
          isInternalTransfer = false,
          isSalary = rule.targetCategory.contains("Gaji", ignoreCase = true),
          reviewStatus = ReviewStatus.CONFIRMED,
          confidenceScore = 1.0f
        )
      }
    }

    // 2. Check if internal transfer
    val isTransfer = internalTransferKeywords.any { textToAnalyze.contains(it) }
    if (isTransfer) {
      return CategorizationResult(
        suggestedType = TransactionType.TRANSFER_INTERNAL,
        suggestedCategory = "Transfer Internal",
        isInternalTransfer = true,
        isSalary = false,
        reviewStatus = ReviewStatus.CONFIRMED,
        confidenceScore = 0.95f
      )
    }

    // 3. Check default rule patterns
    for (pattern in defaultRules) {
      for (kw in pattern.keywords) {
        if (textToAnalyze.contains(kw)) {
          // If rawType explicitly specifies CR (Credit = Income) or DB (Debit = Expense), align accordingly
          val finalType = when {
            rawType.equals("CR", ignoreCase = true) || rawType.equals("INCOME", ignoreCase = true) -> TransactionType.INCOME
            rawType.equals("DB", ignoreCase = true) || rawType.equals("EXPENSE", ignoreCase = true) -> TransactionType.EXPENSE
            else -> pattern.type
          }

          val isSalary = pattern.category.contains("Gaji", ignoreCase = true)
          return CategorizationResult(
            suggestedType = finalType,
            suggestedCategory = pattern.category,
            isInternalTransfer = false,
            isSalary = isSalary,
            reviewStatus = ReviewStatus.CONFIRMED,
            confidenceScore = 0.85f
          )
        }
      }
    }

    // 4. Fallback based on rawType if available
    val fallbackType = if (rawType.equals("CR", ignoreCase = true) || rawType.equals("INCOME", ignoreCase = true)) {
      TransactionType.INCOME
    } else {
      TransactionType.EXPENSE
    }

    // When uncertain, flag as PENDING_REVIEW as required by PRD Section 6
    return CategorizationResult(
      suggestedType = fallbackType,
      suggestedCategory = "Perlu Ditinjau",
      isInternalTransfer = false,
      isSalary = false,
      reviewStatus = ReviewStatus.PENDING_REVIEW,
      confidenceScore = 0.3f
    )
  }
}

private data class RulePattern(
  val keywords: List<String>,
  val category: String,
  val type: TransactionType
)
