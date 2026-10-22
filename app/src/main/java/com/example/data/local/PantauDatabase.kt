package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryRuleEntity
import com.example.data.model.FinancialConnectionEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.SyncLogEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    TransactionEntity::class,
    SavingsGoalEntity::class,
    BudgetEntity::class,
    CategoryEntity::class,
    PreferenceEntity::class,
    FinancialConnectionEntity::class,
    CategoryRuleEntity::class,
    SyncLogEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class PantauDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao
  abstract fun savingsGoalDao(): SavingsGoalDao
  abstract fun budgetDao(): BudgetDao
  abstract fun categoryDao(): CategoryDao
  abstract fun preferenceDao(): PreferenceDao
  abstract fun financialConnectionDao(): FinancialConnectionDao
  abstract fun categoryRuleDao(): CategoryRuleDao
  abstract fun syncLogDao(): SyncLogDao

  companion object {
    @Volatile
    private var INSTANCE: PantauDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN sourceTransactionId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN connectionId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isInternalTransfer INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN targetAccountId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN reviewStatus TEXT NOT NULL DEFAULT 'CONFIRMED'")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS financial_connections (
            id TEXT PRIMARY KEY NOT NULL,
            providerId TEXT NOT NULL,
            providerName TEXT NOT NULL,
            accountType TEXT NOT NULL,
            accountAlias TEXT NOT NULL,
            maskedAccountNumber TEXT NOT NULL,
            status TEXT NOT NULL,
            integrationMethod TEXT NOT NULL,
            lastSyncMillis INTEGER NOT NULL,
            lastSyncStatus TEXT NOT NULL,
            importedTransactionsCount INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
          )
        """.trimIndent())

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS category_rules (
            id TEXT PRIMARY KEY NOT NULL,
            keyword TEXT NOT NULL,
            targetCategory TEXT NOT NULL,
            transactionType TEXT NOT NULL,
            createdAt INTEGER NOT NULL
          )
        """.trimIndent())

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS sync_logs (
            id TEXT PRIMARY KEY NOT NULL,
            connectionId TEXT NOT NULL,
            providerName TEXT NOT NULL,
            timestampMillis INTEGER NOT NULL,
            status TEXT NOT NULL,
            itemsCount INTEGER NOT NULL,
            notes TEXT NOT NULL
          )
        """.trimIndent())
      }
    }

    fun getDatabase(context: Context, scope: CoroutineScope): PantauDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          PantauDatabase::class.java,
          "pantau_database.db"
        )
        .addMigrations(MIGRATION_1_2)
        .addCallback(object : Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
              val database = INSTANCE ?: return@launch
              database.categoryDao().insertAll(DefaultData.getInitialCategories())
              database.preferenceDao().setPreference(PreferenceEntity("initial_balance", "0"))
              database.preferenceDao().setPreference(PreferenceEntity("salary_source", "Kantor"))
              database.preferenceDao().setPreference(PreferenceEntity("expected_salary", "0"))
              database.preferenceDao().setPreference(PreferenceEntity("salary_day", "25"))
              database.preferenceDao().setPreference(PreferenceEntity("allow_negative_balance", "true"))
              database.preferenceDao().setPreference(PreferenceEntity("theme_mode", "SYSTEM"))
            }
          }
        })
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
