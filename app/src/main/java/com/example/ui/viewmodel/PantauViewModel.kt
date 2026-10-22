package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryRuleEntity
import com.example.data.model.FinancialConnectionEntity
import com.example.data.model.ReviewStatus
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.SyncLogEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.PantauRepository
import com.example.domain.BudgetComputation
import com.example.domain.FinancialEngine
import com.example.domain.FinancialSummary
import com.example.domain.GoalComputation
import com.example.integration.CsvStatementParser
import com.example.integration.DuplicateCheckResult
import com.example.integration.DuplicateTransactionChecker
import com.example.integration.FinancialProviders
import com.example.integration.FinancialSyncService
import com.example.integration.SyncReport
import com.example.integration.TransactionCategorizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class ReportPeriod(val title: String) {
  HARI_INI("Hari Ini"),
  MINGGU_INI("Minggu Ini"),
  BULAN_INI("Bulan Ini"),
  BULAN_LALU("Bulan Sebelumnya"),
  TAHUN_INI("Tahun Ini"),
  SEMUA("Semua Waktu")
}

enum class SortOrder(val title: String) {
  NEWEST("Terbaru"),
  OLDEST("Terlama"),
  HIGHEST_AMOUNT("Nominal Tertinggi"),
  LOWEST_AMOUNT("Nominal Terendah")
}

enum class SalaryStatus(val label: String) {
  NOT_RECORDED("Belum Dicatat"),
  RECORDED("Sudah Dicatat"),
  DIFFERENT_AMOUNT("Nominal Berbeda")
}

data class PantauUiState(
  val transactions: List<TransactionEntity> = emptyList(),
  val goals: List<SavingsGoalEntity> = emptyList(),
  val budgets: List<BudgetEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val preferences: Map<String, String> = emptyMap(),
  val connections: List<FinancialConnectionEntity> = emptyList(),
  val categoryRules: List<CategoryRuleEntity> = emptyList(),
  val syncLogs: List<SyncLogEntity> = emptyList(),
  val pendingReviewTransactions: List<TransactionEntity> = emptyList(),
  val summary: FinancialSummary = FinancialSummary(),
  val goalComputations: List<GoalComputation> = emptyList(),
  val budgetComputations: List<BudgetComputation> = emptyList(),
  val isBalanceVisible: Boolean = true,
  val selectedPeriod: ReportPeriod = ReportPeriod.BULAN_INI,
  val searchQuery: String = "",
  val selectedCategoryFilter: String = "",
  val selectedTypeFilter: String = "ALL", // ALL, INCOME, EXPENSE, SAVINGS, INTERNAL_TRANSFER
  val sortOrder: SortOrder = SortOrder.NEWEST,
  val salaryStatus: SalaryStatus = SalaryStatus.NOT_RECORDED,
  val actualSalaryThisMonth: Long = 0L,
  val isSyncing: Boolean = false,
  val activeSyncReport: SyncReport? = null,
  val userMessage: String? = null
)

