package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
  INCOME,
  EXPENSE,
  SAVINGS_DEPOSIT,
  SAVINGS_WITHDRAWAL,
  TRANSFER_INTERNAL
}

enum class AccountType(val label: String) {
  E_WALLET("E-Wallet"),
  BANK_ACCOUNT("Rekening Bank")
}

enum class ConnectionStatus(val label: String) {
  NOT_CONNECTED("Belum Terhubung"),
  PREPARING("Menyiapkan Koneksi"),
  CONNECTED("Terhubung"),
  SYNCING("Sedang Sinkronisasi"),
  SYNC_SUCCESS("Sinkronisasi Berhasil"),
  NEEDS_ATTENTION("Memerlukan Perhatian"),
  DISCONNECTED("Koneksi Terputus"),
  UNAVAILABLE("Integrasi Belum Tersedia")
}

enum class ApiSupportStatus(val label: String, val description: String) {
  OFFICIAL_OPEN_FINANCE_READY(
    "Open Finance API",
    "Didukung melalui integrasi API Open Finance resmi / Sandbox mitra agregator terdaftar."
  ),
  REQUIRES_BACKEND_AGGREGATOR(
    "Perlu Backend Agregator",
    "Memerlukan kontrak backend agregator keuangan perbankan berlisensi OJK/BI. Tidak menggunakan scraping atau SMS."
  ),
  CSV_MUTATION_ONLY(
    "Impor Mutasi CSV",
    "Penyedia belum membuka API transaksi nasabah perorangan publik. Gunakan fitur Impor Mutasi CSV resmi."
  )
}

enum class ReviewStatus {
  CONFIRMED,
  PENDING_REVIEW
}

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey val id: String,
  val type: String, // TransactionType.name
  val amount: Long, // Integer Rupiah
  val category: String,
  val dateMillis: Long,
  val source: String, // Source or Payment method
  val note: String,
  val isRecurringSalary: Boolean = false,
  val savingsGoalId: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  // Integration fields (Default null / false for backwards compatibility)
  val sourceTransactionId: String? = null,
  val connectionId: String? = null,
  val isInternalTransfer: Boolean = false,
  val targetAccountId: String? = null,
  val reviewStatus: String = ReviewStatus.CONFIRMED.name
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
  @PrimaryKey val id: String,
  val name: String,
  val iconName: String,
  val accentColorHex: String,
  val targetAmount: Long,
  val currentAmount: Long,
  val startDateMillis: Long,
  val deadlineDateMillis: Long = 0L,
  val routineTargetAmount: Long = 0L,
  val routineFrequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, NONE
  val note: String = "",
  val isCompleted: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String, // empty string if general/total budget
  val periodMonth: Int = 0, // 0 for recurring monthly or YYYYMM (e.g. 202610)
  val limitAmount: Long,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val type: String, // "INCOME" or "EXPENSE"
  val isDefault: Boolean = true
)

@Entity(tableName = "app_preferences")
data class PreferenceEntity(
  @PrimaryKey val key: String,
  val value: String
)

@Entity(tableName = "financial_connections")
data class FinancialConnectionEntity(
  @PrimaryKey val id: String,
  val providerId: String, // dana, gopay, ovo, shopeepay, linkaja, bca, bri, bni, mandiri
  val providerName: String,
  val accountType: String, // AccountType.name
  val accountAlias: String, // e.g. "DANA Pribadi", "BCA Tabungan"
  val maskedAccountNumber: String, // e.g. "0812****8890", "123****789"
  val status: String, // ConnectionStatus.name
  val integrationMethod: String, // "OPEN_FINANCE_SANDBOX", "AGGREGATOR_BACKEND", "CSV_MUTATION"
  val lastSyncMillis: Long = 0L,
  val lastSyncStatus: String = "Belum pernah sinkronisasi",
  val importedTransactionsCount: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
  @PrimaryKey val id: String,
  val keyword: String,
  val targetCategory: String,
  val transactionType: String, // "EXPENSE" or "INCOME"
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
  @PrimaryKey val id: String,
  val connectionId: String,
  val providerName: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val status: String, // "SUCCESS", "FAILED", "WARNING"
  val itemsCount: Int = 0,
  val notes: String = ""
)
