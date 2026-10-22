package com.example.domain

import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class BudgetStatus(val label: String) {
  SAFE("Aman"),
  WARNING("Mendekati Batas"),
  LIMIT_REACHED("Batas Tercapai"),
  EXCEEDED("Melebihi Anggaran")
}

enum class GoalStatus(val label: String) {
  NOT_STARTED("Baru Dimulai"),
  IN_PROGRESS("Sedang Berjalan"),
  NEAR_TARGET("Mendekati Target"),
  ACHIEVED("Target Tercapai"),
  EXCEEDED("Melebihi Target"),
  OVERDUE("Melewati Tenggat")
}

data class FinancialSummary(
  val initialBalance: Long = 0L,
  val totalIncome: Long = 0L,
  val totalExpense: Long = 0L,
  val totalSavingsDeposit: Long = 0L,
  val totalSavingsWithdrawal: Long = 0L,
  val totalSavings: Long = 0L,
  val availableBalance: Long = 0L,
  val totalRecordedFunds: Long = 0L,
  val savingsRatePercentage: Float = 0f
)

data class BudgetComputation(
  val budgetId: String,
  val name: String,
  val category: String,
  val limitAmount: Long,
  val spentAmount: Long,
  val remainingAmount: Long,
  val percentageUsed: Float,
  val status: BudgetStatus
)

data class GoalComputation(
  val goal: SavingsGoalEntity,
  val percentageAchieved: Float,
  val visualProgress: Float, // clamped 0..1
  val remainingAmount: Long,
  val estimatedRoutineDeposit: Long,
  val status: GoalStatus
)

object FinancialEngine {
  private val idLocale = Locale("id", "ID")
  private val rupiahFormat: NumberFormat = NumberFormat.getNumberInstance(idLocale)

  fun formatRupiah(amount: Long, includePrefix: Boolean = true): String {
    val formatted = rupiahFormat.format(amount)
    return if (includePrefix) "Rp$formatted" else formatted
  }

  fun parseRupiahInput(raw: String): Long {
    val digitsOnly = raw.filter { it.isDigit() }
    return digitsOnly.toLongOrNull() ?: 0L
  }

  fun formatDate(millis: Long, pattern: String = "dd MMM yyyy"): String {
    val sdf = SimpleDateFormat(pattern, idLocale)
    return sdf.format(Date(millis))
  }

  fun calculateSummary(
    initialBalance: Long,
    transactions: List<TransactionEntity>,
    goals: List<SavingsGoalEntity>
  ): FinancialSummary {
    var totalIncome = 0L
    var totalExpense = 0L
    var totalDeposit = 0L
    var totalWithdrawal = 0L

    for (tx in transactions) {
      when (tx.type) {
        TransactionType.INCOME.name -> totalIncome += tx.amount
        TransactionType.EXPENSE.name -> totalExpense += tx.amount
        TransactionType.SAVINGS_DEPOSIT.name -> totalDeposit += tx.amount
        TransactionType.SAVINGS_WITHDRAWAL.name -> totalWithdrawal += tx.amount
      }
    }

    // Saldo Tersedia = Saldo Awal + Total Pemasukan - Total Pengeluaran - Total Setoran Tabungan + Total Penarikan Tabungan
    val availableBalance = initialBalance + totalIncome - totalExpense - totalDeposit + totalWithdrawal

    // Total Tabungan = Sum of all savings goals currentAmount
    var totalSavings = 0L
    for (goal in goals) {
      if (goal.currentAmount > 0) {
        totalSavings += goal.currentAmount
      }
    }

    // Total Dana Tercatat = Saldo Tersedia + Total Tabungan
    val totalRecordedFunds = availableBalance + totalSavings

    val savingsRate = if (totalIncome > 0) {
      ((totalDeposit.toFloat() / totalIncome.toFloat()) * 100f).coerceIn(0f, 100f)
    } else 0f

    return FinancialSummary(
      initialBalance = initialBalance,
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      totalSavingsDeposit = totalDeposit,
      totalSavingsWithdrawal = totalWithdrawal,
      totalSavings = totalSavings,
      availableBalance = availableBalance,
      totalRecordedFunds = totalRecordedFunds,
      savingsRatePercentage = savingsRate
    )
  }

  fun computeGoal(goal: SavingsGoalEntity, nowMillis: Long = System.currentTimeMillis()): GoalComputation {
    val target = goal.targetAmount
    val current = goal.currentAmount

    val percentage = if (target > 0) {
      (current.toFloat() / target.toFloat()) * 100f
    } else 0f

    val visualProgress = (percentage / 100f).coerceIn(0f, 1f)
    val remaining = max(0L, target - current)

    // Calculate routine deposit needed
    var estimatedRoutine = goal.routineTargetAmount
    if (goal.deadlineDateMillis > nowMillis && remaining > 0) {
      val diffDays = max(1L, (goal.deadlineDateMillis - nowMillis) / 86400000L)
      estimatedRoutine = when (goal.routineFrequency) {
        "DAILY" -> remaining / diffDays
        "WEEKLY" -> {
          val weeks = max(1L, diffDays / 7L)
          remaining / weeks
        }
        "MONTHLY" -> {
          val months = max(1L, diffDays / 30L)
          remaining / months
        }
        else -> estimatedRoutine
      }
    }

    val status = when {
      goal.isCompleted || current == target -> GoalStatus.ACHIEVED
      current > target -> GoalStatus.EXCEEDED
      goal.deadlineDateMillis in 1 until nowMillis && current < target -> GoalStatus.OVERDUE
      current == 0L -> GoalStatus.NOT_STARTED
      percentage >= 80f -> GoalStatus.NEAR_TARGET
      else -> GoalStatus.IN_PROGRESS
    }

    return GoalComputation(
      goal = goal,
      percentageAchieved = percentage,
      visualProgress = visualProgress,
      remainingAmount = remaining,
      estimatedRoutineDeposit = estimatedRoutine,
      status = status
    )
  }

  fun computeBudget(
    budgetId: String,
    name: String,
    category: String,
    limitAmount: Long,
    expenses: List<TransactionEntity>,
    monthPeriod: Int = 0 // 0 for current month
  ): BudgetComputation {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)

    val matchingExpenses = expenses.filter { tx ->
      if (tx.type != TransactionType.EXPENSE.name) return@filter false
      
      // Match month if specified, or default to current month
      val txCal = Calendar.getInstance()
      txCal.timeInMillis = tx.dateMillis
      val isMonthMatch = txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth

      val isCategoryMatch = category.isBlank() || tx.category.equals(category, ignoreCase = true)
      isMonthMatch && isCategoryMatch
    }

    val spent = matchingExpenses.sumOf { it.amount }
    val remaining = limitAmount - spent
    val percentage = if (limitAmount > 0) {
      (spent.toFloat() / limitAmount.toFloat()) * 100f
    } else 0f

    val status = when {
      percentage > 100f -> BudgetStatus.EXCEEDED
      percentage == 100f -> BudgetStatus.LIMIT_REACHED
      percentage >= 75f -> BudgetStatus.WARNING
      else -> BudgetStatus.SAFE
    }

    return BudgetComputation(
      budgetId = budgetId,
      name = name,
      category = category,
      limitAmount = limitAmount,
      spentAmount = spent,
      remainingAmount = remaining,
      percentageUsed = percentage,
      status = status
    )
  }
}
