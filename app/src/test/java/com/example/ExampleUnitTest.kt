package com.example

import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.BudgetStatus
import com.example.domain.FinancialEngine
import com.example.domain.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialEngineTest {

  @Test
  fun test1_incomeCalculation() {
    val initialBalance = 0L
    val txs = listOf(
      TransactionEntity(
        id = "tx1",
        type = TransactionType.INCOME.name,
        amount = 5_000_000L,
        category = "Gaji Pokok",
        dateMillis = System.currentTimeMillis(),
        source = "Kantor",
        note = "Gaji"
      )
    )
    val goals = emptyList<SavingsGoalEntity>()
    val summary = FinancialEngine.calculateSummary(initialBalance, txs, goals)

    assertEquals(5_000_000L, summary.totalIncome)
    assertEquals(5_000_000L, summary.availableBalance)
    assertEquals(5_000_000L, summary.totalRecordedFunds)
  }

  @Test
  fun test2_expenseCalculation() {
    val initialBalance = 0L
    val txs = listOf(
      TransactionEntity(
        id = "tx1",
        type = TransactionType.INCOME.name,
        amount = 5_000_000L,
        category = "Gaji",
        dateMillis = System.currentTimeMillis(),
        source = "Kantor",
        note = ""
      ),
      TransactionEntity(
        id = "tx2",
        type = TransactionType.EXPENSE.name,
        amount = 1_000_000L,
        category = "Belanja",
        dateMillis = System.currentTimeMillis(),
        source = "Tunai",
        note = ""
      )
    )
    val summary = FinancialEngine.calculateSummary(initialBalance, txs, emptyList())

    assertEquals(1_000_000L, summary.totalExpense)
    assertEquals(4_000_000L, summary.availableBalance)
    assertEquals(4_000_000L, summary.totalRecordedFunds)
  }

  @Test
  fun test3_savingsDepositCalculation() {
    val initialBalance = 0L
    val goal = SavingsGoalEntity(
      id = "goal_nikah",
      name = "Tabungan Nikah",
      iconName = "Heart",
      accentColorHex = "#EC4899",
      targetAmount = 10_000_000L,
      currentAmount = 500_000L,
      startDateMillis = System.currentTimeMillis()
    )
    val txs = listOf(
      TransactionEntity(
        id = "tx1",
        type = TransactionType.INCOME.name,
        amount = 5_000_000L,
        category = "Gaji",
        dateMillis = System.currentTimeMillis(),
        source = "Kantor",
        note = ""
      ),
      TransactionEntity(
        id = "tx2",
        type = TransactionType.EXPENSE.name,
        amount = 1_000_000L,
        category = "Belanja",
        dateMillis = System.currentTimeMillis(),
        source = "Tunai",
        note = ""
      ),
      TransactionEntity(
        id = "tx3",
        type = TransactionType.SAVINGS_DEPOSIT.name,
        amount = 500_000L,
        category = "Tabungan Nikah",
        dateMillis = System.currentTimeMillis(),
        source = "Transfer",
        note = "",
        savingsGoalId = goal.id
      )
    )
    val summary = FinancialEngine.calculateSummary(initialBalance, txs, listOf(goal))

    assertEquals(3_500_000L, summary.availableBalance)
    assertEquals(500_000L, summary.totalSavings)
    assertEquals(4_000_000L, summary.totalRecordedFunds)
  }

  @Test
  fun test4_savingsWithdrawalCalculation() {
    val initialBalance = 0L
    val goal = SavingsGoalEntity(
      id = "goal_nikah",
      name = "Tabungan Nikah",
      iconName = "Heart",
      accentColorHex = "#EC4899",
      targetAmount = 10_000_000L,
      currentAmount = 400_000L, // after withdrawing 100k
      startDateMillis = System.currentTimeMillis()
    )
    val txs = listOf(
      TransactionEntity(
        id = "tx1",
        type = TransactionType.INCOME.name,
        amount = 5_000_000L,
        category = "Gaji",
        dateMillis = System.currentTimeMillis(),
        source = "Kantor",
        note = ""
      ),
      TransactionEntity(
        id = "tx2",
        type = TransactionType.EXPENSE.name,
        amount = 1_000_000L,
        category = "Belanja",
        dateMillis = System.currentTimeMillis(),
        source = "Tunai",
        note = ""
      ),
      TransactionEntity(
        id = "tx3",
        type = TransactionType.SAVINGS_DEPOSIT.name,
        amount = 500_000L,
        category = "Tabungan Nikah",
        dateMillis = System.currentTimeMillis(),
        source = "Transfer",
        note = "",
        savingsGoalId = goal.id
      ),
      TransactionEntity(
        id = "tx4",
        type = TransactionType.SAVINGS_WITHDRAWAL.name,
        amount = 100_000L,
        category = "Tabungan Nikah",
        dateMillis = System.currentTimeMillis(),
        source = "Transfer",
        note = "",
        savingsGoalId = goal.id
      )
    )
    val summary = FinancialEngine.calculateSummary(initialBalance, txs, listOf(goal))

    assertEquals(3_600_000L, summary.availableBalance)
    assertEquals(400_000L, summary.totalSavings)
    assertEquals(4_000_000L, summary.totalRecordedFunds)
  }

  @Test
  fun test7_savingsGoalProgress() {
    val goal = SavingsGoalEntity(
      id = "g1",
      name = "Tabungan Nikah",
      iconName = "Heart",
      accentColorHex = "#EC4899",
      targetAmount = 10_000_000L,
      currentAmount = 2_500_000L,
      startDateMillis = System.currentTimeMillis()
    )
    val comp = FinancialEngine.computeGoal(goal)

    assertEquals(25.0f, comp.percentageAchieved, 0.01f)
    assertEquals(7_500_000L, comp.remainingAmount)
    assertEquals(GoalStatus.IN_PROGRESS, comp.status)
  }

  @Test
  fun test8_budgetProgress() {
    val expenses = listOf(
      TransactionEntity(
        id = "e1",
        type = TransactionType.EXPENSE.name,
        amount = 800_000L,
        category = "Makanan",
        dateMillis = System.currentTimeMillis(),
        source = "QRIS",
        note = ""
      )
    )
    val comp = FinancialEngine.computeBudget(
      budgetId = "b1",
      name = "Anggaran Makan",
      category = "Makanan",
      limitAmount = 1_000_000L,
      expenses = expenses
    )

    assertEquals(80.0f, comp.percentageUsed, 0.01f)
    assertEquals(200_000L, comp.remainingAmount)
    assertEquals(BudgetStatus.WARNING, comp.status)
  }

  @Test
  fun test10_rupiahFormattingAndParsing() {
    assertEquals(5_000_000L, FinancialEngine.parseRupiahInput("5000000"))
    assertEquals(125_000L, FinancialEngine.parseRupiahInput("Rp 125.000"))
    assertTrue(FinancialEngine.formatRupiah(1_000_000L).contains("1.000.000"))
  }
}
