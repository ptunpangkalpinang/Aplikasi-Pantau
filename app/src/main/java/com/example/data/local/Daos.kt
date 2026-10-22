package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.PreferenceEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, createdAt DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  suspend fun getTransactionById(id: String): TransactionEntity?

  @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateMillis DESC")
  fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(transaction: TransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<TransactionEntity>)

  @Update
  suspend fun update(transaction: TransactionEntity)

  @Delete
  suspend fun delete(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM transactions")
  suspend fun deleteAll()
}

@Dao
interface SavingsGoalDao {
  @Query("SELECT * FROM savings_goals ORDER BY createdAt ASC")
  fun getAllGoals(): Flow<List<SavingsGoalEntity>>

  @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
  suspend fun getGoalById(id: String): SavingsGoalEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(goal: SavingsGoalEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(goals: List<SavingsGoalEntity>)

  @Update
  suspend fun update(goal: SavingsGoalEntity)

  @Delete
  suspend fun delete(goal: SavingsGoalEntity)

  @Query("DELETE FROM savings_goals WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM savings_goals")
  suspend fun deleteAll()
}

@Dao
interface BudgetDao {
  @Query("SELECT * FROM budgets ORDER BY createdAt ASC")
  fun getAllBudgets(): Flow<List<BudgetEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(budget: BudgetEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(budgets: List<BudgetEntity>)

  @Update
  suspend fun update(budget: BudgetEntity)

  @Delete
  suspend fun delete(budget: BudgetEntity)

  @Query("DELETE FROM budgets WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM budgets")
  suspend fun deleteAll()
}

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY name ASC")
  fun getAllCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
  fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(category: CategoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(categories: List<CategoryEntity>)

  @Delete
  suspend fun delete(category: CategoryEntity)

  @Query("DELETE FROM categories WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM categories")
  suspend fun deleteAll()
}

@Dao
interface PreferenceDao {
  @Query("SELECT * FROM app_preferences")
  fun getAllPreferences(): Flow<List<PreferenceEntity>>

  @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
  suspend fun getPreference(key: String): String?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun setPreference(preference: PreferenceEntity)

  @Query("DELETE FROM app_preferences WHERE `key` = :key")
  suspend fun deletePreference(key: String)

  @Query("DELETE FROM app_preferences")
  suspend fun deleteAll()
}
