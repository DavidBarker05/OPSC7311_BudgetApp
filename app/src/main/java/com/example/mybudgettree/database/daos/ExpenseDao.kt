package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.Expense
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Query("""
        UPDATE expenses
        SET description = :newDescription
        WHERE id = :id
    """)
    suspend fun updateExpenseDescription(id: Long, newDescription: String)

    @Query("""
        UPDATE EXPENSES
        SET amount = :amount
        WHERE id = :id
    """)
    suspend fun updateExpenseAmount(id: Long, amount: Double)

    @Query("""
        UPDATE expenses
        SET date = :newDate
        WHERE id = :id
    """)
    suspend fun updateExpenseDate(id: Long, newDate: LocalDate)

    @Query("""
        UPDATE expenses
        SET start_time = :newStartTime
        WHERE id = :id
    """)
    suspend fun updateExpenseStartTime(id: Long, newStartTime: LocalTime)

    @Query("""
        UPDATE expenses
        SET end_time = :newEndTime
        WHERE id = :id
    """)
    suspend fun updateExpenseEndTime(id: Long, newEndTime: LocalTime)

    @Query("""
        UPDATE expenses
        SET image_path = :newImagePath
        WHERE id = :id
    """)
    suspend fun updateExpenseImage(id: Long, newImagePath: String?)

    @Query("""
        SELECT *
        FROM expenses
        WHERE id = :id
    """)
    suspend fun findExpense(id: Long): Expense?

    @Query("""
        SELECT expenses.*
        FROM expenses
        INNER JOIN categories
        ON expenses.category_id = categories.id
        WHERE categories.username = :username
    """)
    suspend fun retrieveAllExpenses(username: String): List<Expense>

    @Query("""
        SELECT expenses.*
        FROM expenses
        INNER JOIN categories
        ON expenses.category_id = categories.id
        WHERE categories.username = :username AND expenses.description = :description
    """)
    suspend fun retrieveAllExpensesByDescription(username: String, description: String): List<Expense>

    @Query("""
        SELECT expenses.*
        FROM expenses
        INNER JOIN categories
        ON expenses.category_id = categories.id
        WHERE categories.username = :username AND expenses.date = :date
    """)
    suspend fun retrieveAllExpensesOnDate(username: String, date: LocalDate): List<Expense>

    @Query("""
        SELECT expenses.*
        FROM expenses
        INNER JOIN categories
        ON expenses.category_id = categories.id
        WHERE categories.username = :username AND expenses.date = :date AND expenses.description = :description
    """)
    suspend fun retrieveAllExpensesByDescriptionOnDate(username: String, date: LocalDate, description: String): List<Expense>

    @Query("""
        SELECT expenses.*
        FROM expenses
        INNER JOIN categories
        ON expenses.category_id = categories.id
        WHERE categories.username = :username AND expenses.date BETWEEN :startDate AND :endDate
    """)
    suspend fun retrieveAllExpensesBetweenDates(username: String, startDate: LocalDate, endDate: LocalDate): List<Expense>

    @Query("""
        SELECT expenses.*
        FROM expenses
        INNER JOIN categories
        ON expenses.category_id = categories.id
        WHERE categories.username = :username AND expenses.date BETWEEN :startDate AND :endDate AND expenses.description = :description
    """)
    suspend fun retrieveAllExpensesByDescriptionBetweenDates(username: String, startDate: LocalDate, endDate: LocalDate, description: String): List<Expense>

    @Query("""
        SELECT *
        FROM expenses
        WHERE category_id = :categoryId
    """)
    suspend fun retrieveAllExpensesForCategory(categoryId: Long): List<Expense>

    @Query("""
        SELECT *
        FROM expenses
        WHERE category_id = :categoryId AND description = :description
    """)
    suspend fun retrieveAllExpensesByDescriptionForCategory(categoryId: Long, description: String): List<Expense>

    @Query("""
        SELECT *
        FROM expenses
        WHERE category_id = :categoryId AND date = :date
    """)
    suspend fun retrieveAllExpensesOnDateForCategory(categoryId: Long, date: LocalDate): List<Expense>

    @Query("""
        SELECT *
        FROM expenses
        WHERE category_id = :categoryId AND description = :description AND date = :date
    """)
    suspend fun retrieveAllExpensesByDescriptionOnDateForCategory(categoryId: Long, description: String, date: LocalDate): List<Expense>

    @Query("""
        SELECT *
        FROM expenses
        WHERE category_id = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun retrieveAllExpensesBetweenDatesForCategory(categoryId: Long, startDate: LocalDate, endDate: LocalDate): List<Expense>

    @Query("""
        SELECT *
        FROM expenses
        WHERE category_id = :categoryId AND description = :description AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun retrieveAllExpensesByDescriptionBetweenDatesForCategory(categoryId: Long, description: String, startDate: LocalDate, endDate: LocalDate): List<Expense>

    @Delete
    suspend fun deleteExpense(expense: Expense): Int
}