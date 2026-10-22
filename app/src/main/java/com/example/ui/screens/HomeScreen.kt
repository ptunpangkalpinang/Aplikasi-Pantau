package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.domain.FinancialEngine
import com.example.ui.components.BalanceHeroCard
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.QuickActionRow
import com.example.ui.components.SavingsProgressCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.PantauDangerRed
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauSuccessGreen
import com.example.ui.theme.PantauWarningAmber
import com.example.ui.viewmodel.PantauUiState
import com.example.ui.viewmodel.SalaryStatus

@Composable
fun HomeScreen(
  uiState: PantauUiState,
  onToggleBalanceVisibility: () -> Unit,
  onAddIncome: () -> Unit,
  onAddExpense: () -> Unit,
  onDepositSavings: () -> Unit,
  onNavigateToTransactions: () -> Unit,
  onNavigateToSavings: () -> Unit,
  onNavigateToBudgets: () -> Unit,
  onNavigateToConnections: () -> Unit = {},
  onDepositGoal: (SavingsGoalEntity) -> Unit,
  onWithdrawGoal: (SavingsGoalEntity) -> Unit,
  onEditGoal: (SavingsGoalEntity) -> Unit,
  onDeleteGoal: (SavingsGoalEntity) -> Unit,
  onEditTx: (TransactionEntity) -> Unit,
  onDeleteTx: (TransactionEntity) -> Unit,
  onApplyDemoData: () -> Unit,
  modifier: Modifier = Modifier
) {
  val summary = uiState.summary
  val hasData = uiState.transactions.isNotEmpty() || uiState.goals.isNotEmpty()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("home_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Balance Hero Card
    item {
      BalanceHeroCard(
        summary = summary,
        isVisible = uiState.isBalanceVisible,
        onToggleVisibility = onToggleBalanceVisibility
      )
    }

    // 2. Quick Actions
    item {
      QuickActionRow(
        onAddIncome = onAddIncome,
        onAddExpense = onAddExpense,
        onDepositSavings = onDepositSavings
      )
    }

    // Salary status banner
    val expectedSalary = uiState.preferences["expected_salary"]?.toLongOrNull() ?: 0L
    if (expectedSalary > 0L) {
      item {
        SalaryStatusBanner(
          status = uiState.salaryStatus,
          expectedSalary = expectedSalary,
          actualSalary = uiState.actualSalaryThisMonth,
          salaryDay = uiState.preferences["salary_day"]?.toIntOrNull() ?: 25
        )
      }
    }

    // Financial Connections Card (PRD Section 3)
    item {
      FinancialConnectionsHomeCard(
        connectionsCount = uiState.connections.size,
        connectedProviders = uiState.connections.map { it.providerName },
        pendingReviewCount = uiState.pendingReviewTransactions.size,
        onClick = onNavigateToConnections
      )
    }

    if (!hasData) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .background(Color(0xFFE0F2FE), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Savings, contentDescription = null, tint = PantauPrimaryBlue, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Mulai Kelola Keuangan Anda",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Catat pemasukan atau muat data demo untuk melihat fitur lengkap PANTAU secara langsung.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = onApplyDemoData,
                colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
              ) {
                Text("Gunakan Data Demo")
              }
              OutlinedButton(onClick = onAddIncome) {
                Text("+ Catat Gaji")
              }
            }
          }
        }
      }
    }

    // 3. Mini Financial Trend Chart
    if (summary.totalIncome > 0 || summary.totalExpense > 0) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          IncomeExpenseBarChart(
            income = summary.totalIncome,
            expense = summary.totalExpense
          )
        }
      }
    }

    // 4. Monthly Budget Summary
    if (uiState.budgetComputations.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Anggaran Bulan Ini",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          TextButton(onClick = onNavigateToBudgets) {
            Text("Kelola", color = PantauPrimaryBlue)
          }
        }
      }
      val topBudget = uiState.budgetComputations.first()
      item {
        BudgetProgressCard(
          computation = topBudget,
          onEdit = onNavigateToBudgets,
          onDelete = onNavigateToBudgets
        )
      }
    }

    // 5. Savings Goals Highlight
    if (uiState.goalComputations.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Tujuan Tabungan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          TextButton(onClick = onNavigateToSavings) {
            Text("Semua (${uiState.goals.size})", color = PantauPrimaryBlue)
          }
        }
      }
      val topGoals = uiState.goalComputations.take(2)
      items(topGoals, key = { it.goal.id }) { comp ->
        SavingsProgressCard(
          computation = comp,
          onDeposit = { onDepositGoal(comp.goal) },
          onWithdraw = { onWithdrawGoal(comp.goal) },
          onEdit = { onEditGoal(comp.goal) },
          onDelete = { onDeleteGoal(comp.goal) }
        )
      }
    }

    // 6. Recent Transactions
    if (uiState.transactions.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Transaksi Terbaru",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          TextButton(onClick = onNavigateToTransactions) {
            Text("Lihat Semua", color = PantauPrimaryBlue)
          }
        }
      }
      val recentTxs = uiState.transactions.take(5)
      items(recentTxs, key = { it.id }) { tx ->
        TransactionRowItem(
          transaction = tx,
          onEdit = { onEditTx(tx) },
          onDelete = { onDeleteTx(tx) }
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
fun SalaryStatusBanner(
  status: SalaryStatus,
  expectedSalary: Long,
  actualSalary: Long,
  salaryDay: Int,
  modifier: Modifier = Modifier
) {
  val (bgColor, icon, title, desc) = when (status) {
    SalaryStatus.RECORDED -> Tuple4(
      PantauSuccessGreen.copy(alpha = 0.12f),
      Icons.Default.CheckCircle,
      "Gaji Bulan Ini Telah Tercatat",
      "Total tercatat: ${FinancialEngine.formatRupiah(actualSalary)}"
    )
    SalaryStatus.DIFFERENT_AMOUNT -> Tuple4(
      PantauWarningAmber.copy(alpha = 0.15f),
      Icons.Default.Warning,
      "Nominal Gaji Berbeda dari Perkiraan",
      "Tercatat: ${FinancialEngine.formatRupiah(actualSalary)} (Perkiraan: ${FinancialEngine.formatRupiah(expectedSalary)})"
    )
    SalaryStatus.NOT_RECORDED -> Tuple4(
      Color(0xFFE0F2FE),
      Icons.Default.Info,
      "Gaji Bulan Ini Belum Dicatat",
      "Perkiraan gajian: tgl $salaryDay (${FinancialEngine.formatRupiah(expectedSalary)})"
    )
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = when (status) {
          SalaryStatus.RECORDED -> PantauSuccessGreen
          SalaryStatus.DIFFERENT_AMOUNT -> PantauWarningAmber
          SalaryStatus.NOT_RECORDED -> PantauPrimaryBlue
        },
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = desc,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun FinancialConnectionsHomeCard(
  connectionsCount: Int,
  connectedProviders: List<String>,
  pendingReviewCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(Color(0xFFE0F2FE), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Savings,
          contentDescription = null,
          tint = PantauPrimaryBlue,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Koneksi Keuangan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (pendingReviewCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = PantauWarningAmber
            ) {
              Text(
                text = "$pendingReviewCount tinjau",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
        Text(
          text = if (connectionsCount > 0) {
            "$connectionsCount akun terhubung (${connectedProviders.joinToString(", ")})"
          } else {
            "Hubungkan DANA, e-wallet, atau rekening bank Anda"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }
      Icon(
        imageVector = Icons.Default.ArrowForward,
        contentDescription = "Kelola",
        tint = PantauPrimaryBlue,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

