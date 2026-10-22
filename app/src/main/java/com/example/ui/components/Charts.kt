package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FinancialEngine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

val chartPalette = listOf(
  Color(0xFF2563EB), // Primary Blue
  Color(0xFFE85D5D), // Red
  Color(0xFF19A974), // Emerald Green
  Color(0xFFF59E0B), // Amber
  Color(0xFF8B5CF6), // Purple
  Color(0xFF06B6D4), // Cyan
  Color(0xFFEC4899), // Pink
  Color(0xFF10B981), // Teal
  Color(0xFFF97316), // Orange
  Color(0xFF6366F1), // Indigo
  Color(0xFF64748B)  // Slate
)

data class ChartSlice(
  val label: String,
  val amount: Long,
  val percentage: Float,
  val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutExpenseChart(
  expensesByCategory: Map<String, Long>,
  modifier: Modifier = Modifier
) {
  val total = expensesByCategory.values.sum()

  if (total == 0L || expensesByCategory.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .height(180.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Belum ada data pengeluaran untuk periode ini",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
    return
  }

  val slices = remember(expensesByCategory) {
    val sorted = expensesByCategory.entries.sortedByDescending { it.value }
    sorted.mapIndexed { index, entry ->
      val pct = (entry.value.toFloat() / total.toFloat()) * 100f
      ChartSlice(
        label = entry.key,
        amount = entry.value,
        percentage = pct,
        color = chartPalette[index % chartPalette.size]
      )
    }
  }

  var selectedSlice by remember { mutableStateOf<ChartSlice?>(null) }
  val progress = remember { Animatable(0f) }

  LaunchedEffect(slices) {
    progress.snapTo(0f)
    progress.animateTo(1f, animationSpec = tween(durationMillis = 650))
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(220.dp)
        .padding(12.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .size(200.dp)
          .pointerInput(slices) {
            detectTapGestures { offset ->
              val center = Offset(size.width / 2f, size.height / 2f)
              val dx = offset.x - center.x
              val dy = offset.y - center.y
              var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
              if (touchAngle < 0) touchAngle += 360f

              var currentAngle = -90f
              var hit: ChartSlice? = null
              for (slice in slices) {
                val sweep = (slice.percentage / 100f) * 360f
                val start = (currentAngle % 360f + 360f) % 360f
                val end = ((currentAngle + sweep) % 360f + 360f) % 360f

                val isInside = if (start < end) {
                  touchAngle in start..end
                } else {
                  touchAngle >= start || touchAngle <= end
                }

                if (isInside) {
                  hit = slice
                  break
                }
                currentAngle += sweep
              }
              selectedSlice = if (selectedSlice == hit) null else hit
            }
          }
      ) {
        val strokeWidth = 32.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

        var startAngle = -90f
        for (slice in slices) {
          val sweepAngle = (slice.percentage / 100f) * 360f * progress.value
          val isSelected = selectedSlice?.label == slice.label
          val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

          drawArc(
            color = slice.color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = currentStroke, cap = StrokeCap.Round)
          )
          startAngle += sweepAngle
        }
      }

      // Center text
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (selectedSlice != null) {
          Text(
            text = selectedSlice!!.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
          )
          Text(
            text = "${String.format("%.1f", selectedSlice!!.percentage)}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = selectedSlice!!.color
          )
          Text(
            text = FinancialEngine.formatRupiah(selectedSlice!!.amount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
          )
        } else {
          Text(
            text = "Total Keluar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = FinancialEngine.formatRupiah(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Legend
    FlowRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.Center,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      slices.take(6).forEach { slice ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(slice.color, CircleShape)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${slice.label} (${String.format("%.0f", slice.percentage)}%)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun IncomeExpenseBarChart(
  income: Long,
  expense: Long,
  modifier: Modifier = Modifier
) {
  val maxVal = maxOf(income, expense, 1L).toFloat()
  val animProgress = remember { Animatable(0f) }

  LaunchedEffect(income, expense) {
    animProgress.snapTo(0f)
    animProgress.animateTo(1f, animationSpec = tween(500))
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Pemasukan vs Pengeluaran",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      val cashflow = income - expense
      Text(
        text = if (cashflow >= 0) "+${FinancialEngine.formatRupiah(cashflow)}" else FinancialEngine.formatRupiah(cashflow),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (cashflow >= 0) Color(0xFF19A974) else Color(0xFFE85D5D)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Bar for Income
    val incomeFraction = (income.toFloat() / maxVal) * animProgress.value
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Pemasukan", style = MaterialTheme.typography.bodySmall)
        Text(FinancialEngine.formatRupiah(income), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(14.dp)
          .background(Color(0xFFE2E8F0), RoundedCornerShape(7.dp))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(fraction = incomeFraction.coerceIn(0.01f, 1f))
            .height(14.dp)
            .background(Color(0xFF19A974), RoundedCornerShape(7.dp))
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Bar for Expense
    val expenseFraction = (expense.toFloat() / maxVal) * animProgress.value
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Pengeluaran", style = MaterialTheme.typography.bodySmall)
        Text(FinancialEngine.formatRupiah(expense), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(14.dp)
          .background(Color(0xFFE2E8F0), RoundedCornerShape(7.dp))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(fraction = expenseFraction.coerceIn(0.01f, 1f))
            .height(14.dp)
            .background(Color(0xFFE85D5D), RoundedCornerShape(7.dp))
        )
      }
    }
  }
}
