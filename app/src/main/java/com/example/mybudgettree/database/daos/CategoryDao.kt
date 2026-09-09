package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.Category
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Query("""
        UPDATE categories
        SET category_name = :newName
        WHERE id = :id
    """)
    suspend fun updateCategoryName(id: Long, newName: String)

    @Query("""
        UPDATE categories
        SET budget_amount = :newBudgetAmount
        WHERE id = :id
    """)
    suspend fun updateCategoryBudget(id: Long, newBudgetAmount: Double?)

    @Delete
    suspend fun deleteCategory(category: Category): Int

    @Query("""
        SELECT *
        FROM categories
        WHERE id = :id
    """)
    suspend fun findCategory(id: Long): Category?

    @Query("""
        SELECT *
        FROM categories
        WHERE username = :username AND category_name = :categoryName
    """)
    suspend fun findCategory(username: String, categoryName: String): Category?

    @Query("""
        SELECT *
        FROM categories
        WHERE username = :username
    """)
    suspend fun retrieveAllCategories(username: String): List<Category>
}