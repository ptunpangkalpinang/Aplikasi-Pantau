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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
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
import com.example.data.model.SavingsGoalEntity
import com.example.domain.FinancialEngine
import com.example.ui.components.SavingsProgressCard
import com.example.ui.theme.PantauNavy
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.viewmodel.PantauUiState

@Composable
fun SavingsScreen(
  uiState: PantauUiState,
  onAddNewGoal: () -> Unit,
  onDepositGoal: (SavingsGoalEntity) -> Unit,
  onWithdrawGoal: (SavingsGoalEntity) -> Unit,
  onEditGoal: (SavingsGoalEntity) -> Unit,
  onDeleteGoal: (SavingsGoalEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val totalSavings = uiState.summary.totalSavings
  val activeGoalsCount = uiState.goals.count { !it.isCompleted }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("savings_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Total Savings Banner
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
                text = "Total Tabungan Terkumpul",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = FinancialEngine.formatRupiah(totalSavings),
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
              Icon(Icons.Default.Savings, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "$activeGoalsCount tujuan tabungan sedang aktif • Setoran tabungan otomatis memindahkan dana dari Saldo Tersedia tanpa dihitung sebagai pengeluaran konsumtif.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
          )
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
          text = "Daftar Tujuan (${uiState.goals.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Button(
          onClick = onAddNewGoal,
          colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Tambah Target")
        }
      }
    }

    // 3. Goals list or empty state
    if (uiState.goalComputations.isEmpty()) {
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
              Icons.Default.Savings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Belum Ada Tujuan Tabungan",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Mulai rencanakan Dana Darurat, Tabungan Nikah, Rumah, Liburan, atau tabungan lainnya sekarang.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onAddNewGoal,
              colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
            ) {
              Text("Buat Target Pertama")
            }
          }
        }
      }
    } else {
      items(uiState.goalComputations, key = { it.goal.id }) { comp ->
        SavingsProgressCard(
          computation = comp,
          onDeposit = { onDepositGoal(comp.goal) },
          onWithdraw = { onWithdrawGoal(comp.goal) },
          onEdit = { onEditGoal(comp.goal) },
          onDelete = { onDeleteGoal(comp.goal) }
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}
