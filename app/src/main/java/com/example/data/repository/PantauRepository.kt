package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.local.CategoryDao
import com.example.data.local.DefaultData
import com.example.data.local.PreferenceDao
import com.example.data.local.SavingsGoalDao
import com.example.data.local.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.FinancialEngine
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PantauRepository(
  private val transactionDao: TransactionDao,
  private val savingsGoalDao: SavingsGoalDao,
  private val budgetDao: BudgetDao,
  private val categoryDao: CategoryDao,
  private val preferenceDao: PreferenceDao
) {
  val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
  val allGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
  val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
  val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
  val allPreferences: Flow<List<PreferenceEntity>> = preferenceDao.getAllPreferences()

  suspend fun insertTransaction(transaction: TransactionEntity) {
    transactionDao.insert(transaction)
  }

  suspend fun updateTransaction(transaction: TransactionEntity) {
    transactionDao.update(transaction)
  }

  suspend fun deleteTransaction(transaction: TransactionEntity) {
    // If it was a savings deposit/withdrawal, reverse the goal currentAmount
    if (transaction.type == TransactionType.SAVINGS_DEPOSIT.name && transaction.savingsGoalId != null) {
      val goal = savingsGoalDao.getGoalById(transaction.savingsGoalId)
      if (goal != null) {
        val updatedAmount = (goal.currentAmount - transaction.amount).coerceAtLeast(0L)
        savingsGoalDao.update(goal.copy(currentAmount = updatedAmount, updatedAt = System.currentTimeMillis()))
      }
    } else if (transaction.type == TransactionType.SAVINGS_WITHDRAWAL.name && transaction.savingsGoalId != null) {
      val goal = savingsGoalDao.getGoalById(transaction.savingsGoalId)
      if (goal != null) {
        val updatedAmount = goal.currentAmount + transaction.amount
        savingsGoalDao.update(goal.copy(currentAmount = updatedAmount, updatedAt = System.currentTimeMillis()))
      }
    }
    transactionDao.delete(transaction)
  }

  suspend fun depositToSavingsGoal(
    goalId: String,
    amount: Long,
    dateMillis: Long = System.currentTimeMillis(),
    source: String = "Transfer Bank",
    note: String = ""
  ) {
    val goal = savingsGoalDao.getGoalById(goalId) ?: return
    val newAmount = goal.currentAmount + amount
    savingsGoalDao.update(
      goal.copy(
        currentAmount = newAmount,
        isCompleted = newAmount >= goal.targetAmount && goal.targetAmount > 0,
        updatedAt = System.currentTimeMillis()
      )
    )

    val tx = TransactionEntity(
      id = UUID.randomUUID().toString(),
      type = TransactionType.SAVINGS_DEPOSIT.name,
      amount = amount,
      category = goal.name,
      dateMillis = dateMillis,
      source = source,
      note = if (note.isBlank()) "Setoran ke ${goal.name}" else note,
      savingsGoalId = goal.id
    )
    transactionDao.insert(tx)
  }

  suspend fun withdrawFromSavingsGoal(
    goalId: String,
    amount: Long,
    dateMillis: Long = System.currentTimeMillis(),
    source: String = "Transfer Bank",
    note: String = ""
  ) {
    val goal = savingsGoalDao.getGoalById(goalId) ?: return
    val newAmount = (goal.currentAmount - amount).coerceAtLeast(0L)
    savingsGoalDao.update(
      goal.copy(
        currentAmount = newAmount,
        isCompleted = newAmount >= goal.targetAmount && goal.targetAmount > 0,
        updatedAt = System.currentTimeMillis()
      )
    )

    val tx = TransactionEntity(
      id = UUID.randomUUID().toString(),
      type = TransactionType.SAVINGS_WITHDRAWAL.name,
      amount = amount,
      category = goal.name,
      dateMillis = dateMillis,
      source = source,
      note = if (note.isBlank()) "Penarikan dari ${goal.name}" else note,
      savingsGoalId = goal.id
    )
    transactionDao.insert(tx)
  }

  suspend fun insertGoal(goal: SavingsGoalEntity) {
    savingsGoalDao.insert(goal)
  }

  suspend fun updateGoal(goal: SavingsGoalEntity) {
    savingsGoalDao.update(goal)
  }

  suspend fun deleteGoal(goal: SavingsGoalEntity) {
    savingsGoalDao.delete(goal)
  }

  suspend fun insertBudget(budget: BudgetEntity) {
    budgetDao.insert(budget)
  }

  suspend fun updateBudget(budget: BudgetEntity) {
    budgetDao.update(budget)
  }

  suspend fun deleteBudget(budget: BudgetEntity) {
    budgetDao.delete(budget)
  }

  suspend fun insertCategory(category: CategoryEntity) {
    categoryDao.insert(category)
  }

  suspend fun deleteCategory(category: CategoryEntity) {
    categoryDao.delete(category)
  }

  suspend fun setPreference(key: String, value: String) {
    preferenceDao.setPreference(PreferenceEntity(key, value))
  }

  suspend fun applyDemoData() {
    val demo = DefaultData.generateDemoData()
    setPreference("initial_balance", demo.initialBalance.toString())
    savingsGoalDao.insertAll(demo.goals)
    transactionDao.insertAll(demo.transactions)
    
    // Add default budgets for demo
    val demoBudgets = listOf(
      BudgetEntity(
        id = "demo_budget_food",
        name = "Anggaran Makan Bulanan",
        category = "Makanan dan Kebutuhan Pokok",
        limitAmount = 2_500_000L
      ),
      BudgetEntity(
        id = "demo_budget_transport",
        name = "Anggaran Bensin & Transport",
        category = "Bensin dan Parkir",
        limitAmount = 1_000_000L
      ),
      BudgetEntity(
        id = "demo_budget_entertainment",
        name = "Anggaran Hiburan",
        category = "Hiburan",
        limitAmount = 800_000L
      )
    )
    budgetDao.insertAll(demoBudgets)
  }

  suspend fun clearDemoData() {
    // Clear demo transactions & goals
    transactionDao.deleteAll()
    savingsGoalDao.deleteAll()
    budgetDao.deleteAll()
    setPreference("initial_balance", "0")
  }

  suspend fun resetAllData() {
    transactionDao.deleteAll()
    savingsGoalDao.deleteAll()
    budgetDao.deleteAll()
    categoryDao.deleteAll()
    preferenceDao.deleteAll()

    // Restore defaults
    categoryDao.insertAll(DefaultData.getInitialCategories())
    setPreference("initial_balance", "0")
    setPreference("salary_source", "Kantor")
    setPreference("expected_salary", "0")
    setPreference("salary_day", "25")
    setPreference("allow_negative_balance", "true")
    setPreference("theme_mode", "SYSTEM")
  }

  suspend fun exportToJson(
    transactions: List<TransactionEntity>,
    goals: List<SavingsGoalEntity>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    preferences: List<PreferenceEntity>
  ): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())

    val txArray = JSONArray()
    for (tx in transactions) {
      val obj = JSONObject()
      obj.put("id", tx.id)
      obj.put("type", tx.type)
      obj.put("amount", tx.amount)
      obj.put("category", tx.category)
      obj.put("dateMillis", tx.dateMillis)
      obj.put("source", tx.source)
      obj.put("note", tx.note)
      obj.put("isRecurringSalary", tx.isRecurringSalary)
      obj.put("savingsGoalId", tx.savingsGoalId ?: "")
      obj.put("createdAt", tx.createdAt)
      txArray.put(obj)
    }
    root.put("transactions", txArray)

    val goalArray = JSONArray()
    for (g in goals) {
      val obj = JSONObject()
      obj.put("id", g.id)
      obj.put("name", g.name)
      obj.put("iconName", g.iconName)
      obj.put("accentColorHex", g.accentColorHex)
      obj.put("targetAmount", g.targetAmount)
      obj.put("currentAmount", g.currentAmount)
      obj.put("startDateMillis", g.startDateMillis)
      obj.put("deadlineDateMillis", g.deadlineDateMillis)
      obj.put("routineTargetAmount", g.routineTargetAmount)
      obj.put("routineFrequency", g.routineFrequency)
      obj.put("note", g.note)
      obj.put("isCompleted", g.isCompleted)
      goalArray.put(obj)
    }
    root.put("goals", goalArray)

    val budgetArray = JSONArray()
    for (b in budgets) {
      val obj = JSONObject()
      obj.put("id", b.id)
      obj.put("name", b.name)
      obj.put("category", b.category)
      obj.put("limitAmount", b.limitAmount)
      budgetArray.put(obj)
    }
    root.put("budgets", budgetArray)

    val prefArray = JSONArray()
    for (p in preferences) {
      val obj = JSONObject()
      obj.put("key", p.key)
      obj.put("value", p.value)
      prefArray.put(obj)
    }
    root.put("preferences", prefArray)

    return root.toString(2)
  }

  suspend fun importFromJson(jsonString: String, replaceAll: Boolean = true): Boolean {
    return try {
      val root = JSONObject(jsonString)
      if (!root.has("transactions") && !root.has("goals")) {
        return false
      }

      if (replaceAll) {
        transactionDao.deleteAll()
        savingsGoalDao.deleteAll()
        budgetDao.deleteAll()
      }

      if (root.has("goals")) {
        val goalArray = root.getJSONArray("goals")
        val goals = mutableListOf<SavingsGoalEntity>()
        for (i in 0 until goalArray.length()) {
          val obj = goalArray.getJSONObject(i)
          goals.add(
            SavingsGoalEntity(
              id = obj.optString("id", UUID.randomUUID().toString()),
              name = obj.getString("name"),
              iconName = obj.optString("iconName", "Savings"),
              accentColorHex = obj.optString("accentColorHex", "#2563EB"),
              targetAmount = obj.optLong("targetAmount", 0L),
              currentAmount = obj.optLong("currentAmount", 0L),
              startDateMillis = obj.optLong("startDateMillis", System.currentTimeMillis()),
              deadlineDateMillis = obj.optLong("deadlineDateMillis", 0L),
              routineTargetAmount = obj.optLong("routineTargetAmount", 0L),
              routineFrequency = obj.optString("routineFrequency", "MONTHLY"),
              note = obj.optString("note", ""),
              isCompleted = obj.optBoolean("isCompleted", false)
            )
          )
        }
        savingsGoalDao.insertAll(goals)
      }

      if (root.has("transactions")) {
        val txArray = root.getJSONArray("transactions")
        val transactions = mutableListOf<TransactionEntity>()
        for (i in 0 until txArray.length()) {
          val obj = txArray.getJSONObject(i)
          val gId = obj.optString("savingsGoalId", "")
          transactions.add(
            TransactionEntity(
              id = obj.optString("id", UUID.randomUUID().toString()),
              type = obj.getString("type"),
              amount = obj.getLong("amount"),
              category = obj.getString("category"),
              dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
              source = obj.optString("source", "Umum"),
              note = obj.optString("note", ""),
              isRecurringSalary = obj.optBoolean("isRecurringSalary", false),
              savingsGoalId = if (gId.isBlank()) null else gId
            )
          )
        }
        transactionDao.insertAll(transactions)
      }

      if (root.has("budgets")) {
        val bArray = root.getJSONArray("budgets")
        val budgets = mutableListOf<BudgetEntity>()
        for (i in 0 until bArray.length()) {
          val obj = bArray.getJSONObject(i)
          budgets.add(
            BudgetEntity(
              id = obj.optString("id", UUID.randomUUID().toString()),
              name = obj.getString("name"),
              category = obj.optString("category", ""),
              limitAmount = obj.getLong("limitAmount")
            )
          )
        }
        budgetDao.insertAll(budgets)
      }

      if (root.has("preferences")) {
        val pArray = root.getJSONArray("preferences")
        for (i in 0 until pArray.length()) {
          val obj = pArray.getJSONObject(i)
          setPreference(obj.getString("key"), obj.getString("value"))
        }
      }

      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun exportToCsv(transactions: List<TransactionEntity>): String {
    val sb = StringBuilder()
    sb.append("ID,Tipe,Nominal,Kategori,Tanggal,Sumber_Metode,Catatan,Gaji_Rutin,Tujuan_Tabungan\n")
    for (tx in transactions) {
      val dateStr = FinancialEngine.formatDate(tx.dateMillis, "yyyy-MM-dd HH:mm:ss")
      val cleanNote = tx.note.replace(",", " ").replace("\n", " ")
      val cleanSource = tx.source.replace(",", " ")
      sb.append("${tx.id},${tx.type},${tx.amount},\"${tx.category}\",\"$dateStr\",\"$cleanSource\",\"$cleanNote\",${tx.isRecurringSalary},\"${tx.savingsGoalId ?: ""}\"\n")
    }
    return sb.toString()
  }
}
