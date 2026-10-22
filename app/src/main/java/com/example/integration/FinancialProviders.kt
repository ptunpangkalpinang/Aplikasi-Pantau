package com.example.integration

import androidx.compose.ui.graphics.Color
import com.example.data.model.AccountType
import com.example.data.model.ApiSupportStatus

data class FinancialProvider(
  val id: String,
  val name: String,
  val accountType: AccountType,
  val colorHex: String,
  val apiSupportStatus: ApiSupportStatus,
  val defaultMaskedPlaceholder: String,
  val officialNotes: String
)

object FinancialProviders {
  val allProviders = listOf(
    FinancialProvider(
      id = "dana",
      name = "DANA",
      accountType = AccountType.E_WALLET,
      colorHex = "#118EEA",
      apiSupportStatus = ApiSupportStatus.OFFICIAL_OPEN_FINANCE_READY,
      defaultMaskedPlaceholder = "0812-****-8890",
      officialNotes = "Didukung melalui Open Finance API / Mitra Agregator Terdaftar & Impor Mutasi CSV."
    ),
    FinancialProvider(
      id = "gopay",
      name = "GoPay",
      accountType = AccountType.E_WALLET,
      colorHex = "#00AED6",
      apiSupportStatus = ApiSupportStatus.OFFICIAL_OPEN_FINANCE_READY,
      defaultMaskedPlaceholder = "0857-****-4321",
      officialNotes = "Didukung melalui Open Finance API / Mitra Agregator & Impor Riwayat Transaksi."
    ),
    FinancialProvider(
      id = "ovo",
      name = "OVO",
      accountType = AccountType.E_WALLET,
      colorHex = "#4C3494",
      apiSupportStatus = ApiSupportStatus.OFFICIAL_OPEN_FINANCE_READY,
      defaultMaskedPlaceholder = "0813-****-5678",
      officialNotes = "Didukung melalui Open Finance Sandbox & Impor Mutasi."
    ),
    FinancialProvider(
      id = "shopeepay",
      name = "ShopeePay",
      accountType = AccountType.E_WALLET,
      colorHex = "#EE4D2D",
      apiSupportStatus = ApiSupportStatus.CSV_MUTATION_ONLY,
      defaultMaskedPlaceholder = "0819-****-9012",
      officialNotes = "API perorangan publik belum dibuka resmi. Tersedia ekspor & impor mutasi CSV bulanan."
    ),
    FinancialProvider(
      id = "linkaja",
      name = "LinkAja",
      accountType = AccountType.E_WALLET,
      colorHex = "#ED1C24",
      apiSupportStatus = ApiSupportStatus.OFFICIAL_OPEN_FINANCE_READY,
      defaultMaskedPlaceholder = "0821-****-3456",
      officialNotes = "Didukung melalui Open Finance API / Mitra BUMN & Mutasi CSV."
    ),
    FinancialProvider(
      id = "bca",
      name = "Bank BCA",
      accountType = AccountType.BANK_ACCOUNT,
      colorHex = "#005EAC",
      apiSupportStatus = ApiSupportStatus.REQUIRES_BACKEND_AGGREGATOR,
      defaultMaskedPlaceholder = "BCA •••• 4821",
      officialNotes = "Memerlukan integrasi backend SNAP BI / Open Banking BCA atau impor mutasi e-statement CSV/XLSX."
    ),
    FinancialProvider(
      id = "bri",
      name = "Bank BRI",
      accountType = AccountType.BANK_ACCOUNT,
      colorHex = "#00529C",
      apiSupportStatus = ApiSupportStatus.REQUIRES_BACKEND_AGGREGATOR,
      defaultMaskedPlaceholder = "BRI •••• 7639",
      officialNotes = "Memerlukan backend agregator SNAP BI berizin OJK atau impor mutasi rekening koran CSV."
    ),
    FinancialProvider(
      id = "bni",
      name = "Bank BNI",
      accountType = AccountType.BANK_ACCOUNT,
      colorHex = "#F15A24",
      apiSupportStatus = ApiSupportStatus.REQUIRES_BACKEND_AGGREGATOR,
      defaultMaskedPlaceholder = "BNI •••• 1928",
      officialNotes = "Mendukung SNAP Open Banking perbankan atau impor mutasi transaksi CSV."
    ),
    FinancialProvider(
      id = "mandiri",
      name = "Bank Mandiri",
      accountType = AccountType.BANK_ACCOUNT,
      colorHex = "#003D79",
      apiSupportStatus = ApiSupportStatus.REQUIRES_BACKEND_AGGREGATOR,
      defaultMaskedPlaceholder = "Mandiri •••• 5520",
      officialNotes = "Mendukung Livin' API / SNAP BI & format ekspor mutasi Livin' CSV."
    ),
    FinancialProvider(
      id = "other_bank",
      name = "Bank Lainnya",
      accountType = AccountType.BANK_ACCOUNT,
      colorHex = "#475569",
      apiSupportStatus = ApiSupportStatus.CSV_MUTATION_ONLY,
      defaultMaskedPlaceholder = "Rekening •••• 9999",
      officialNotes = "Dukung berbagai bank nasional/swasta lainnya melalui impor mutasi standar CSV."
    )
  )

  fun getProvider(providerId: String): FinancialProvider {
    return allProviders.find { it.id.equals(providerId, ignoreCase = true) }
      ?: FinancialProvider(
        id = providerId,
        name = providerId.uppercase(),
        accountType = AccountType.BANK_ACCOUNT,
        colorHex = "#475569",
        apiSupportStatus = ApiSupportStatus.CSV_MUTATION_ONLY,
        defaultMaskedPlaceholder = "Akun •••• 0000",
        officialNotes = "Penyedia kustom melalui impor mutasi transaksi."
      )
  }
}
