package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.domain.FinancialEngine
import com.example.ui.components.DonutExpenseChart
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.theme.PantauDangerRed
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauSuccessGreen
import com.example.ui.viewmodel.PantauUiState
import com.example.ui.viewmodel.ReportPeriod
import java.util.Calendar

@Composable
fun ReportsScreen(
  uiState: PantauUiState,
  onPeriodChange: (ReportPeriod) -> Unit,
  onExportCsv: () -> Unit,
  onExportJson: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Filter transactions according to selectedPeriod
  val filteredTransactions = remember(uiState.transactions, uiState.selectedPeriod) {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)
    val currentDay = now.get(Calendar.DAY_OF_YEAR)

    uiState.transactions.filter { tx ->
      val txCal = Calendar.getInstance()
      txCal.timeInMillis = tx.dateMillis
      val txYear = txCal.get(Calendar.YEAR)
      val txMonth = txCal.get(Calendar.MONTH)
      val txDay = txCal.get(Calendar.DAY_OF_YEAR)

      when (uiState.selectedPeriod) {
        ReportPeriod.HARI_INI -> txYear == currentYear && txDay == currentDay
        ReportPeriod.MINGGU_INI -> {
          val diffDays = (now.timeInMillis - tx.dateMillis) / 86400000L
          diffDays in 0..7
        }
        ReportPeriod.BULAN_INI -> txYear == currentYear && txMonth == currentMonth
        ReportPeriod.BULAN_LALU -> {
          val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
          val targetYear = if (currentMonth == 0) currentYear - 1 else currentYear
          txYear == targetYear && txMonth == lastMonth
        }
        ReportPeriod.TAHUN_INI -> txYear == currentYear
        ReportPeriod.SEMUA -> true
      }
    }
  }

  var periodIncome = 0L
  var periodExpense = 0L
  var periodDeposit = 0L
  var periodWithdrawal = 0L
  val expensesByCategory = mutableMapOf<String, Long>()

  for (tx in filteredTransactions) {
    when (tx.type) {
      TransactionType.INCOME.name -> periodIncome += tx.amount
      TransactionType.EXPENSE.name -> {
        periodExpense += tx.amount
        expensesByCategory[tx.category] = (expensesByCategory[tx.category] ?: 0L) + tx.amount
      }
      TransactionType.SAVINGS_DEPOSIT.name -> periodDeposit += tx.amount
      TransactionType.SAVINGS_WITHDRAWAL.name -> periodWithdrawal += tx.amount
    }
  }

  val cashflow = periodIncome - periodExpense

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("reports_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Period Selector Chips
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        ReportPeriod.entries.forEach { period ->
          FilterChip(
            selected = uiState.selectedPeriod == period,
            onClick = { onPeriodChange(period) },
            label = {
              Text(
                text = period.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (uiState.selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }
    }

    // 2. Financial Overview Card
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Ringkasan Arus Kas (${uiState.selectedPeriod.title})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Total Pemasukan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(FinancialEngine.formatRupiah(periodIncome), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PantauSuccessGreen)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Total Pengeluaran", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(FinancialEngine.formatRupiah(periodExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PantauDangerRed)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Selisih Kas (Net)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              val netPrefix = if (cashflow >= 0) "+" else ""
              Text(
                "$netPrefix${FinancialEngine.formatRupiah(cashflow)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (cashflow >= 0) PantauSuccessGreen else PantauDangerRed
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Setoran Tabungan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(FinancialEngine.formatRupiah(periodDeposit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PantauPrimaryBlue)
            }
          }
        }
      }
    }

    // 3. Expense Distribution Donut Chart
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Distribusi Pengeluaran per Kategori",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          DonutExpenseChart(expensesByCategory = expensesByCategory)
        }
      }
    }

    // 4. Comparison Bar Chart
    if (periodIncome > 0 || periodExpense > 0) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          IncomeExpenseBarChart(
            income = periodIncome,
            expense = periodExpense
          )
        }
      }
    }

    // 5. Export Section
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "Ekspor Laporan Keuangan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Unduh atau simpan data transaksi untuk pembukuan dan cadangan arsip.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onExportCsv,
              colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Ekspor CSV")
            }
            OutlinedButton(
              onClick = onExportJson,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Cadangan JSON")
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
