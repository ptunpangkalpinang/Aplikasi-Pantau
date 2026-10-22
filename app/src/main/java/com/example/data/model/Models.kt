package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
  INCOME,
  EXPENSE,
  SAVINGS_DEPOSIT,
  SAVINGS_WITHDRAWAL
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
  val updatedAt: Long = System.currentTimeMillis()
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
