package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PantauDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.PantauRepository
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddIncomeDialog
import com.example.ui.components.BudgetDialog
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.SavingsDepositWithdrawDialog
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SavingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PantauNavy
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauTheme
import com.example.ui.viewmodel.PantauViewModel
import com.example.ui.viewmodel.PantauViewModelFactory
import kotlinx.coroutines.launch

enum class PantauScreen(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  HOME("Beranda", Icons.Filled.Home, Icons.Outlined.Home),
  TRANSACTIONS("Transaksi", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
  SAVINGS("Tabungan", Icons.Filled.Savings, Icons.Outlined.Savings),
  BUDGET("Anggaran", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
  REPORTS("Laporan", Icons.Filled.BarChart, Icons.Outlined.BarChart),
  SETTINGS("Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = PantauDatabase.getDatabase(applicationContext, lifecycleScope)
    val repository = PantauRepository(
      database.transactionDao(),
      database.savingsGoalDao(),
      database.budgetDao(),
      database.categoryDao(),
      database.preferenceDao()
    )

    setContent {
      val viewModel: PantauViewModel = viewModel(
        factory = PantauViewModelFactory(repository)
      )
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      val themePref = uiState.preferences["theme_mode"] ?: "SYSTEM"
      val darkTheme = when (themePref) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
      }

      PantauTheme(darkTheme = darkTheme) {
        PantauMainApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun PantauMainApp(viewModel: PantauViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var currentScreen by remember { mutableStateOf(PantauScreen.HOME) }
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  // Handle messages in Snackbar
  LaunchedEffect(uiState.userMessage) {
    uiState.userMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearUserMessage()
    }
  }

  // Handle Back press: navigate to HOME before exiting
  BackHandler(enabled = currentScreen != PantauScreen.HOME) {
    currentScreen = PantauScreen.HOME
  }

  // Dialog states
  var showIncomeDialog by remember { mutableStateOf(false) }
  var editingIncomeTx by remember { mutableStateOf<TransactionEntity?>(null) }

  var showExpenseDialog by remember { mutableStateOf(false) }
  var editingExpenseTx by remember { mutableStateOf<TransactionEntity?>(null) }

  var depositGoalTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }
  var withdrawGoalTarget by remember { mutableStateOf<SavingsGoalEntity?>(null) }

  var showAddGoalDialog by remember { mutableStateOf(false) }
  var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

  var showAddBudgetDialog by remember { mutableStateOf(false) }
  var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

  var deletingTx by remember { mutableStateOf<TransactionEntity?>(null) }
  var deletingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
  var deletingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

  val allowNegative = uiState.preferences["allow_negative_balance"]?.toBooleanStrictOrNull() ?: true

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isTablet = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (!isTablet) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp
          ) {
            PantauScreen.entries.forEach { screen ->
              val isSelected = currentScreen == screen
              NavigationBarItem(
                selected = isSelected,
                onClick = { currentScreen = screen },
                icon = {
                  Icon(
                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                    contentDescription = screen.title,
                    modifier = Modifier.size(22.dp)
                  )
                },
                label = {
                  Text(
                    text = screen.title,
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = PantauPrimaryBlue,
                  selectedTextColor = PantauPrimaryBlue,
                  indicatorColor = PantauPrimaryBlue.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (currentScreen == PantauScreen.TRANSACTIONS) {
          FloatingActionButton(
            onClick = { showExpenseDialog = true },
            containerColor = PantauPrimaryBlue,
            contentColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("fab_add_tx")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
          }
        }
      }
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isTablet) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
          ) {
            PantauScreen.entries.forEach { screen ->
              val isSelected = currentScreen == screen
              NavigationRailItem(
                selected = isSelected,
                onClick = { currentScreen = screen },
                icon = {
                  Icon(
                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                    contentDescription = screen.title
                  )
                },
                label = { Text(screen.title) }
              )
            }
          }
        }

        AnimatedContent(
          targetState = currentScreen,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          modifier = Modifier.weight(1f),
          label = "screen_transition"
        ) { targetScreen ->
          when (targetScreen) {
            PantauScreen.HOME -> HomeScreen(
              uiState = uiState,
              onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
              onAddIncome = { showIncomeDialog = true },
              onAddExpense = { showExpenseDialog = true },
              onDepositSavings = {
                if (uiState.goals.isNotEmpty()) {
                  depositGoalTarget = uiState.goals.first()
                } else {
                  showAddGoalDialog = true
                }
              },
              onNavigateToTransactions = { currentScreen = PantauScreen.TRANSACTIONS },
              onNavigateToSavings = { currentScreen = PantauScreen.SAVINGS },
              onNavigateToBudgets = { currentScreen = PantauScreen.BUDGET },
              onDepositGoal = { goal -> depositGoalTarget = goal },
              onWithdrawGoal = { goal -> withdrawGoalTarget = goal },
              onEditGoal = { goal ->
                editingGoal = goal
                showAddGoalDialog = true
              },
              onDeleteGoal = { goal -> deletingGoal = goal },
              onEditTx = { tx ->
                if (tx.type == TransactionType.INCOME.name) {
                  editingIncomeTx = tx
                  showIncomeDialog = true
                } else if (tx.type == TransactionType.EXPENSE.name) {
                  editingExpenseTx = tx
                  showExpenseDialog = true
                }
              },
              onDeleteTx = { tx -> deletingTx = tx },
              onApplyDemoData = { viewModel.applyDemoData() }
            )

            PantauScreen.TRANSACTIONS -> TransactionsScreen(
              uiState = uiState,
              onSearchQueryChange = { viewModel.setSearchQuery(it) },
              onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
              onTypeFilterChange = { viewModel.setTypeFilter(it) },
              onSortOrderChange = { viewModel.setSortOrder(it) },
              onEditTx = { tx ->
                if (tx.type == TransactionType.INCOME.name) {
                  editingIncomeTx = tx
                  showIncomeDialog = true
                } else if (tx.type == TransactionType.EXPENSE.name) {
                  editingExpenseTx = tx
                  showExpenseDialog = true
                }
              },
              onDeleteTx = { tx -> deletingTx = tx }
            )

            PantauScreen.SAVINGS -> SavingsScreen(
              uiState = uiState,
              onAddNewGoal = {
                editingGoal = null
                showAddGoalDialog = true
              },
              onDepositGoal = { goal -> depositGoalTarget = goal },
              onWithdrawGoal = { goal -> withdrawGoalTarget = goal },
              onEditGoal = { goal ->
                editingGoal = goal
                showAddGoalDialog = true
              },
              onDeleteGoal = { goal -> deletingGoal = goal }
            )

            PantauScreen.BUDGET -> BudgetScreen(
              uiState = uiState,
              onAddNewBudget = {
                editingBudget = null
                showAddBudgetDialog = true
              },
              onEditBudget = { budget ->
                editingBudget = budget
                showAddBudgetDialog = true
              },
              onDeleteBudget = { budget -> deletingBudget = budget }
            )

            PantauScreen.REPORTS -> ReportsScreen(
              uiState = uiState,
              onPeriodChange = { viewModel.setReportPeriod(it) },
              onExportCsv = {
                val csv = viewModel.exportCsv()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Pantau_CSV", csv))
                coroutineScope.launch {
                  snackbarHostState.showSnackbar("Data CSV (${uiState.transactions.size} baris) berhasil disalin ke clipboard!")
                }
              },
              onExportJson = {
                coroutineScope.launch {
                  val json = viewModel.exportJson()
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("Pantau_Backup_JSON", json))
                  snackbarHostState.showSnackbar("Cadangan JSON berhasil disalin ke clipboard!")
                }
              }
            )

            PantauScreen.SETTINGS -> SettingsScreen(
              uiState = uiState,
              onSetInitialBalance = { viewModel.setInitialBalance(it) },
              onSetSalaryConfig = { s, a, d -> viewModel.setSalaryConfig(s, a, d) },
              onSetAllowNegativeBalance = { viewModel.setAllowNegativeBalance(it) },
              onSetThemeMode = { viewModel.setThemeMode(it) },
              onAddCategory = { n, t -> viewModel.addCategory(n, t) },
              onDeleteCategory = { viewModel.deleteCategory(it) },
              onApplyDemoData = { viewModel.applyDemoData() },
              onClearDemoData = { viewModel.clearDemoData() },
              onResetAllData = { viewModel.resetAllData() },
              onExportJson = {
                coroutineScope.launch {
                  val json = viewModel.exportJson()
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("Pantau_Backup_JSON", json))
                  snackbarHostState.showSnackbar("Cadangan JSON berhasil disalin ke clipboard!")
                }
              },
              onImportJson = { jsonStr -> viewModel.importJson(jsonStr) }
            )
          }
        }
      }
    }
  }

  // Dialogs
  if (showIncomeDialog) {
    AddIncomeDialog(
      existingTx = editingIncomeTx,
      categories = uiState.categories,
      onDismiss = {
        showIncomeDialog = false
        editingIncomeTx = null
      },
      onSave = { amount, category, source, note, isSalary ->
        if (editingIncomeTx != null) {
          viewModel.updateTransaction(
            editingIncomeTx!!.copy(
              amount = amount,
              category = category,
              source = source,
              note = note,
              isRecurringSalary = isSalary
            )
          )
        } else {
          viewModel.addTransaction(
            type = TransactionType.INCOME,
            amount = amount,
            category = category,
            dateMillis = System.currentTimeMillis(),
            source = source,
            note = note,
            isRecurringSalary = isSalary
          )
        }
        showIncomeDialog = false
        editingIncomeTx = null
      }
    )
  }

  if (showExpenseDialog) {
    AddExpenseDialog(
      existingTx = editingExpenseTx,
      categories = uiState.categories,
      budgets = uiState.budgets,
      availableBalance = uiState.summary.availableBalance,
      allowNegativeBalance = allowNegative,
      onDismiss = {
        showExpenseDialog = false
        editingExpenseTx = null
      },
      onSave = { amount, category, paymentMethod, note ->
        if (editingExpenseTx != null) {
          viewModel.updateTransaction(
            editingExpenseTx!!.copy(
              amount = amount,
              category = category,
              source = paymentMethod,
              note = note
            )
          )
        } else {
          viewModel.addTransaction(
            type = TransactionType.EXPENSE,
            amount = amount,
            category = category,
            dateMillis = System.currentTimeMillis(),
            source = paymentMethod,
            note = note
          )
        }
        showExpenseDialog = false
        editingExpenseTx = null
      }
    )
  }

  if (depositGoalTarget != null) {
    SavingsDepositWithdrawDialog(
      isDeposit = true,
      goal = depositGoalTarget!!,
      availableBalance = uiState.summary.availableBalance,
      allowNegativeBalance = allowNegative,
      onDismiss = { depositGoalTarget = null },
      onConfirm = { amount, note ->
        viewModel.depositToSavings(
          goalId = depositGoalTarget!!.id,
          amount = amount,
          note = note
        )
        depositGoalTarget = null
      }
    )
  }

  if (withdrawGoalTarget != null) {
    SavingsDepositWithdrawDialog(
      isDeposit = false,
      goal = withdrawGoalTarget!!,
      availableBalance = uiState.summary.availableBalance,
      allowNegativeBalance = allowNegative,
      onDismiss = { withdrawGoalTarget = null },
      onConfirm = { amount, note ->
        viewModel.withdrawFromSavings(
          goalId = withdrawGoalTarget!!.id,
          amount = amount,
          note = note
        )
        withdrawGoalTarget = null
      }
    )
  }

  if (showAddGoalDialog) {
    SavingsGoalDialog(
      existingGoal = editingGoal,
      onDismiss = {
        showAddGoalDialog = false
        editingGoal = null
      },
      onSave = { name, iconName, accentColorHex, targetAmount, deadlineDateMillis, routineTargetAmount, routineFrequency, note ->
        if (editingGoal != null) {
          viewModel.updateSavingsGoal(
            editingGoal!!.copy(
              name = name,
              iconName = iconName,
              accentColorHex = accentColorHex,
              targetAmount = targetAmount,
              deadlineDateMillis = deadlineDateMillis,
              routineTargetAmount = routineTargetAmount,
              routineFrequency = routineFrequency,
              note = note
            )
          )
        } else {
          viewModel.addSavingsGoal(
            name = name,
            iconName = iconName,
            accentColorHex = accentColorHex,
            targetAmount = targetAmount,
            deadlineDateMillis = deadlineDateMillis,
            routineTargetAmount = routineTargetAmount,
            routineFrequency = routineFrequency,
            note = note
          )
        }
        showAddGoalDialog = false
        editingGoal = null
      }
    )
  }

  if (showAddBudgetDialog) {
    BudgetDialog(
      existingBudget = editingBudget,
      categories = uiState.categories,
      onDismiss = {
        showAddBudgetDialog = false
        editingBudget = null
      },
      onSave = { name, category, limitAmount ->
        if (editingBudget != null) {
          viewModel.updateBudget(
            editingBudget!!.copy(
              name = name,
              category = category,
              limitAmount = limitAmount
            )
          )
        } else {
          viewModel.addBudget(
            name = name,
            category = category,
            limitAmount = limitAmount
          )
        }
        showAddBudgetDialog = false
        editingBudget = null
      }
    )
  }

  if (deletingTx != null) {
    ConfirmDialog(
      title = "Hapus Transaksi",
      message = "Apakah Anda yakin ingin menghapus transaksi ${deletingTx!!.category} sebesar ${com.example.domain.FinancialEngine.formatRupiah(deletingTx!!.amount)}? Saldo akan disesuaikan kembali secara otomatis.",
      confirmText = "Hapus",
      isDanger = true,
      onConfirm = {
        viewModel.deleteTransaction(deletingTx!!)
        deletingTx = null
      },
      onDismiss = { deletingTx = null }
    )
  }

  if (deletingGoal != null) {
    ConfirmDialog(
      title = "Hapus Tujuan Tabungan",
      message = "Apakah Anda yakin ingin menghapus ${deletingGoal!!.name}? Dana tersimpan (${com.example.domain.FinancialEngine.formatRupiah(deletingGoal!!.currentAmount)}) akan tetap tercatat.",
      confirmText = "Hapus",
      isDanger = true,
      onConfirm = {
        viewModel.deleteSavingsGoal(deletingGoal!!)
        deletingGoal = null
      },
      onDismiss = { deletingGoal = null }
    )
  }

  if (deletingBudget != null) {
    ConfirmDialog(
      title = "Hapus Anggaran",
      message = "Apakah Anda yakin ingin menghapus pos anggaran ${deletingBudget!!.name}?",
      confirmText = "Hapus",
      isDanger = true,
      onConfirm = {
        viewModel.deleteBudget(deletingBudget!!)
        deletingBudget = null
      },
      onDismiss = { deletingBudget = null }
    )
  }
}
