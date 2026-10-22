package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.BudgetComputation
import com.example.domain.BudgetStatus
import com.example.domain.FinancialEngine
import com.example.domain.FinancialSummary
import com.example.domain.GoalComputation
import com.example.domain.GoalStatus
import com.example.ui.theme.PantauDangerRed
import com.example.ui.theme.PantauLightBlue
import com.example.ui.theme.PantauNavy
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauSuccessGreen
import com.example.ui.theme.PantauWarningAmber

fun getGoalIcon(iconName: String): ImageVector {
  return when (iconName) {
    "Shield" -> Icons.Default.Shield
    "Heart" -> Icons.Default.Savings
    "Home" -> Icons.Default.Home
    "DirectionsCar" -> Icons.Default.DirectionsCar
    "Flight" -> Icons.Default.Flight
    "Smartphone" -> Icons.Default.Smartphone
    "School" -> Icons.Default.School
    "TrendingUp" -> Icons.Default.TrendingUp
    else -> Icons.Default.Savings
  }
}

@Composable
fun BalanceHeroCard(
  summary: FinancialSummary,
  isVisible: Boolean,
  onToggleVisibility: () -> Unit,
  modifier: Modifier = Modifier
) {
  val gradient = Brush.linearGradient(
    colors = listOf(
      PantauNavy,
      PantauPrimaryBlue
    )
  )

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    modifier = modifier
      .fillMaxWidth()
      .testTag("balance_hero_card")
  ) {
    Box(
      modifier = Modifier
        .background(gradient)
        .padding(20.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Saldo Tersedia",
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val balanceText = if (isVisible) {
              FinancialEngine.formatRupiah(summary.availableBalance)
            } else {
              "••••••••"
            }
            Text(
              text = balanceText,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          IconButton(
            onClick = onToggleVisibility,
            modifier = Modifier
              .background(Color.White.copy(alpha = 0.15f), CircleShape)
              .testTag("toggle_balance_visibility")
          ) {
            Icon(
              imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = if (isVisible) "Sembunyikan Saldo" else "Tampilkan Saldo",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Total Recorded Funds sub-badge
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Total Dana Tercatat (Kas + Tabungan)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
          )
          Text(
            text = if (isVisible) FinancialEngine.formatRupiah(summary.totalRecordedFunds) else "••••••••",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3 Stats Pills (Pemasukan, Pengeluaran, Tabungan)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Pemasukan
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .background(PantauSuccessGreen, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowDownward,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Pemasukan",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isVisible) FinancialEngine.formatRupiah(summary.totalIncome) else "••••",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          // Pengeluaran
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .background(PantauDangerRed, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowUpward,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Pengeluaran",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isVisible) FinancialEngine.formatRupiah(summary.totalExpense) else "••••",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          // Tabungan
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .background(Color(0xFF06B6D4), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Savings,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Tabungan",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isVisible) FinancialEngine.formatRupiah(summary.totalSavings) else "••••",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@Composable
fun QuickActionRow(
  onAddIncome: () -> Unit,
  onAddExpense: () -> Unit,
  onDepositSavings: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Pemasukan
    ElevatedCard(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
      modifier = Modifier
        .weight(1f)
        .clickable(onClick = onAddIncome)
        .testTag("quick_action_income")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFFE8F7F0), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = PantauSuccessGreen, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pemasukan",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    // Pengeluaran
    ElevatedCard(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
      modifier = Modifier
        .weight(1f)
        .clickable(onClick = onAddExpense)
        .testTag("quick_action_expense")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFFFDECEC), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Remove, contentDescription = null, tint = PantauDangerRed, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pengeluaran",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    // Setor Tabungan
    ElevatedCard(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
      modifier = Modifier
        .weight(1f)
        .clickable(onClick = onDepositSavings)
        .testTag("quick_action_deposit")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFFE0F2FE), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Savings, contentDescription = null, tint = PantauPrimaryBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Setor",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
fun TransactionRowItem(
  transaction: TransactionEntity,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  val (icon, bgColor, tintColor, prefix, amountColor) = when (transaction.type) {
    TransactionType.INCOME.name -> Tuple5(
      Icons.Default.ArrowDownward,
      Color(0xFFE8F7F0),
      PantauSuccessGreen,
      "+",
      PantauSuccessGreen
    )
    TransactionType.EXPENSE.name -> Tuple5(
      Icons.Default.ArrowUpward,
      Color(0xFFFDECEC),
      PantauDangerRed,
      "-",
      PantauDangerRed
    )
    TransactionType.SAVINGS_DEPOSIT.name -> Tuple5(
      Icons.Default.Savings,
      Color(0xFFE0F2FE),
      PantauPrimaryBlue,
      "↗",
      PantauPrimaryBlue
    )
    TransactionType.SAVINGS_WITHDRAWAL.name -> Tuple5(
      Icons.Default.Savings,
      Color(0xFFFEF3C7),
      PantauWarningAmber,
      "↙",
      PantauWarningAmber
    )
    else -> Tuple5(
      Icons.Default.TrendingUp,
      Color(0xFFF1F5F9),
      Color.Gray,
      "",
      Color.Gray
    )
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(22.dp))
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = transaction.category,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = FinancialEngine.formatDate(transaction.dateMillis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (transaction.source.isNotBlank()) {
            Text(
              text = " • ${transaction.source}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        if (transaction.note.isNotBlank()) {
          Text(
            text = transaction.note,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "$prefix${FinancialEngine.formatRupiah(transaction.amount)}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = amountColor
        )

        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
          }
          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Edit") },
              leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
              onClick = {
                showMenu = false
                onEdit()
              }
            )
            DropdownMenuItem(
              text = { Text("Hapus", color = PantauDangerRed) },
              leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = PantauDangerRed) },
              onClick = {
                showMenu = false
                onDelete()
              }
            )
          }
        }
      }
    }
  }
}

data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

@Composable
fun SavingsProgressCard(
  computation: GoalComputation,
  onDeposit: () -> Unit,
  onWithdraw: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val goal = computation.goal
  val animProgress by animateFloatAsState(
    targetValue = computation.visualProgress,
    animationSpec = tween(600),
    label = "progress"
  )

  var showMenu by remember { mutableStateOf(false) }

  val statusColor = when (computation.status) {
    GoalStatus.ACHIEVED, GoalStatus.EXCEEDED -> PantauSuccessGreen
    GoalStatus.OVERDUE -> PantauDangerRed
    GoalStatus.NEAR_TARGET -> PantauPrimaryBlue
    GoalStatus.IN_PROGRESS -> PantauLightBlue
    GoalStatus.NOT_STARTED -> Color.Gray
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(Color(0xFFE0F2FE), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = getGoalIcon(goal.iconName),
              contentDescription = null,
              tint = PantauPrimaryBlue,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = goal.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = statusColor.copy(alpha = 0.15f)
            ) {
              Text(
                text = computation.status.label,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Box {
          IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
              text = { Text("Edit Target") },
              leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
              onClick = {
                showMenu = false
                onEdit()
              }
            )
            DropdownMenuItem(
              text = { Text("Hapus", color = PantauDangerRed) },
              leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = PantauDangerRed) },
              onClick = {
                showMenu = false
                onDelete()
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Text("Terkumpul", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            FinancialEngine.formatRupiah(goal.currentAmount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Target: ${FinancialEngine.formatRupiah(goal.targetAmount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            "${String.format("%.1f", computation.percentageAchieved)}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      LinearProgressIndicator(
        progress = { animProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = statusColor,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        if (computation.remainingAmount > 0) {
          Text(
            text = "Sisa: ${FinancialEngine.formatRupiah(computation.remainingAmount)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          Text(
            text = "Target terpenuhi!",
            style = MaterialTheme.typography.labelSmall,
            color = PantauSuccessGreen,
            fontWeight = FontWeight.Bold
          )
        }

        if (goal.deadlineDateMillis > 0) {
          Text(
            text = "Tenggat: ${FinancialEngine.formatDate(goal.deadlineDateMillis)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (computation.estimatedRoutineDeposit > 0 && computation.remainingAmount > 0) {
        val freqLabel = when (goal.routineFrequency) {
          "DAILY" -> "per hari"
          "WEEKLY" -> "per minggu"
          else -> "per bulan"
        }
        Text(
          text = "Estimasi setoran: ${FinancialEngine.formatRupiah(computation.estimatedRoutineDeposit)} $freqLabel",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onWithdraw,
          enabled = goal.currentAmount > 0,
          modifier = Modifier.weight(1f)
        ) {
          Text("Tarik Dana", style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
          onClick = onDeposit,
          modifier = Modifier.weight(1f)
        ) {
          Text("+ Setor Dana", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun BudgetProgressCard(
  computation: BudgetComputation,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val animProgress by animateFloatAsState(
    targetValue = (computation.percentageUsed / 100f).coerceIn(0f, 1f),
    animationSpec = tween(600),
    label = "budget_progress"
  )

  var showMenu by remember { mutableStateOf(false) }

  val statusColor = when (computation.status) {
    BudgetStatus.SAFE -> PantauSuccessGreen
    BudgetStatus.WARNING -> PantauWarningAmber
    BudgetStatus.LIMIT_REACHED, BudgetStatus.EXCEEDED -> PantauDangerRed
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = computation.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (computation.category.isNotBlank()) {
            Text(
              text = "Kategori: ${computation.category}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Text(
              text = "Total Semua Pengeluaran",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = statusColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = computation.status.label,
              style = MaterialTheme.typography.labelSmall,
              color = statusColor,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                  showMenu = false
                  onEdit()
                }
              )
              DropdownMenuItem(
                text = { Text("Hapus", color = PantauDangerRed) },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = PantauDangerRed) },
                onClick = {
                  showMenu = false
                  onDelete()
                }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Terpakai: ${FinancialEngine.formatRupiah(computation.spentAmount)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Batas: ${FinancialEngine.formatRupiah(computation.limitAmount)}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      LinearProgressIndicator(
        progress = { animProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = statusColor,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        val remainingText = if (computation.remainingAmount >= 0) {
          "Sisa: ${FinancialEngine.formatRupiah(computation.remainingAmount)}"
        } else {
          "Lebih: ${FinancialEngine.formatRupiah(-computation.remainingAmount)}"
        }
        Text(
          text = remainingText,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (computation.remainingAmount >= 0) PantauSuccessGreen else PantauDangerRed
        )

        Text(
          text = "${String.format("%.1f", computation.percentageUsed)}%",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = statusColor
        )
      }
    }
  }
}
