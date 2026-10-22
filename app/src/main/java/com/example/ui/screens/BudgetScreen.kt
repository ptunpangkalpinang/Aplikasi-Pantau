package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetEntity
import com.example.domain.FinancialEngine
import com.example.ui.components.BudgetProgressCard
import com.example.ui.theme.PantauNavy
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauSuccessGreen
import com.example.ui.viewmodel.PantauUiState

@Composable
fun BudgetScreen(
  uiState: PantauUiState,
  onAddNewBudget: () -> Unit,
  onEditBudget: (BudgetEntity) -> Unit,
  onDeleteBudget: (BudgetEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val totalLimit = uiState.budgets.sumOf { it.limitAmount }
  val totalSpent = uiState.budgetComputations.sumOf { it.spentAmount }
  val totalRemaining = totalLimit - totalSpent

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("budget_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Total Budget Banner
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PantauNavy),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Total Batas Anggaran",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = FinancialEngine.formatRupiah(totalLimit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Box(
              modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Pengeluaran Aktual:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
              Text(FinancialEngine.formatRupiah(totalSpent), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Sisa Anggaran:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
              Text(
                FinancialEngine.formatRupiah(totalRemaining),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalRemaining >= 0) PantauSuccessGreen else Color(0xFFF87171)
              )
            }
          }
        }
      }
    }

    // 2. Add Button & Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Pos Anggaran (${uiState.budgets.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Button(
          onClick = onAddNewBudget,
          colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tambah Anggaran")
        }
      }
    }

    // 3. Budgets list or empty state
    if (uiState.budgetComputations.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.AccountBalanceWallet,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Belum Ada Pos Anggaran",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tentukan batas pengeluaran untuk pos tertentu (cth: Makanan, Bensin, Hiburan) agar keuangan tetap terkendali.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onAddNewBudget,
              colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
            ) {
              Text("Buat Anggaran Sekarang")
            }
          }
        }
      }
    } else {
      items(uiState.budgetComputations, key = { it.budgetId }) { comp ->
        val entity = uiState.budgets.find { it.id == comp.budgetId }
        BudgetProgressCard(
          computation = comp,
          onEdit = { if (entity != null) onEditBudget(entity) },
          onDelete = { if (entity != null) onDeleteBudget(entity) }
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
