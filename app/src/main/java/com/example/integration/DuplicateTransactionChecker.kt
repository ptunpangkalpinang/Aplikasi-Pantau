package com.example.integration

import com.example.data.model.TransactionEntity
import kotlin.math.abs

enum class DuplicateCheckResult {
  UNIQUE,
  EXACT_DUPLICATE,
  POSSIBLE_MANUAL_MATCH
}

object DuplicateTransactionChecker {

  fun check(
    incomingSourceTxId: String?,
    incomingDateMillis: Long,
    incomingAmount: Long,
    incomingNote: String,
    incomingConnectionId: String?,
    existingTransactions: List<TransactionEntity>
  ): DuplicateCheckResult {
    // 1. Primary check by sourceTransactionId
    if (!incomingSourceTxId.isNullOrBlank()) {
      val foundBySourceId = existingTransactions.any { it.sourceTransactionId == incomingSourceTxId }
      if (foundBySourceId) {
        return DuplicateCheckResult.EXACT_DUPLICATE
      }
    }

    // 2. Check by connectionId + dateMillis + amount (within 1 hour / 3,600,000ms)
    val sameConnectionMatch = existingTransactions.any { tx ->
      tx.connectionId == incomingConnectionId &&
        tx.amount == incomingAmount &&
        abs(tx.dateMillis - incomingDateMillis) < 3_600_000L
    }
    if (sameConnectionMatch) {
      return DuplicateCheckResult.EXACT_DUPLICATE
    }

    // 3. Check for potential manual duplicate (same amount on same calendar day)
    val manualMatch = existingTransactions.any { tx ->
      tx.connectionId == null &&
        tx.amount == incomingAmount &&
        abs(tx.dateMillis - incomingDateMillis) < 86_400_000L
    }
    if (manualMatch) {
      return DuplicateCheckResult.POSSIBLE_MANUAL_MATCH
    }

    return DuplicateCheckResult.UNIQUE
  }
}
