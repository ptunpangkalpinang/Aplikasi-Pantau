package com.example.data.local

import com.example.data.model.CategoryEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.util.Calendar
import java.util.UUID

object DefaultData {
  val defaultIncomeCategories = listOf(
    "Gaji Pokok",
    "Tunjangan",
    "Uang Lembur",
    "Bonus",
    "THR",
    "Usaha atau Bisnis",
    "Freelance",
    "Hadiah",
    "Bunga atau Hasil Investasi",
    "Pemasukan Lainnya"
  )

  val defaultExpenseCategories = listOf(
    "Makanan dan Kebutuhan Pokok",
    "Belanja Bulanan",
    "Tagihan Listrik dan Air",
    "Internet dan Pulsa",
    "Transportasi",
    "Bensin dan Parkir",
    "Tempat Tinggal",
    "Kesehatan",
    "Hiburan",
    "Keluarga",
    "Pendidikan",
    "Cicilan",
    "Pembayaran Utang",
    "Langganan",
    "Perawatan Pribadi",
    "Donasi",
    "Pengeluaran Lainnya"
  )

  fun getInitialCategories(): List<CategoryEntity> {
    val list = mutableListOf<CategoryEntity>()
    defaultIncomeCategories.forEach { name ->
      list.add(CategoryEntity(id = "inc_${name.replace(" ", "_").lowercase()}", name = name, type = "INCOME", isDefault = true))
    }
    defaultExpenseCategories.forEach { name ->
      list.add(CategoryEntity(id = "exp_${name.replace(" ", "_").lowercase()}", name = name, type = "EXPENSE", isDefault = true))
    }
    return list
  }

  // Pre-configured templates for creating new savings goals
  data class GoalTemplate(
    val name: String,
    val iconName: String,
    val accentColorHex: String,
    val defaultTarget: Long
  )

  val savingsTemplates = listOf(
    GoalTemplate("Dana Darurat", "Shield", "#2563EB", 15_000_000L),
    GoalTemplate("Tabungan Nikah", "Heart", "#EC4899", 50_000_000L),
    GoalTemplate("Tabungan Rumah", "Home", "#10B981", 100_000_000L),
    GoalTemplate("Tabungan Kendaraan", "DirectionsCar", "#F59E0B", 30_000_000L),
    GoalTemplate("Tabungan Liburan", "Flight", "#06B6D4", 12_000_000L),
    GoalTemplate("Tabungan Gadget", "Smartphone", "#8B5CF6", 15_000_000L),
    GoalTemplate("Tabungan Pendidikan", "School", "#6366F1", 25_000_000L),
    GoalTemplate("Tabungan Ibadah", "Mosque", "#14B8A6", 40_000_000L),
    GoalTemplate("Tabungan Pensiun", "Elderly", "#64748B", 200_000_000L),
    GoalTemplate("Tabungan Investasi", "TrendingUp", "#3B82F6", 20_000_000L),
    GoalTemplate("Tabungan Usaha", "Store", "#F97316", 50_000_000L),
    GoalTemplate("Tabungan Anak", "ChildCare", "#F43F5E", 20_000_000L),
    GoalTemplate("Tabungan Kesehatan", "LocalHospital", "#EF4444", 10_000_000L),
    GoalTemplate("Tabungan Renovasi", "Build", "#84CC16", 15_000_000L),
    GoalTemplate("Tabungan Bebas", "Savings", "#0EA5E9", 5_000_000L)
  )

