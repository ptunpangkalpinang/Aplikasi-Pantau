package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.ApiSupportStatus
import com.example.data.model.CategoryEntity
import com.example.data.model.ConnectionStatus
import com.example.data.model.FinancialConnectionEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.FinancialEngine
import com.example.integration.CsvStatementParser
import com.example.integration.FinancialProvider
import com.example.integration.FinancialProviders
import com.example.integration.ParsedStatementItem
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.PantauDangerRed
import com.example.ui.theme.PantauNavy
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.theme.PantauSuccessGreen
import com.example.ui.theme.PantauWarningAmber
import com.example.ui.viewmodel.PantauUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialConnectionsScreen(
  uiState: PantauUiState,
  onNavigateBack: () -> Unit,
  onConnectAccount: (providerId: String, alias: String, maskedAccount: String, method: String) -> Unit,
  onDisconnectAccount: (FinancialConnectionEntity) -> Unit,
  onSyncAccount: (FinancialConnectionEntity) -> Unit,
  onSyncAllAccounts: () -> Unit,
  onImportCsv: (csvContent: String, providerId: String, connectionId: String?) -> Unit,
  onConfirmPendingTx: (
    txId: String,
    correctedCategory: String,
    correctedType: TransactionType,
    isInternalTransfer: Boolean,
    linkedGoalId: String?,
    saveAsRuleKeyword: String?
  ) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf("ALL") } // ALL, E_WALLET, BANK, CONNECTED
  var showConnectDialog by remember { mutableStateOf(false) }
  var selectedProviderForConnect by remember { mutableStateOf<FinancialProvider?>(null) }
  var showCsvImportDialog by remember { mutableStateOf(false) }
  var showReviewPendingDialog by remember { mutableStateOf(false) }
  var deletingConnection by remember { mutableStateOf<FinancialConnectionEntity?>(null) }

  val connectedAccountsCount = uiState.connections.size
  val pendingReviewCount = uiState.pendingReviewTransactions.size

  // Calculate imported income and expense
  val importedTransactions = uiState.transactions.filter { !it.connectionId.isNullOrBlank() }
  val importedIncome = importedTransactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
  val importedExpense = importedTransactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

  val lastSyncTimeStr = remember(uiState.connections) {
    val latest = uiState.connections.map { it.lastSyncMillis }.maxOrNull() ?: 0L
    if (latest > 0) FinancialEngine.formatDate(latest, "dd MMM yyyy, HH:mm") else "Belum ada"
  }

  val displayedProviders = remember(selectedTab, uiState.connections) {
    FinancialProviders.allProviders.filter { p ->
      when (selectedTab) {
        "CONNECTED" -> uiState.connections.any { it.providerId == p.id }
        "E_WALLET" -> p.accountType == AccountType.E_WALLET
        "BANK" -> p.accountType == AccountType.BANK_ACCOUNT
        else -> true
      }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("financial_connections_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Top Bar / Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onNavigateBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Koneksi Keuangan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Hubungkan DANA, e-wallet & rekening bank Anda",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 2. Dashboard Koneksi Banner
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PantauNavy),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Dashboard Integrasi",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "$connectedAccountsCount Akun Terhubung",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }

            if (uiState.isSyncing) {
              CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp
              )
            } else {
              IconButton(
                onClick = onSyncAllAccounts,
                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sinkronkan Semua", tint = Color.White)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Statistics Grid
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Total Impor Masuk", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
              Text(FinancialEngine.formatRupiah(importedIncome), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PantauSuccessGreen)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Total Impor Keluar", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
              Text(FinancialEngine.formatRupiah(importedExpense), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PantauDangerRed)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Terakhir: $lastSyncTimeStr",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.75f)
            )
            if (pendingReviewCount > 0) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = PantauWarningAmber
              ) {
                Text(
                  text = "$pendingReviewCount Perlu Ditinjau",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.Black,
                  modifier = Modifier
                    .clickable { showReviewPendingDialog = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }
    }

    // 3. Action Buttons Row (Hubungkan Akun, Impor Mutasi CSV, Tinjau Transaksi)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            selectedProviderForConnect = null
            showConnectDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Hubungkan", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = { showCsvImportDialog = true },
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Impor CSV", style = MaterialTheme.typography.labelMedium)
        }

        if (pendingReviewCount > 0) {
          Button(
            onClick = { showReviewPendingDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = PantauWarningAmber),
            modifier = Modifier.weight(1f)
          ) {
            Text("Tinjau ($pendingReviewCount)", color = Color.Black, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 4. Privacy & Security Assurance Note (PRD Section 4 & 13)
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = PantauSuccessGreen, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Keamanan Terjamin: PANTAU tidak pernah meminta PIN, kata sandi perbankan, atau kode OTP. Tidak membaca SMS pribadi dan tidak melakukan scraping ilegal.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 5. Filter Chips
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          "ALL" to "Semua Penyedia",
          "CONNECTED" to "Terhubung (${uiState.connections.size})",
          "E_WALLET" to "DANA & E-Wallet",
          "BANK" to "Rekening Bank"
        ).forEach { (key, label) ->
          FilterChip(
            selected = selectedTab == key,
            onClick = { selectedTab = key },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
          )
        }
      }
    }

    // 6. Provider Cards List
    items(displayedProviders, key = { it.id }) { provider ->
      val connection = uiState.connections.find { it.providerId == provider.id }
      ProviderCard(
        provider = provider,
        connection = connection,
        onConnect = {
          selectedProviderForConnect = provider
          showConnectDialog = true
        },
        onSync = { if (connection != null) onSyncAccount(connection) },
        onDisconnect = { if (connection != null) deletingConnection = connection },
        onImportCsv = {
          showCsvImportDialog = true
        }
      )
    }

    item {
      Spacer(modifier = Modifier.height(32.dp))
    }
  }

  // Dialog: Hubungkan Akun Baru
  if (showConnectDialog) {
    ConnectAccountDialog(
      preselectedProvider = selectedProviderForConnect,
      onDismiss = {
        showConnectDialog = false
        selectedProviderForConnect = null
      },
      onConfirm = { providerId, alias, maskedNum, method ->
        onConnectAccount(providerId, alias, maskedNum, method)
        showConnectDialog = false
        selectedProviderForConnect = null
      }
    )
  }

  // Dialog: Impor Mutasi CSV
  if (showCsvImportDialog) {
    ImportCsvDialog(
      onDismiss = { showCsvImportDialog = false },
      onConfirmImport = { csvContent, providerId, connId ->
        onImportCsv(csvContent, providerId, connId)
        showCsvImportDialog = false
      }
    )
  }

  // Dialog: Tinjau Transaksi Pending Review
  if (showReviewPendingDialog) {
    ReviewPendingTransactionsDialog(
      pendingTransactions = uiState.pendingReviewTransactions,
      categories = uiState.categories,
      savingsGoals = uiState.goals,
      onDismiss = { showReviewPendingDialog = false },
      onConfirm = { txId, cat, type, isInternal, goalId, keyword ->
        onConfirmPendingTx(txId, cat, type, isInternal, goalId, keyword)
      }
    )
  }

  // Confirm Disconnect Dialog
  if (deletingConnection != null) {
    ConfirmDialog(
      title = "Putuskan Koneksi",
      message = "Apakah Anda yakin ingin memutuskan integrasi ${deletingConnection!!.providerName}? Transaksi yang telah diimpor sebelumnya akan tetap disimpan di PANTAU.",
      confirmText = "Putuskan",
      isDanger = true,
      onConfirm = {
        onDisconnectAccount(deletingConnection!!)
        deletingConnection = null
      },
      onDismiss = { deletingConnection = null }
    )
  }
}