class PantauViewModel(
  private val repository: PantauRepository
) : ViewModel() {

  private val syncService = FinancialSyncService(repository)

  private val _isBalanceVisible = MutableStateFlow(true)
  private val _selectedPeriod = MutableStateFlow(ReportPeriod.BULAN_INI)
  private val _searchQuery = MutableStateFlow("")
  private val _selectedCategoryFilter = MutableStateFlow("")
  private val _selectedTypeFilter = MutableStateFlow("ALL")
  private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
  private val _isSyncing = MutableStateFlow(false)
  private val _activeSyncReport = MutableStateFlow<SyncReport?>(null)
  private val _userMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<PantauUiState> = combine(
    combine(
      repository.allTransactions,
      repository.allGoals,
      repository.allBudgets,
      repository.allCategories,
      repository.allPreferences
    ) { txs, goals, budgets, categories, prefs ->
      Tuple5(txs, goals, budgets, categories, prefs)
    },
    combine(
      repository.allConnections,
      repository.allCategoryRules,
      repository.recentSyncLogs,
      repository.pendingReviewTransactions
    ) { conns, rules, logs, pendingTxs ->
      Tuple4(conns, rules, logs, pendingTxs)
    },
    combine(
      _isSyncing,
      _activeSyncReport,
      _userMessage
    ) { syncing, report, msg ->
      Triple(syncing, report, msg)
    }
  ) { (txs, goals, budgets, categories, prefs), (conns, rules, logs, pendingTxs), (syncing, report, msg) ->
    val prefMap = prefs.associate { it.key to it.value }
    val initialBalance = prefMap["initial_balance"]?.toLongOrNull() ?: 0L

    val summary = FinancialEngine.calculateSummary(initialBalance, txs, goals)
    val goalComps = goals.map { FinancialEngine.computeGoal(it) }
    val budgetComps = budgets.map { b ->
      FinancialEngine.computeBudget(
        budgetId = b.id,
        name = b.name,
        category = b.category,
        limitAmount = b.limitAmount,
        expenses = txs
      )
    }

    // Check salary status for this month
    val expectedSalary = prefMap["expected_salary"]?.toLongOrNull() ?: 0L
    val now = Calendar.getInstance()
    val thisYear = now.get(Calendar.YEAR)
    val thisMonth = now.get(Calendar.MONTH)

    val salaryTxsThisMonth = txs.filter { tx ->
      if (tx.type != TransactionType.INCOME.name) return@filter false
      val isSalaryCat = tx.category.contains("Gaji", ignoreCase = true) || tx.isRecurringSalary
      val cal = Calendar.getInstance()
      cal.timeInMillis = tx.dateMillis
      isSalaryCat && cal.get(Calendar.YEAR) == thisYear && cal.get(Calendar.MONTH) == thisMonth
    }

    val actualSalary = salaryTxsThisMonth.sumOf { it.amount }
    val salaryStatus = when {
      salaryTxsThisMonth.isEmpty() -> SalaryStatus.NOT_RECORDED
      expectedSalary > 0L && actualSalary != expectedSalary -> SalaryStatus.DIFFERENT_AMOUNT
      else -> SalaryStatus.RECORDED
    }

    PantauUiState(
      transactions = txs,
      goals = goals,
      budgets = budgets,
      categories = categories,
      preferences = prefMap,
      connections = conns,
      categoryRules = rules,
      syncLogs = logs,
      pendingReviewTransactions = pendingTxs,
      summary = summary,
      goalComputations = goalComps,
      budgetComputations = budgetComps,
      isBalanceVisible = _isBalanceVisible.value,
      selectedPeriod = _selectedPeriod.value,
      searchQuery = _searchQuery.value,
      selectedCategoryFilter = _selectedCategoryFilter.value,
      selectedTypeFilter = _selectedTypeFilter.value,
      sortOrder = _sortOrder.value,
      salaryStatus = salaryStatus,
      actualSalaryThisMonth = actualSalary,
      isSyncing = syncing,
      activeSyncReport = report,
      userMessage = msg
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = PantauUiState()
  )

  fun toggleBalanceVisibility() {
    _isBalanceVisible.value = !_isBalanceVisible.value
  }

  fun setReportPeriod(period: ReportPeriod) {
    _selectedPeriod.value = period
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setCategoryFilter(category: String) {
    _selectedCategoryFilter.value = category
  }

  fun setTypeFilter(type: String) {
    _selectedTypeFilter.value = type
  }

  fun setSortOrder(order: SortOrder) {
    _sortOrder.value = order
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun dismissSyncReport() {
    _activeSyncReport.value = null
  }

  fun addTransaction(
    type: TransactionType,
    amount: Long,
    category: String,
    dateMillis: Long,
    source: String,
    note: String,
    isRecurringSalary: Boolean = false,
    savingsGoalId: String? = null
  ) {
    if (amount <= 0) {
      _userMessage.value = "Nominal transaksi harus lebih dari 0"
      return
    }

    viewModelScope.launch {
      val tx = TransactionEntity(
        id = UUID.randomUUID().toString(),
        type = type.name,
        amount = amount,
        category = category.ifBlank { "Umum" },
        dateMillis = dateMillis,
        source = source.ifBlank { "Tunai" },
        note = note,
        isRecurringSalary = isRecurringSalary,
        savingsGoalId = savingsGoalId,
        reviewStatus = ReviewStatus.CONFIRMED.name
      )
      repository.insertTransaction(tx)
      _userMessage.value = "Transaksi berhasil dicatat"
    }
  }

  fun updateTransaction(transaction: TransactionEntity) {
    if (transaction.amount <= 0) {
      _userMessage.value = "Nominal transaksi harus lebih dari 0"
      return
    }
    viewModelScope.launch {
      repository.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
      _userMessage.value = "Transaksi berhasil diperbarui"
    }
  }

  fun deleteTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
      repository.deleteTransaction(transaction)
      _userMessage.value = "Transaksi berhasil dihapus"
    }
  }

  fun depositToSavings(
    goalId: String,
    amount: Long,
    dateMillis: Long = System.currentTimeMillis(),
    source: String = "Transfer Bank",
    note: String = ""
  ) {
    if (amount <= 0) {
      _userMessage.value = "Nominal setoran harus lebih dari 0"
      return
    }
    viewModelScope.launch {
      repository.depositToSavingsGoal(goalId, amount, dateMillis, source, note)
      _userMessage.value = "Setoran tabungan berhasil disimpan"
    }
  }

  fun withdrawFromSavings(
    goalId: String,
    amount: Long,
    dateMillis: Long = System.currentTimeMillis(),
    source: String = "Transfer Bank",
    note: String = ""
  ) {
    if (amount <= 0) {
      _userMessage.value = "Nominal penarikan harus lebih dari 0"
      return
    }
    viewModelScope.launch {
      repository.withdrawFromSavingsGoal(goalId, amount, dateMillis, source, note)
      _userMessage.value = "Penarikan tabungan berhasil disimpan"
    }
  }

  fun addSavingsGoal(
    name: String,
    iconName: String,
    accentColorHex: String,
    targetAmount: Long,
    deadlineDateMillis: Long,
    routineTargetAmount: Long,
    routineFrequency: String,
    note: String
  ) {
    if (name.isBlank()) {
      _userMessage.value = "Nama tabungan tidak boleh kosong"
      return
    }
    viewModelScope.launch {
      val goal = SavingsGoalEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        iconName = iconName,
        accentColorHex = accentColorHex,
        targetAmount = targetAmount,
        currentAmount = 0L,
        startDateMillis = System.currentTimeMillis(),
        deadlineDateMillis = deadlineDateMillis,
        routineTargetAmount = routineTargetAmount,
        routineFrequency = routineFrequency,
        note = note
      )
      repository.insertGoal(goal)
      _userMessage.value = "Tujuan tabungan berhasil dibuat"
    }
  }

  fun updateSavingsGoal(goal: SavingsGoalEntity) {
    viewModelScope.launch {
      repository.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
      _userMessage.value = "Tujuan tabungan berhasil diperbarui"
    }
  }

  fun deleteSavingsGoal(goal: SavingsGoalEntity) {
    viewModelScope.launch {
      repository.deleteGoal(goal)
      _userMessage.value = "Tujuan tabungan berhasil dihapus"
    }
  }

  fun addBudget(name: String, category: String, limitAmount: Long) {
    if (name.isBlank() || limitAmount <= 0) {
      _userMessage.value = "Nama dan batas nominal anggaran harus diisi"
      return
    }
    viewModelScope.launch {
      val budget = BudgetEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        category = category,
        limitAmount = limitAmount
      )
      repository.insertBudget(budget)
      _userMessage.value = "Anggaran berhasil ditambahkan"
    }
  }

  fun updateBudget(budget: BudgetEntity) {
    viewModelScope.launch {
      repository.updateBudget(budget)
      _userMessage.value = "Anggaran berhasil diperbarui"
    }
  }

  fun deleteBudget(budget: BudgetEntity) {
    viewModelScope.launch {
      repository.deleteBudget(budget)
      _userMessage.value = "Anggaran berhasil dihapus"
    }
  }

  fun addCategory(name: String, type: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val id = "cat_${UUID.randomUUID().toString().take(8)}"
      repository.insertCategory(CategoryEntity(id = id, name = name, type = type, isDefault = false))
      _userMessage.value = "Kategori berhasil ditambahkan"
    }
  }

  fun deleteCategory(category: CategoryEntity) {
    viewModelScope.launch {
      repository.deleteCategory(category)
      _userMessage.value = "Kategori berhasil dihapus"
    }
  }

  fun setInitialBalance(amount: Long) {
    viewModelScope.launch {
      repository.setPreference("initial_balance", amount.toString())
      _userMessage.value = "Saldo awal diperbarui"
    }
  }

  fun setSalaryConfig(source: String, amount: Long, day: Int) {
    viewModelScope.launch {
      repository.setPreference("salary_source", source)
      repository.setPreference("expected_salary", amount.toString())
      repository.setPreference("salary_day", day.toString())
      _userMessage.value = "Pengaturan gaji berhasil disimpan"
    }
  }

  fun setAllowNegativeBalance(allow: Boolean) {
    viewModelScope.launch {
      repository.setPreference("allow_negative_balance", allow.toString())
    }
  }

  fun setThemeMode(mode: String) {
    viewModelScope.launch {
      repository.setPreference("theme_mode", mode)
    }
  }

  // --- Financial Connection Actions ---
  fun connectAccount(
    providerId: String,
    alias: String,
    maskedAccountNumber: String,
    method: String = "OPEN_FINANCE_SANDBOX"
  ) {
    viewModelScope.launch {
      val provider = FinancialProviders.getProvider(providerId)
      val connection = FinancialConnectionEntity(
        id = UUID.randomUUID().toString(),
        providerId = provider.id,
        providerName = provider.name,
        accountType = provider.accountType.name,
        accountAlias = alias.ifBlank { "${provider.name} Pribadi" },
        maskedAccountNumber = maskedAccountNumber.ifBlank { provider.defaultMaskedPlaceholder },
        status = "CONNECTED",
        integrationMethod = method,
        lastSyncMillis = 0L,
        lastSyncStatus = "Koneksi berhasil disiapkan. Siap untuk sinkronisasi.",
        importedTransactionsCount = 0
      )
      repository.insertConnection(connection)
      _userMessage.value = "Akun ${provider.name} berhasil dihubungkan"

      // Trigger initial sync automatically
      syncAccount(connection)
    }
  }

  fun disconnectAccount(connection: FinancialConnectionEntity) {
    viewModelScope.launch {
      repository.deleteConnection(connection)
      _userMessage.value = "Koneksi ke ${connection.providerName} berhasil diputuskan"
    }
  }

  fun syncAccount(connection: FinancialConnectionEntity) {
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        val rules = uiState.value.categoryRules
        val report = syncService.syncConnection(connection, rules)
        _activeSyncReport.value = report
        _userMessage.value = "Sinkronisasi ${connection.providerName}: ${report.successImported} transaksi baru diimpor"
      } catch (e: Exception) {
        _userMessage.value = "Gagal sinkronisasi ${connection.providerName}: ${e.message}"
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun syncAllAccounts() {
    viewModelScope.launch {
      val conns = uiState.value.connections
      if (conns.isEmpty()) {
        _userMessage.value = "Belum ada akun yang terhubung"
        return@launch
      }
      _isSyncing.value = true
      var totalImported = 0
      try {
        val rules = uiState.value.categoryRules
        for (conn in conns) {
          val report = syncService.syncConnection(conn, rules)
          totalImported += report.successImported
        }
        _userMessage.value = "Sinkronisasi selesai: Total $totalImported transaksi baru diimpor"
      } catch (e: Exception) {
        _userMessage.value = "Terjadi kesalahan saat sinkronisasi: ${e.message}"
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun importCsvStatement(
    csvContent: String,
    providerId: String,
    connectionId: String? = null
  ) {
    viewModelScope.launch {
      try {
        val parsed = CsvStatementParser.parse(csvContent, providerId)
        if (parsed.isEmpty()) {
          _userMessage.value = "Tidak ada baris transaksi valid yang ditemukan di file CSV"
          return@launch
        }

        val existing = repository.getAllTransactionsSnapshot()
        val rules = uiState.value.categoryRules
        val provider = FinancialProviders.getProvider(providerId)

        val txsToInsert = mutableListOf<TransactionEntity>()
        var imported = 0
        var duplicates = 0

        for (item in parsed) {
          val dup = DuplicateTransactionChecker.check(
            incomingSourceTxId = null,
            incomingDateMillis = item.dateMillis,
            incomingAmount = item.amount,
            incomingNote = item.description,
            incomingConnectionId = connectionId,
            existingTransactions = existing
          )

          if (dup == DuplicateCheckResult.EXACT_DUPLICATE) {
            duplicates++
            continue
          }

          val catResult = TransactionCategorizer.categorize(
            rawDescription = item.description,
            merchantName = "",
            rawType = item.rawType,
            customRules = rules
          )

          val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = catResult.suggestedType.name,
            amount = item.amount,
            category = catResult.suggestedCategory,
            dateMillis = item.dateMillis,
            source = item.rawSource,
            note = item.description,
            isRecurringSalary = catResult.isSalary,
            savingsGoalId = null,
            connectionId = connectionId,
            isInternalTransfer = catResult.isInternalTransfer,
            reviewStatus = catResult.reviewStatus.name
          )
          txsToInsert.add(tx)
          imported++
        }

        if (txsToInsert.isNotEmpty()) {
          repository.insertTransactions(txsToInsert)
        }

        _userMessage.value = "Impor CSV selesai: $imported berhasil diimpor, $duplicates duplikat dilewati."
      } catch (e: Exception) {
        _userMessage.value = "Gagal memproses file CSV: ${e.message}"
      }
    }
  }

  fun confirmPendingTransaction(
    transactionId: String,
    correctedCategory: String,
    correctedType: TransactionType,
    isInternalTransfer: Boolean,
    linkedSavingsGoalId: String? = null,
    saveAsRuleKeyword: String? = null
  ) {
    viewModelScope.launch {
      repository.confirmPendingTransaction(
        transactionId = transactionId,
        correctedCategory = correctedCategory,
        correctedType = correctedType,
        isInternalTransfer = isInternalTransfer,
        linkedSavingsGoalId = linkedSavingsGoalId,
        saveAsRuleKeyword = saveAsRuleKeyword
      )
      _userMessage.value = "Transaksi berhasil dikonfirmasi"
    }
  }

  fun addCategoryRule(keyword: String, targetCategory: String, type: String) {
    if (keyword.isBlank() || targetCategory.isBlank()) return
    viewModelScope.launch {
      repository.insertCategoryRule(
        CategoryRuleEntity(
          id = UUID.randomUUID().toString(),
          keyword = keyword.trim(),
          targetCategory = targetCategory,
          transactionType = type
        )
      )
      _userMessage.value = "Aturan kategori '$keyword' berhasil disimpan"
    }
  }

  fun deleteCategoryRule(rule: CategoryRuleEntity) {
    viewModelScope.launch {
      repository.deleteCategoryRule(rule)
      _userMessage.value = "Aturan kategori dihapus"
    }
  }

  fun applyDemoData() {
    viewModelScope.launch {
      repository.applyDemoData()
      _userMessage.value = "Data demo & akun DANA/BCA berhasil dimuat"
    }
  }

  fun clearDemoData() {
    viewModelScope.launch {
      repository.clearDemoData()
      _userMessage.value = "Data demo berhasil dibersihkan"
    }
  }

  fun resetAllData() {
    viewModelScope.launch {
      repository.resetAllData()
      _userMessage.value = "Semua data berhasil direset"
    }
  }

  suspend fun exportJson(): String {
    val state = uiState.value
    val prefs = state.preferences.map { com.example.data.model.PreferenceEntity(it.key, it.value) }
    return repository.exportToJson(
      transactions = state.transactions,
      goals = state.goals,
      budgets = state.budgets,
      categories = state.categories,
      preferences = prefs,
      connections = state.connections
    )
  }

  fun exportCsv(): String {
    return repository.exportToCsv(uiState.value.transactions)
  }

  fun importJson(jsonString: String) {
    viewModelScope.launch {
      val success = repository.importFromJson(jsonString, replaceAll = true)
      if (success) {
        _userMessage.value = "Cadangan data berhasil dipulihkan"
      } else {
        _userMessage.value = "Gagal membaca file cadangan JSON"
      }
    }
  }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

class PantauViewModelFactory(
  private val repository: PantauRepository
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(PantauViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return PantauViewModel(repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