  fun generateDemoData(): DemoDataSet {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    // Create 3 demo savings goals
    val goalNikah = SavingsGoalEntity(
      id = "demo_goal_nikah",
      name = "Tabungan Nikah",
      iconName = "Heart",
      accentColorHex = "#EC4899",
      targetAmount = 50_000_000L,
      currentAmount = 17_500_000L,
      startDateMillis = now - 90L * 86400000L,
      deadlineDateMillis = now + 180L * 86400000L,
      routineTargetAmount = 2_000_000L,
      routineFrequency = "MONTHLY",
      note = "Persiapan resepsi dan akad",
      isCompleted = false
    )

    val goalDarurat = SavingsGoalEntity(
      id = "demo_goal_darurat",
      name = "Dana Darurat",
      iconName = "Shield",
      accentColorHex = "#2563EB",
      targetAmount = 20_000_000L,
      currentAmount = 14_000_000L,
      startDateMillis = now - 120L * 86400000L,
      deadlineDateMillis = now + 60L * 86400000L,
      routineTargetAmount = 1_500_000L,
      routineFrequency = "MONTHLY",
      note = "Target 6x pengeluaran bulanan",
      isCompleted = false
    )

    val goalLiburan = SavingsGoalEntity(
      id = "demo_goal_liburan",
      name = "Tabungan Liburan",
      iconName = "Flight",
      accentColorHex = "#06B6D4",
      targetAmount = 10_000_000L,
      currentAmount = 4_500_000L,
      startDateMillis = now - 60L * 86400000L,
      deadlineDateMillis = now + 90L * 86400000L,
      routineTargetAmount = 1_000_000L,
      routineFrequency = "MONTHLY",
      note = "Liburan akhir tahun ke Jepang",
      isCompleted = false
    )

    val goals = listOf(goalNikah, goalDarurat, goalLiburan)

    // Transactions over the last few days
    val transactions = mutableListOf<TransactionEntity>()

    // Salary (Income)
    cal.timeInMillis = now
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val salaryDate = cal.timeInMillis
    transactions.add(
      TransactionEntity(
        id = "demo_tx_1",
        type = TransactionType.INCOME.name,
        amount = 12_500_000L,
        category = "Gaji Pokok",
        dateMillis = salaryDate,
        source = "PT Maju Gemilang",
        note = "Gaji bulanan reguler",
        isRecurringSalary = true
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_2",
        type = TransactionType.INCOME.name,
        amount = 3_000_000L,
        category = "Freelance",
        dateMillis = now - 8L * 86400000L,
        source = "Klien Desain Web",
        note = "Proyek UI/UX landing page",
        isRecurringSalary = false
      )
    )

    // Savings deposits
    transactions.add(
      TransactionEntity(
        id = "demo_tx_3",
        type = TransactionType.SAVINGS_DEPOSIT.name,
        amount = 2_000_000L,
        category = "Tabungan Nikah",
        dateMillis = now - 15L * 86400000L,
        source = "Transfer Bank",
        note = "Setoran rutin bulan ini",
        savingsGoalId = goalNikah.id
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_4",
        type = TransactionType.SAVINGS_DEPOSIT.name,
        amount = 1_500_000L,
        category = "Dana Darurat",
        dateMillis = now - 14L * 86400000L,
        source = "Transfer Bank",
        note = "Top-up dana darurat",
        savingsGoalId = goalDarurat.id
      )
    )

    // Expenses
    transactions.add(
      TransactionEntity(
        id = "demo_tx_5",
        type = TransactionType.EXPENSE.name,
        amount = 1_850_000L,
        category = "Makanan dan Kebutuhan Pokok",
        dateMillis = now - 2L * 86400000L,
        source = "QRIS",
        note = "Makan siang kantor & makan malam keluarga"
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_6",
        type = TransactionType.EXPENSE.name,
        amount = 1_200_000L,
        category = "Belanja Bulanan",
        dateMillis = now - 10L * 86400000L,
        source = "Transfer Bank",
        note = "Supermarket stok perlengkapan rumah"
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_7",
        type = TransactionType.EXPENSE.name,
        amount = 450_000L,
        category = "Tagihan Listrik dan Air",
        dateMillis = now - 12L * 86400000L,
        source = "E-Wallet",
        note = "Token PLN & tagihan PDAM"
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_8",
        type = TransactionType.EXPENSE.name,
        amount = 350_000L,
        category = "Internet dan Pulsa",
        dateMillis = now - 5L * 86400000L,
        source = "Transfer Bank",
        note = "Tagihan WiFi Indihome"
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_9",
        type = TransactionType.EXPENSE.name,
        amount = 600_000L,
        category = "Bensin dan Parkir",
        dateMillis = now - 3L * 86400000L,
        source = "Tunai",
        note = "Isi Pertamax motor & mobil"
      )
    )

    transactions.add(
      TransactionEntity(
        id = "demo_tx_10",
        type = TransactionType.EXPENSE.name,
        amount = 500_000L,
        category = "Hiburan",
        dateMillis = now - 1L * 86400000L,
        source = "QRIS",
        note = "Nonton bioskop & kopi akhir pekan"
      )
    )

    // Initial balance: 5,000,000
    return DemoDataSet(
      initialBalance = 5_000_000L,
      goals = goals,
      transactions = transactions
    )
  }
}

data class DemoDataSet(
  val initialBalance: Long,
  val goals: List<SavingsGoalEntity>,
  val transactions: List<TransactionEntity>
)