@Composable
fun ProviderCard(
  provider: FinancialProvider,
  connection: FinancialConnectionEntity?,
  onConnect: () -> Unit,
  onSync: () -> Unit,
  onDisconnect: () -> Unit,
  onImportCsv: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isConnected = connection != null
  var showMenu by remember { mutableStateOf(false) }

  val brandColor = Color(android.graphics.Color.parseColor(provider.colorHex))

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .background(brandColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = provider.name.take(3).uppercase(),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = brandColor
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = if (isConnected) connection!!.accountAlias else provider.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (isConnected) connection!!.maskedAccountNumber else provider.accountType.label,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Status badge
        val statusText = if (isConnected) connection!!.status else "Belum Terhubung"
        val statusColor = when {
          isConnected && connection!!.status == "SYNC_SUCCESS" -> PantauSuccessGreen
          isConnected -> PantauPrimaryBlue
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = statusColor.copy(alpha = 0.12f)
        ) {
          Text(
            text = if (isConnected) "Terhubung" else "Belum Terhubung",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Provider API documentation / capability note
      Text(
        text = provider.officialNotes,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
      )

      if (isConnected) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Status: ${connection!!.lastSyncStatus}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          Text(
            text = "${connection.importedTransactionsCount} transaksi",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (isConnected) {
          Button(
            onClick = onSync,
            colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sinkronkan", style = MaterialTheme.typography.labelMedium)
          }
          OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.weight(1f)
          ) {
            Text("Putuskan", style = MaterialTheme.typography.labelMedium, color = PantauDangerRed)
          }
        } else {
          Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Hubungkan", style = MaterialTheme.typography.labelMedium)
          }
          OutlinedButton(
            onClick = onImportCsv,
            modifier = Modifier.weight(1f)
          ) {
            Text("Impor CSV", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectAccountDialog(
  preselectedProvider: FinancialProvider?,
  onDismiss: () -> Unit,
  onConfirm: (providerId: String, alias: String, maskedAccount: String, method: String) -> Unit
) {
  var selectedProvider by remember { mutableStateOf(preselectedProvider ?: FinancialProviders.allProviders.first()) }
  var isExpanded by remember { mutableStateOf(false) }
  var alias by remember { mutableStateOf(selectedProvider.name + " Pribadi") }
  var maskedAccount by remember { mutableStateOf(selectedProvider.defaultMaskedPlaceholder) }
  var integrationMethod by remember { mutableStateOf("OPEN_FINANCE_SANDBOX") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Hubungkan Sumber Keuangan", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Pilih penyedia e-wallet atau bank untuk menyinkronkan transaksi ke PANTAU secara otomatis:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Dropdown Provider
        ExposedDropdownMenuBox(
          expanded = isExpanded,
          onExpandedChange = { isExpanded = !isExpanded }
        ) {
          OutlinedTextField(
            value = selectedProvider.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Pilih Penyedia") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
          ) {
            FinancialProviders.allProviders.forEach { prov ->
              DropdownMenuItem(
                text = { Text("${prov.name} (${prov.accountType.label})") },
                onClick = {
                  selectedProvider = prov
                  alias = "${prov.name} Pribadi"
                  maskedAccount = prov.defaultMaskedPlaceholder
                  isExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = alias,
          onValueChange = { alias = it },
          label = { Text("Nama Alias Akun") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = maskedAccount,
          onValueChange = { maskedAccount = it },
          label = { Text("Nomor Akun / Rekening (Disamarkan)") },
          placeholder = { Text("cth: 0812-••••-8890 atau BCA •••• 1234") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Information banner
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Status Integrasi: ${selectedProvider.apiSupportStatus.label}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = PantauPrimaryBlue
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = selectedProvider.officialNotes,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(selectedProvider.id, alias, maskedAccount, integrationMethod)
        },
        colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
      ) {
        Text("Hubungkan Akun")
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
fun ImportCsvDialog(
  onDismiss: () -> Unit,
  onConfirmImport: (csvContent: String, providerId: String, connectionId: String?) -> Unit
) {
  var selectedProviderId by remember { mutableStateOf("bca") }
  var isExpanded by remember { mutableStateOf(false) }
  var csvText by remember { mutableStateOf(CsvStatementParser.getSampleCsv("bca")) }
  var parsedItems by remember { mutableStateOf(CsvStatementParser.parse(csvText, "bca")) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Impor Mutasi Transaksi CSV", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Impor file e-statement atau mutasi transaksi dari aplikasi bank atau e-wallet Anda tanpa membutuhkan kredensial login:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Format selector
        ExposedDropdownMenuBox(
          expanded = isExpanded,
          onExpandedChange = { isExpanded = !isExpanded }
        ) {
          OutlinedTextField(
            value = FinancialProviders.getProvider(selectedProviderId).name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Format Mutasi Penyedia") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
          ) {
            FinancialProviders.allProviders.forEach { prov ->
              DropdownMenuItem(
                text = { Text(prov.name) },
                onClick = {
                  selectedProviderId = prov.id
                  csvText = CsvStatementParser.getSampleCsv(prov.id)
                  parsedItems = CsvStatementParser.parse(csvText, prov.id)
                  isExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = csvText,
          onValueChange = {
            csvText = it
            parsedItems = CsvStatementParser.parse(it, selectedProviderId)
          },
          label = { Text("Isi Data CSV") },
          placeholder = { Text("Tempel data baris CSV di sini...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
          maxLines = 6
        )

        // Live preview of parsed records
        Text(
          text = "Pratinjau Hasil Pembacaan: ${parsedItems.size} baris ditemukan",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        parsedItems.take(3).forEach { item ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(FinancialEngine.formatDate(item.dateMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                text = (if (item.rawType == "INCOME") "+ " else "- ") + FinancialEngine.formatRupiah(item.amount),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.rawType == "INCOME") PantauSuccessGreen else PantauDangerRed
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedItems.isNotEmpty()) {
            onConfirmImport(csvText, selectedProviderId, null)
          }
        },
        enabled = parsedItems.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = PantauPrimaryBlue)
      ) {
        Text("Konfirmasi Impor (${parsedItems.size})")
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
fun ReviewPendingTransactionsDialog(
  pendingTransactions: List<TransactionEntity>,
  categories: List<CategoryEntity>,
  savingsGoals: List<SavingsGoalEntity>,
  onDismiss: () -> Unit,
  onConfirm: (
    txId: String,
    category: String,
    type: TransactionType,
    isInternalTransfer: Boolean,
    linkedGoalId: String?,
    saveAsRuleKeyword: String?
  ) -> Unit
) {
  var currentIndex by remember { mutableStateOf(0) }
  val currentTx = pendingTransactions.getOrNull(currentIndex)

  if (currentTx == null) {
    onDismiss()
    return
  }

  var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Pengeluaran Lainnya") }
  var selectedType by remember { mutableStateOf(if (currentTx.type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE) }
  var isInternalTransfer by remember { mutableStateOf(currentTx.isInternalTransfer) }
  var linkedGoalId by remember { mutableStateOf<String?>(null) }
  var saveRuleKeyword by remember { mutableStateOf("") }
  var isCategoryExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Tinjau Transaksi (${currentIndex + 1} dari ${pendingTransactions.size})", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("Deskripsi Asli:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(currentTx.note.ifBlank { currentTx.category }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text(FinancialEngine.formatDate(currentTx.dateMillis), style = MaterialTheme.typography.labelSmall)
              Text(FinancialEngine.formatRupiah(currentTx.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PantauPrimaryBlue)
            }
          }
        }

        Text("Klasifikasi Jenis Transaksi:", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = selectedType == TransactionType.EXPENSE && !isInternalTransfer && linkedGoalId == null,
            onClick = {
              selectedType = TransactionType.EXPENSE
              isInternalTransfer = false
              linkedGoalId = null
            },
            label = { Text("Pengeluaran") }
          )
          FilterChip(
            selected = selectedType == TransactionType.INCOME && !isInternalTransfer && linkedGoalId == null,
            onClick = {
              selectedType = TransactionType.INCOME
              isInternalTransfer = false
              linkedGoalId = null
            },
            label = { Text("Pemasukan") }
          )
          FilterChip(
            selected = isInternalTransfer,
            onClick = {
              isInternalTransfer = !isInternalTransfer
              linkedGoalId = null
            },
            label = { Text("Transfer Internal") }
          )
        }

        // Option to link to savings goal
        if (savingsGoals.isNotEmpty()) {
          Text("Atau Alokasikan ke Tabungan PANTAU:", style = MaterialTheme.typography.labelSmall)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            savingsGoals.forEach { goal ->
              FilterChip(
                selected = linkedGoalId == goal.id,
                onClick = {
                  linkedGoalId = if (linkedGoalId == goal.id) null else goal.id
                  if (linkedGoalId != null) {
                    isInternalTransfer = false
                  }
                },
                label = { Text("Setor ke ${goal.name}") }
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
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text("Pilih Kategori") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = isCategoryExpanded,
            onDismissRequest = { isCategoryExpanded = false }
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat.name) },
                onClick = {
                  selectedCategory = cat.name
                  isCategoryExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = saveRuleKeyword,
          onValueChange = { saveRuleKeyword = it },
          label = { Text("Ingat Kata Kunci untuk Otomatisasi (Opsional)") },
          placeholder = { Text("cth: indomaret, kopi, spbu") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(
            currentTx.id,
            selectedCategory,
            selectedType,
            isInternalTransfer,
            linkedGoalId,
            saveRuleKeyword.ifBlank { null }
          )
          if (currentIndex < pendingTransactions.size - 1) {
            currentIndex++
            saveRuleKeyword = ""
          } else {
            onDismiss()
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PantauSuccessGreen)
      ) {
        Text("Konfirmasi & Simpan")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Tutup")
      }
    }
  )
}
