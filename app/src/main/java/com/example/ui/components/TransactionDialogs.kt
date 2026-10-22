package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DefaultData
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.FinancialEngine
import com.example.ui.theme.PantauDangerRed
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauSuccessGreen
import com.example.ui.theme.PantauWarningAmber

@Composable
fun ConfirmDialog(
  title: String,
  message: String,
  confirmText: String = "Lanjutkan",
  dismissText: String = "Batal",
  isDanger: Boolean = false,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    },
    text = {
      Text(text = message, style = MaterialTheme.typography.bodyMedium)
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = if (isDanger) ButtonDefaults.buttonColors(containerColor = PantauDangerRed) else ButtonDefaults.buttonColors()
      ) {
        Text(confirmText)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(dismissText)
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
  existingTx: TransactionEntity? = null,
  categories: List<CategoryEntity>,
  onDismiss: () -> Unit,
  onSave: (amount: Long, category: String, source: String, note: String, isSalary: Boolean) -> Unit
) {
  var amount by remember { mutableLongStateOf(existingTx?.amount ?: 0L) }
  var category by remember { mutableStateOf(existingTx?.category ?: (categories.firstOrNull { it.type == "INCOME" }?.name ?: "Gaji Pokok")) }
  var source by remember { mutableStateOf(existingTx?.source ?: "Kantor") }
  var note by remember { mutableStateOf(existingTx?.note ?: "") }
  var isSalary by remember { mutableStateOf(existingTx?.isRecurringSalary ?: false) }
  var isCategoryExpanded by remember { mutableStateOf(false) }

  val incomeCategories = categories.filter { it.type == "INCOME" }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingTx != null) "Edit Pemasukan" else "Tambah Pemasukan",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        CurrencyInputField(
          value = amount,
          onValueChange = { amount = it },
          label = "Nominal Pemasukan",
          modifier = Modifier.fillMaxWidth()
        )

        // Category Picker
        ExposedDropdownMenuBox(
          expanded = isCategoryExpanded,
          onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
        ) {
          OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = isCategoryExpanded,
            onDismissRequest = { isCategoryExpanded = false }
          ) {
            incomeCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  category = cat.name
                  isCategoryExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = source,
          onValueChange = { source = it },
          label = { Text("Sumber Pemasukan") },
          placeholder = { Text("cth: Gaji Kantor, Bonus, Klien Freelance") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan (Opsional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Tandai sebagai Gaji Rutin", style = MaterialTheme.typography.bodyMedium)
            Text("Membantu verifikasi status gaji bulan ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Switch(
            checked = isSalary,
            onCheckedChange = { isSalary = it }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (amount > 0) {
            onSave(amount, category, source, note, isSalary)
          }
        },
        enabled = amount > 0,
        colors = ButtonDefaults.buttonColors(containerColor = PantauSuccessGreen)
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
  existingTx: TransactionEntity? = null,
  categories: List<CategoryEntity>,
  budgets: List<BudgetEntity>,
  availableBalance: Long,
  allowNegativeBalance: Boolean,
  onDismiss: () -> Unit,
  onSave: (amount: Long, category: String, source: String, note: String) -> Unit
) {
  var amount by remember { mutableLongStateOf(existingTx?.amount ?: 0L) }
  var category by remember { mutableStateOf(existingTx?.category ?: (categories.firstOrNull { it.type == "EXPENSE" }?.name ?: "Makanan dan Kebutuhan Pokok")) }
  var paymentMethod by remember { mutableStateOf(existingTx?.source ?: "QRIS") }
  var note by remember { mutableStateOf(existingTx?.note ?: "") }
  var isCategoryExpanded by remember { mutableStateOf(false) }

  val expenseCategories = categories.filter { it.type == "EXPENSE" }
  val paymentMethods = listOf("QRIS", "Tunai", "Transfer Bank", "E-Wallet", "Kartu Debit/Kredit")

  // Check budget for this category
  val matchingBudget = budgets.find { it.category.equals(category, ignoreCase = true) }
  val isExceedingBalance = amount > availableBalance

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingTx != null) "Edit Pengeluaran" else "Tambah Pengeluaran",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        CurrencyInputField(
          value = amount,
          onValueChange = { amount = it },
          label = "Nominal Pengeluaran",
          modifier = Modifier.fillMaxWidth()
        )

        // Balance warning if exceeding
        if (isExceedingBalance) {
          Card(
            colors = CardDefaults.cardColors(containerColor = PantauWarningAmber.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = PantauWarningAmber, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Nominal melebihi saldo tersedia (${FinancialEngine.formatRupiah(availableBalance)})." +
                  if (!allowNegativeBalance) " Pengaturan saat ini menolak saldo negatif." else " Saldo akan menjadi negatif.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        // Category Picker
        ExposedDropdownMenuBox(
          expanded = isCategoryExpanded,
          onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
        ) {
          OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = isCategoryExpanded,
            onDismissRequest = { isCategoryExpanded = false }
          ) {
            expenseCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  category = cat.name
                  isCategoryExpanded = false
                }
              )
            }
          }
        }

        // Budget indicator if set
        if (matchingBudget != null) {
          Text(
            text = "Kategori ini memiliki anggaran: ${FinancialEngine.formatRupiah(matchingBudget.limitAmount)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }

        // Payment Method Chips
        Text("Metode Pembayaran", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          paymentMethods.forEach { method ->
            FilterChip(
              selected = paymentMethod == method,
              onClick = { paymentMethod = method },
              label = { Text(method, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan (Opsional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      val canSave = amount > 0 && (allowNegativeBalance || !isExceedingBalance)
      Button(
        onClick = {
          if (canSave) {
            onSave(amount, category, paymentMethod, note)
          }
        },
        enabled = canSave,
        colors = ButtonDefaults.buttonColors(containerColor = PantauDangerRed)
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

@Composable
fun SavingsDepositWithdrawDialog(
  isDeposit: Boolean,
  goal: SavingsGoalEntity,
  availableBalance: Long,
  allowNegativeBalance: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (amount: Long, note: String) -> Unit
) {
  var amount by remember { mutableLongStateOf(0L) }
  var note by remember { mutableStateOf("") }

  val title = if (isDeposit) "Setor ke ${goal.name}" else "Tarik dari ${goal.name}"
  val isExceedingAvailable = isDeposit && amount > availableBalance && !allowNegativeBalance
  val isExceedingGoal = !isDeposit && amount > goal.currentAmount

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = if (isDeposit) {
            "Saldo Tersedia: ${FinancialEngine.formatRupiah(availableBalance)}\nSaldo Tabungan saat ini: ${FinancialEngine.formatRupiah(goal.currentAmount)}"
          } else {
            "Saldo Tabungan saat ini: ${FinancialEngine.formatRupiah(goal.currentAmount)}\nDana akan dikembalikan ke Saldo Tersedia."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CurrencyInputField(
          value = amount,
          onValueChange = { amount = it },
          label = if (isDeposit) "Nominal Setoran" else "Nominal Penarikan",
          modifier = Modifier.fillMaxWidth()
        )

        if (isExceedingAvailable) {
          Text(
            text = "Nominal melebihi saldo tersedia yang ada.",
            style = MaterialTheme.typography.labelSmall,
            color = PantauDangerRed
          )
        }
        if (isExceedingGoal) {
          Text(
            text = "Nominal melebihi saldo yang ada di tabungan ini.",
            style = MaterialTheme.typography.labelSmall,
            color = PantauDangerRed
          )
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan (Opsional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      val isValid = amount > 0 && !isExceedingAvailable && !isExceedingGoal
      Button(
        onClick = { if (isValid) onConfirm(amount, note) },
        enabled = isValid,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDeposit) PantauPrimaryBlue else PantauWarningAmber
        )
      ) {
        Text(if (isDeposit) "Setor Sekarang" else "Tarik Sekarang")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

@Composable
fun SavingsGoalDialog(
  existingGoal: SavingsGoalEntity? = null,
  onDismiss: () -> Unit,
  onSave: (
    name: String,
    iconName: String,
    accentColorHex: String,
    targetAmount: Long,
    deadlineDateMillis: Long,
    routineTargetAmount: Long,
    routineFrequency: String,
    note: String
  ) -> Unit
) {
  var name by remember { mutableStateOf(existingGoal?.name ?: "") }
  var iconName by remember { mutableStateOf(existingGoal?.iconName ?: "Savings") }
  var accentColorHex by remember { mutableStateOf(existingGoal?.accentColorHex ?: "#2563EB") }
  var targetAmount by remember { mutableLongStateOf(existingGoal?.targetAmount ?: 10_000_000L) }
  var routineFrequency by remember { mutableStateOf(existingGoal?.routineFrequency ?: "MONTHLY") }
  var routineAmount by remember { mutableLongStateOf(existingGoal?.routineTargetAmount ?: 500_000L) }
  var note by remember { mutableStateOf(existingGoal?.note ?: "") }

  val frequencies = listOf("DAILY" to "Harian", "WEEKLY" to "Mingguan", "MONTHLY" to "Bulanan", "NONE" to "Bebas")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingGoal != null) "Edit Tujuan Tabungan" else "Tambah Tujuan Tabungan",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (existingGoal == null) {
          Text("Pilih Template Cepat:", style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            DefaultData.savingsTemplates.take(8).forEach { tmpl ->
              FilterChip(
                selected = name == tmpl.name,
                onClick = {
                  name = tmpl.name
                  iconName = tmpl.iconName
                  accentColorHex = tmpl.accentColorHex
                  targetAmount = tmpl.defaultTarget
                },
                label = { Text(tmpl.name, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nama Tujuan Tabungan") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        CurrencyInputField(
          value = targetAmount,
          onValueChange = { targetAmount = it },
          label = "Target Nominal (Rp)",
          modifier = Modifier.fillMaxWidth()
        )

        Text("Frekuensi Rencana Setoran:", style = MaterialTheme.typography.labelSmall)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          frequencies.forEach { (key, label) ->
            FilterChip(
              selected = routineFrequency == key,
              onClick = { routineFrequency = key },
              label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }

        if (routineFrequency != "NONE") {
          CurrencyInputField(
            value = routineAmount,
            onValueChange = { routineAmount = it },
            label = "Target Setoran Rutin",
            modifier = Modifier.fillMaxWidth()
          )
        }

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Catatan / Alasan") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && targetAmount > 0) {
            val deadline = if (existingGoal != null && existingGoal.deadlineDateMillis > 0) {
              existingGoal.deadlineDateMillis
            } else {
              System.currentTimeMillis() + 180L * 86400000L // 6 months default
            }
            onSave(name, iconName, accentColorHex, targetAmount, deadline, routineAmount, routineFrequency, note)
          }
        },
        enabled = name.isNotBlank() && targetAmount > 0
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
  existingBudget: BudgetEntity? = null,
  categories: List<CategoryEntity>,
  onDismiss: () -> Unit,
  onSave: (name: String, category: String, limitAmount: Long) -> Unit
) {
  var name by remember { mutableStateOf(existingBudget?.name ?: "Anggaran Makan Bulanan") }
  var category by remember { mutableStateOf(existingBudget?.category ?: "") }
  var limitAmount by remember { mutableLongStateOf(existingBudget?.limitAmount ?: 1_500_000L) }
  var isCategoryExpanded by remember { mutableStateOf(false) }

  val expenseCategories = categories.filter { it.type == "EXPENSE" }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingBudget != null) "Edit Anggaran" else "Tambah Anggaran Bulanan",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nama Anggaran") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
          expanded = isCategoryExpanded,
          onExpandedChange = { isCategoryExpanded = !isCategoryExpanded }
        ) {
          OutlinedTextField(
            value = if (category.isBlank()) "Semua Pengeluaran (Total)" else category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategori yang Dibatasi") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = isCategoryExpanded,
            onDismissRequest = { isCategoryExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("Semua Pengeluaran (Total)") },
              onClick = {
                category = ""
                isCategoryExpanded = false
              }
            )
            expenseCategories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  category = cat.name
                  isCategoryExpanded = false
                }
              )
            }
          }
        }

        CurrencyInputField(
          value = limitAmount,
          onValueChange = { limitAmount = it },
          label = "Batas Nominal Anggaran",
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && limitAmount > 0) {
            onSave(name, category, limitAmount)
          }
        },
        enabled = name.isNotBlank() && limitAmount > 0
      ) {
        Text("Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Batal")
      }
    }
  )
}
