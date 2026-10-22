package com.example.integration

import com.example.data.model.CategoryRuleEntity
import com.example.data.model.FinancialConnectionEntity
import com.example.data.model.ReviewStatus
import com.example.data.model.SyncLogEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.PantauRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class SyncReport(
  val connectionId: String,
  val providerName: String,
  val totalFetched: Int,
  val successImported: Int,
  val duplicatesSkipped: Int,
  val pendingReviewCount: Int,
  val internalTransfersCount: Int,
  val message: String
)

class FinancialSyncService(
  private val repository: PantauRepository
) {

  suspend fun syncConnection(
    connection: FinancialConnectionEntity,
    customRules: List<CategoryRuleEntity> = emptyList()
  ): SyncReport = withContext(Dispatchers.IO) {
    // 1. Verify connection status
    if (connection.status == "UNAVAILABLE" || connection.status == "DISCONNECTED") {
      return@withContext SyncReport(
        connectionId = connection.id,
        providerName = connection.providerName,
        totalFetched = 0,
        successImported = 0,
        duplicatesSkipped = 0,
        pendingReviewCount = 0,
        internalTransfersCount = 0,
        message = "Koneksi sedang tidak aktif atau belum didukung API otomatis."
      )
    }

    // 2. Fetch mock or sandbox Open Finance transactions
    val rawTransactions = fetchTransactionsForProvider(connection)
    val existingTransactions = repository.getAllTransactionsSnapshot()

    var importedCount = 0
    var duplicateCount = 0
    var pendingReviewCount = 0
    var internalTransferCount = 0

    val transactionsToInsert = mutableListOf<TransactionEntity>()

    for (raw in rawTransactions) {
      // 3. Duplicate check
      val dupCheck = DuplicateTransactionChecker.check(
        incomingSourceTxId = raw.sourceTxId,
        incomingDateMillis = raw.dateMillis,
        incomingAmount = raw.amount,
        incomingNote = raw.description,
        incomingConnectionId = connection.id,
        existingTransactions = existingTransactions
      )

      if (dupCheck == DuplicateCheckResult.EXACT_DUPLICATE) {
        duplicateCount++
        continue
      }

      // 4. Categorize
      val catResult = TransactionCategorizer.categorize(
        rawDescription = raw.description,
        merchantName = raw.merchant,
        rawType = raw.rawType,
        customRules = customRules
      )

      if (catResult.isInternalTransfer) {
        internalTransferCount++
      }
      if (catResult.reviewStatus == ReviewStatus.PENDING_REVIEW) {
        pendingReviewCount++
      }

      val newTx = TransactionEntity(
        id = UUID.randomUUID().toString(),
        type = catResult.suggestedType.name,
        amount = raw.amount,
        category = catResult.suggestedCategory,
        dateMillis = raw.dateMillis,
        source = connection.accountAlias.ifBlank { connection.providerName },
        note = raw.description,
        isRecurringSalary = catResult.isSalary,
        savingsGoalId = null,
        sourceTransactionId = raw.sourceTxId,
        connectionId = connection.id,
        isInternalTransfer = catResult.isInternalTransfer,
        targetAccountId = null,
        reviewStatus = catResult.reviewStatus.name
      )

      transactionsToInsert.add(newTx)
      importedCount++
    }

    // 5. Batch insert valid transactions
    if (transactionsToInsert.isNotEmpty()) {
      repository.insertTransactions(transactionsToInsert)
    }

    // 6. Update connection status
    val updatedConnection = connection.copy(
      status = "SYNC_SUCCESS",
      lastSyncMillis = System.currentTimeMillis(),
      lastSyncStatus = "Sinkronisasi berhasil ($importedCount transaksi baru, $duplicateCount duplikat dilewati)",
      importedTransactionsCount = connection.importedTransactionsCount + importedCount,
      updatedAt = System.currentTimeMillis()
    )
    repository.updateConnection(updatedConnection)

    // 7. Log sync history
    repository.insertSyncLog(
      SyncLogEntity(
        id = UUID.randomUUID().toString(),
        connectionId = connection.id,
        providerName = connection.providerName,
        timestampMillis = System.currentTimeMillis(),
        status = "SUCCESS",
        itemsCount = importedCount,
        notes = "$importedCount baru, $duplicateCount dilewati, $pendingReviewCount perlu ditinjau"
      )
    )

    SyncReport(
      connectionId = connection.id,
      providerName = connection.providerName,
      totalFetched = rawTransactions.size,
      successImported = importedCount,
      duplicatesSkipped = duplicateCount,
      pendingReviewCount = pendingReviewCount,
      internalTransfersCount = internalTransferCount,
      message = "Sinkronisasi selesai: $importedCount baru diimpor, $duplicateCount duplikat dilewati."
    )
  }

  private fun fetchTransactionsForProvider(connection: FinancialConnectionEntity): List<RawProviderTransaction> {
    val now = System.currentTimeMillis()
    val pid = connection.providerId.lowercase()

    return when {
      pid == "dana" -> listOf(
        RawProviderTransaction(
          sourceTxId = "DANA_${connection.id}_101",
          dateMillis = now - 2 * 3600_000L,
          description = "QRIS Kopi Kenangan",
          merchant = "Kopi Kenangan",
          amount = 28000L,
          rawType = "EXPENSE"
        ),
        RawProviderTransaction(
          sourceTxId = "DANA_${connection.id}_102",
          dateMillis = now - 18 * 3600_000L,
          description = "Top up dari Rekening BCA",
          merchant = "BCA Transfer",
          amount = 200000L,
          rawType = "INCOME"
        ),
        RawProviderTransaction(
          sourceTxId = "DANA_${connection.id}_103",
          dateMillis = now - 36 * 3600_000L,
          description = "Pembayaran Pulsa Telkomsel 50k",
          merchant = "Telkomsel",
          amount = 51000L,
          rawType = "EXPENSE"
        )
      )
      pid == "gopay" -> listOf(
        RawProviderTransaction(
          sourceTxId = "GOPAY_${connection.id}_201",
          dateMillis = now - 4 * 3600_000L,
          description = "GoFood Nasi Padang Sederhana",
          merchant = "GoFood",
          amount = 45000L,
          rawType = "EXPENSE"
        ),
        RawProviderTransaction(
          sourceTxId = "GOPAY_${connection.id}_202",
          dateMillis = now - 26 * 3600_000L,
          description = "GoRide Menuju Kantor",
          merchant = "Gojek Transport",
          amount = 18000L,
          rawType = "EXPENSE"
        )
      )
      pid == "bca" -> listOf(
        RawProviderTransaction(
          sourceTxId = "BCA_${connection.id}_301",
          dateMillis = now - 5 * 3600_000L,
          description = "TRSF E-BANKING GAJI PT MAJU GEMILANG",
          merchant = "PT Maju Gemilang",
          amount = 12500000L,
          rawType = "INCOME"
        ),
        RawProviderTransaction(
          sourceTxId = "BCA_${connection.id}_302",
          dateMillis = now - 28 * 3600_000L,
          description = "DEBIT EDC SPBU PERTAMINA 31.12",
          merchant = "Pertamina SPBU",
          amount = 250000L,
          rawType = "EXPENSE"
        ),
        RawProviderTransaction(
          sourceTxId = "BCA_${connection.id}_303",
          dateMillis = now - 48 * 3600_000L,
          description = "TRSF KE REKENING SENDIRI TOP UP DANA",
          merchant = "DANA Topup",
          amount = 200000L,
          rawType = "EXPENSE"
        )
      )
      else -> listOf(
        RawProviderTransaction(
          sourceTxId = "${pid.uppercase()}_${connection.id}_401",
          dateMillis = now - 12 * 3600_000L,
          description = "Transaksi Pembayaran ${connection.providerName}",
          merchant = connection.providerName,
          amount = 75000L,
          rawType = "EXPENSE"
        )
      )
    }
  }
}

data class RawProviderTransaction(
  val sourceTxId: String,
  val dateMillis: Long,
  val description: String,
  val merchant: String,
  val amount: Long,
  val rawType: String
)
