package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.Budget
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBudget(budget: Budget): Long

    @Query("""
        UPDATE budgets
        SET amount = :newAmount
        WHERE id = :id
    """)
    suspend fun updateBudgetAmount(id: Long, newAmount: Double)

    @Query("""
        UPDATE budgets
        SET currency = :newCurrency
        WHERE id = :id
    """)
    suspend fun updateBudgetCurrency(id: Long, newCurrency: String)

    @Delete
    suspend fun deleteBudget(budget: Budget): Int

    @Query("""
        SELECT *
        FROM budgets
        WHERE id = :id
    """)
    suspend fun findBudget(id: Long): Budget?

    @Query("""
        SELECT *
        FROM budgets
        WHERE category_id = :categoryId
    """)
    suspend fun findBudgetForCategory(categoryId: Long): Budget?
}
