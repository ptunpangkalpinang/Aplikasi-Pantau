package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.model.CategoryEntity
import com.example.domain.FinancialEngine
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.CurrencyInputField
import com.example.ui.theme.PantauDangerRed
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.viewmodel.PantauUiState

@Composable
fun SettingsScreen(
  uiState: PantauUiState,
  onSetInitialBalance: (Long) -> Unit,
  onSetSalaryConfig: (source: String, amount: Long, day: Int) -> Unit,
  onSetAllowNegativeBalance: (Boolean) -> Unit,
  onSetThemeMode: (String) -> Unit,
  onAddCategory: (name: String, type: String) -> Unit,
  onDeleteCategory: (CategoryEntity) -> Unit,
  onApplyDemoData: () -> Unit,
  onClearDemoData: () -> Unit,
  onResetAllData: () -> Unit,
  onExportJson: () -> Unit,
  onImportJson: (String) -> Unit,
  onNavigateToConnections: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showInitialBalanceDialog by remember { mutableStateOf(false) }
  var showSalaryConfigDialog by remember { mutableStateOf(false) }
  var showCategoryDialog by remember { mutableStateOf(false) }
  var showResetStep1 by remember { mutableStateOf(false) }
  var showResetStep2 by remember { mutableStateOf(false) }
  var showImportDialog by remember { mutableStateOf(false) }
  var importJsonText by remember { mutableStateOf("") }

  val initialBalance = uiState.preferences["initial_balance"]?.toLongOrNull() ?: 0L
  val salarySource = uiState.preferences["salary_source"] ?: "Kantor"
  val expectedSalary = uiState.preferences["expected_salary"]?.toLongOrNull() ?: 0L
  val salaryDay = uiState.preferences["salary_day"]?.toIntOrNull() ?: 25
  val allowNegative = uiState.preferences["allow_negative_balance"]?.toBooleanStrictOrNull() ?: true
  val themeMode = uiState.preferences["theme_mode"] ?: "SYSTEM"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("settings_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header Info
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text("PANTAU", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PantauPrimaryBlue)
          Text(
            "Pantau Uangmu, Atur Masa Depanmu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Penyimpanan lokal aman di perangkat tanpa server eksternal.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }
      }
    }

    // 2. Saldo Awal & Gaji
    item {
      Text("Konfigurasi Finansial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Saldo Awal
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showInitialBalanceDialog = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AccountBalance, contentDescription = null, tint = PantauPrimaryBlue)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Saldo Awal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Titik awal kas sebelum transaksi dicatat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Text(
              FinancialEngine.formatRupiah(initialBalance),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = PantauPrimaryBlue
            )
          }

          // Gaji Bulanan
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showSalaryConfigDialog = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Work, contentDescription = null, tint = PantauPrimaryBlue)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Gaji Bulanan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("$salarySource • Tgl $salaryDay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            Text(
              if (expectedSalary > 0) FinancialEngine.formatRupiah(expectedSalary) else "Atur",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = PantauPrimaryBlue
            )
          }

          // Izinkan Saldo Negatif
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Izinkan Saldo Negatif", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              Text("Tetap izinkan pengeluaran jika melebihi saldo tersedia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = allowNegative,
              onCheckedChange = { onSetAllowNegativeBalance(it) }
            )
          }

          // Koneksi Keuangan (DANA, E-Wallet, Bank) - PRD Section 3
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onNavigateToConnections() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.CloudSync, contentDescription = null, tint = PantauPrimaryBlue)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Koneksi Keuangan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                  text = if (uiState.connections.isNotEmpty()) {
                    "${uiState.connections.size} akun terhubung (${uiState.connections.joinToString { it.providerName }})"
                  } else {
                    "Hubungkan DANA, e-wallet, dan rekening bank"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
              }
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PantauPrimaryBlue, modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    // 3. Tema Aplikasi
    item {
      Text("Tampilan & Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Pilihan Tema", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("SYSTEM" to "Sistem", "LIGHT" to "Terang", "DARK" to "Gelap").forEach { (key, label) ->
              FilterChip(
                selected = themeMode == key,
                onClick = { onSetThemeMode(key) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
              )
            }
          }
        }
      }
    }

    // 4. Kelola Kategori
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Kelola Kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = { showCategoryDialog = true }) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Kategori Baru")
        }
      }
    }

    val customCategories = uiState.categories.filter { !it.isDefault }
    if (customCategories.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("Kategori Buatan Anda:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            customCategories.forEach { cat ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("${cat.name} (${if (cat.type == "INCOME") "Pemasukan" else "Pengeluaran"})", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { onDeleteCategory(cat) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = PantauDangerRed, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }

    // 5. Data Demo
    item {
      Text("Data Demonstrasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Coba semua visualisasi, grafik, dan formula tabungan PANTAU dengan data contoh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onApplyDemoData,
              colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Gunakan Demo")
            }
            OutlinedButton(
              onClick = onClearDemoData,
              modifier = Modifier.weight(1f)
            ) {
              Text("Bersihkan")
            }
          }
        }
      }
    }

    // 6. Cadangan & Pemulihan
    item {
      Text("Cadangan & Pemulihan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Ekspor atau pulihkan semua transaksi, target tabungan, dan anggaran dalam format JSON standar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onExportJson,
              colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ekspor JSON")
            }
            OutlinedButton(
              onClick = { showImportDialog = true },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Impor JSON")
            }
          }
        }
      }
    }

    // 7. Reset Data
    item {
      Text("Zona Bahaya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PantauDangerRed)
    }

    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PantauDangerRed.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Hapus seluruh transaksi, tujuan tabungan, anggaran, dan kembalikan ke pengaturan awal pabrik.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
          Button(
            onClick = { showResetStep1 = true },
            colors = ButtonDefaults.buttonColors(containerColor = PantauDangerRed)
          ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Semua Data")
          }
        }
      }
    }

    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("PANTAU Versi 1.0.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Dibuat dengan Jetpack Compose & Room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
      }
      Spacer(modifier = Modifier.height(32.dp))
    }
  }

  // Dialog: Edit Saldo Awal
  if (showInitialBalanceDialog) {
    var tempBalance by remember { mutableLongStateOf(initialBalance) }
    AlertDialog(
      onDismissRequest = { showInitialBalanceDialog = false },
      title = { Text("Atur Saldo Awal") },
      text = {
        Column {
          Text(
            "Saldo awal digunakan sebagai titik awal kalkulasi kas sebelum transaksi tercatat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))
          CurrencyInputField(
            value = tempBalance,
            onValueChange = { tempBalance = it },
            label = "Saldo Awal (Rp)",
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSetInitialBalance(tempBalance)
            showInitialBalanceDialog = false
          }
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showInitialBalanceDialog = false }) {
          Text("Batal")
        }
      }
    )
  }

  // Dialog: Gaji Bulanan Config
  if (showSalaryConfigDialog) {
    var tempSource by remember { mutableStateOf(salarySource) }
    var tempAmount by remember { mutableLongStateOf(expectedSalary) }
    var tempDay by remember { mutableStateOf(salaryDay.toString()) }

    AlertDialog(
      onDismissRequest = { showSalaryConfigDialog = false },
      title = { Text("Pengaturan Gaji Bulanan") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = tempSource,
            onValueChange = { tempSource = it },
            label = { Text("Nama Sumber Gaji") },
            placeholder = { Text("cth: PT Maju Gemilang") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          CurrencyInputField(
            value = tempAmount,
            onValueChange = { tempAmount = it },
            label = "Nominal Gaji yang Diharapkan",
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = tempDay,
            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) tempDay = it },
            label = { Text("Tanggal Penerimaan Gaji (1-31)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val dayInt = tempDay.toIntOrNull()?.coerceIn(1, 31) ?: 25
            onSetSalaryConfig(tempSource, tempAmount, dayInt)
            showSalaryConfigDialog = false
          }
        ) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSalaryConfigDialog = false }) {
          Text("Batal")
        }
      }
    )
  }

  // Dialog: Tambah Kategori
  if (showCategoryDialog) {
    var catName by remember { mutableStateOf("") }
    var catType by remember { mutableStateOf("EXPENSE") }

    AlertDialog(
      onDismissRequest = { showCategoryDialog = false },
      title = { Text("Tambah Kategori Baru") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = catName,
            onValueChange = { catName = it },
            label = { Text("Nama Kategori") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text("Jenis Kategori:", style = MaterialTheme.typography.labelSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = catType == "EXPENSE",
              onClick = { catType = "EXPENSE" },
              label = { Text("Pengeluaran") }
            )
            FilterChip(
              selected = catType == "INCOME",
              onClick = { catType = "INCOME" },
              label = { Text("Pemasukan") }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (catName.isNotBlank()) {
              onAddCategory(catName, catType)
              showCategoryDialog = false
            }
          },
          enabled = catName.isNotBlank()
        ) {
          Text("Tambah")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCategoryDialog = false }) {
          Text("Batal")
        }
      }
    )
  }

  // Dialog: Import JSON
  if (showImportDialog) {
    AlertDialog(
      onDismissRequest = { showImportDialog = false },
      title = { Text("Impor Cadangan Data JSON") },
      text = {
        Column {
          Text(
            "Tempelkan isi string data cadangan JSON yang sebelumnya diekspor:",
            style = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = importJsonText,
            onValueChange = { importJsonText = it },
            placeholder = { Text("{\n  \"transactions\": [...],\n  \"goals\": [...]\n}") },
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
            maxLines = 8
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (importJsonText.isNotBlank()) {
              onImportJson(importJsonText)
              showImportDialog = false
              importJsonText = ""
            }
          },
          enabled = importJsonText.isNotBlank()
        ) {
          Text("Validasi & Impor")
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportDialog = false }) {
          Text("Batal")
        }
      }
    )
  }

  // 2-Step Reset Confirmation Dialogs (Requirement 15 & 20)
  if (showResetStep1) {
    ConfirmDialog(
      title = "Konfirmasi Reset Tahap 1",
      message = "Apakah Anda yakin ingin menghapus seluruh data transaksi, tabungan, dan anggaran? Tindakan ini tidak dapat dibatalkan.",
      confirmText = "Ya, Lanjutkan",
      dismissText = "Batal",
      isDanger = true,
      onConfirm = {
        showResetStep1 = false
        showResetStep2 = true
      },
      onDismiss = { showResetStep1 = false }
    )
  }

  if (showResetStep2) {
    ConfirmDialog(
      title = "Peringatan Terakhir!",
      message = "Semua riwayat keuangan akan dihapus secara permanen dari perangkat ini. Pastikan Anda sudah mengekspor cadangan jika diperlukan.",
      confirmText = "HAPUS SEMUA DATA SEKARANG",
      dismissText = "Batalkan",
      isDanger = true,
      onConfirm = {
        showResetStep2 = false
        onResetAllData()
      },
      onDismiss = { showResetStep2 = false }
    )
  }
}
