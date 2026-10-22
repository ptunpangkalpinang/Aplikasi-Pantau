package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.PantauPrimaryBlue
import com.example.ui.viewmodel.PantauUiState
import com.example.ui.viewmodel.SortOrder

@Composable
fun TransactionsScreen(
  uiState: PantauUiState,
  onSearchQueryChange: (String) -> Unit,
  onCategoryFilterChange: (String) -> Unit,
  onTypeFilterChange: (String) -> Unit,
  onSortOrderChange: (SortOrder) -> Unit,
  onEditTx: (TransactionEntity) -> Unit,
  onDeleteTx: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val tabs = listOf(
    "ALL" to "Semua",
    "INCOME" to "Pemasukan",
    "EXPENSE" to "Pengeluaran",
    "SAVINGS" to "Transfer Tabungan"
  )
  val selectedTabIndex = tabs.indexOfFirst { it.first == uiState.selectedTypeFilter }.coerceAtLeast(0)
  var showSortMenu by remember { mutableStateOf(false) }

  // Filter and sort transactions
  val filteredTransactions = remember(
    uiState.transactions,
    uiState.selectedTypeFilter,
    uiState.searchQuery,
    uiState.selectedCategoryFilter,
    uiState.sortOrder
  ) {
    uiState.transactions.filter { tx ->
      // Type match
      val typeMatch = when (uiState.selectedTypeFilter) {
        "ALL" -> true
        "INCOME" -> tx.type == TransactionType.INCOME.name
        "EXPENSE" -> tx.type == TransactionType.EXPENSE.name
        "SAVINGS" -> tx.type == TransactionType.SAVINGS_DEPOSIT.name || tx.type == TransactionType.SAVINGS_WITHDRAWAL.name
        else -> true
      }

      // Search match
      val query = uiState.searchQuery.trim().lowercase()
      val searchMatch = query.isEmpty() ||
        tx.category.lowercase().contains(query) ||
        tx.note.lowercase().contains(query) ||
        tx.source.lowercase().contains(query)

      // Category match
      val categoryMatch = uiState.selectedCategoryFilter.isEmpty() ||
        tx.category.equals(uiState.selectedCategoryFilter, ignoreCase = true)

      typeMatch && searchMatch && categoryMatch
    }.let { list ->
      when (uiState.sortOrder) {
        SortOrder.NEWEST -> list.sortedWith(compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.createdAt })
        SortOrder.OLDEST -> list.sortedWith(compareBy<TransactionEntity> { it.dateMillis }.thenBy { it.createdAt })
        SortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
        SortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("transactions_screen")
  ) {
    // 1. Subcategories Tabs
    TabRow(
      selectedTabIndex = selectedTabIndex,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = PantauPrimaryBlue
    ) {
      tabs.forEachIndexed { index, (key, label) ->
        Tab(
          selected = index == selectedTabIndex,
          onClick = { onTypeFilterChange(key) },
          text = {
            Text(
              text = label,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = if (index == selectedTabIndex) FontWeight.Bold else FontWeight.Normal
            )
          }
        )
      }
    }

    // 2. Search & Sort Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Cari kategori, sumber, catatan...", style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
      )

      Box {
        IconButton(
          onClick = { showSortMenu = true },
          modifier = Modifier.padding(start = 6.dp)
        ) {
          Icon(Icons.Default.Sort, contentDescription = "Urutkan", tint = PantauPrimaryBlue)
        }

        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
          SortOrder.entries.forEach { order ->
            DropdownMenuItem(
              text = {
                Text(
                  text = order.title,
                  fontWeight = if (uiState.sortOrder == order) FontWeight.Bold else FontWeight.Normal
                )
              },
              onClick = {
                onSortOrderChange(order)
                showSortMenu = false
              }
            )
          }
        }
      }
    }

    // 3. Category Filter Chips (if any available)
    val availableCategories = remember(uiState.transactions) {
      uiState.transactions.map { it.category }.distinct()
    }
    if (availableCategories.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = uiState.selectedCategoryFilter.isEmpty(),
          onClick = { onCategoryFilterChange("") },
          label = { Text("Semua Kategori", style = MaterialTheme.typography.labelSmall) }
        )
        availableCategories.forEach { cat ->
          FilterChip(
            selected = uiState.selectedCategoryFilter == cat,
            onClick = {
              onCategoryFilterChange(if (uiState.selectedCategoryFilter == cat) "" else cat)
            },
            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
          )
        }
      }
    }

    // 4. Transaction List
    if (filteredTransactions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Tidak ada transaksi yang cocok",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
      ) {
        item {
          Text(
            text = "Total ${filteredTransactions.size} Transaksi",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
          )
        }
        items(filteredTransactions, key = { it.id }) { tx ->
          TransactionRowItem(
            transaction = tx,
            onEdit = { onEditTx(tx) },
            onDelete = { onDeleteTx(tx) }
          )
        }
        item {
          Spacer(modifier = Modifier.height(32.dp))
        }
      }
    }
  }
}
