package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.SavingsGoalEntity
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
    PreferenceEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class PantauDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao
  abstract fun savingsGoalDao(): SavingsGoalDao
  abstract fun budgetDao(): BudgetDao
  abstract fun categoryDao(): CategoryDao
  abstract fun preferenceDao(): PreferenceDao

  companion object {
    @Volatile
    private var INSTANCE: PantauDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): PantauDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          PantauDatabase::class.java,
          "pantau_database.db"
        )
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
