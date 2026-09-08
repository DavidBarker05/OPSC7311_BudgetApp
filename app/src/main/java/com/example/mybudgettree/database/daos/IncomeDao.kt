package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.Income
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface IncomeDao {
    @Insert
    suspend fun insertIncome(income: Income): Long

    @Query("""
        UPDATE incomes
        SET description = :newDescription
        WHERE id = :id
    """)
    suspend fun updateIncomeDescription(id: Long, newDescription: String)

    @Query("""
        UPDATE INCOMES
        SET amount = :amount
        WHERE id = :id
    """)
    suspend fun updateIncomeAmount(id: Long, amount: Double)

    @Query("""
        UPDATE incomes
        SET date = :newDate
        WHERE id = :id
    """)
    suspend fun updateIncomeDate(id: Long, newDate: LocalDate)

    @Query("""
        UPDATE incomes
        SET start_time = :newStartTime
        WHERE id = :id
    """)
    suspend fun updateIncomeStartTime(id: Long, newStartTime: LocalTime)

    @Query("""
        UPDATE incomes
        SET end_time = :newEndTime
        WHERE id = :id
    """)
    suspend fun updateIncomeEndTime(id: Long, newEndTime: LocalTime)

    @Query("""
        UPDATE incomes
        SET image_path = :newImagePath
        WHERE id = :id
    """)
    suspend fun updateIncomeImage(id: Long, newImagePath: String?)

    @Query("""
        SELECT *
        FROM incomes
        WHERE id = :id
    """)
    suspend fun findIncome(id: Long): Income?

    @Query("""
        SELECT incomes.*
        FROM incomes
        INNER JOIN categories
        ON incomes.category_id = categories.id
        WHERE categories.username = :username
    """)
    suspend fun retrieveAllIncomes(username: String): List<Income>

    @Query("""
        SELECT incomes.*
        FROM incomes
        INNER JOIN categories
        ON incomes.category_id = categories.id
        WHERE categories.username = :username AND incomes.description = :description
    """)
    suspend fun retrieveAllIncomesByDescription(username: String, description: String): List<Income>

    @Query("""
        SELECT incomes.*
        FROM incomes
        INNER JOIN categories
        ON incomes.category_id = categories.id
        WHERE categories.username = :username AND incomes.date = :date
    """)
    suspend fun retrieveAllIncomesOnDate(username: String, date: LocalDate): List<Income>

    @Query("""
        SELECT incomes.*
        FROM incomes
        INNER JOIN categories
        ON incomes.category_id = categories.id
        WHERE categories.username = :username AND incomes.date = :date AND incomes.description = :description
    """)
    suspend fun retrieveAllIncomesByDescriptionOnDate(username: String, date: LocalDate, description: String): List<Income>

    @Query("""
        SELECT incomes.*
        FROM incomes
        INNER JOIN categories
        ON incomes.category_id = categories.id
        WHERE categories.username = :username AND incomes.date BETWEEN :startDate AND :endDate
    """)
    suspend fun retrieveAllIncomesBetweenDates(username: String, startDate: LocalDate, endDate: LocalDate): List<Income>

    @Query("""
        SELECT incomes.*
        FROM incomes
        INNER JOIN categories
        ON incomes.category_id = categories.id
        WHERE categories.username = :username AND incomes.date BETWEEN :startDate AND :endDate AND incomes.description = :description
    """)
    suspend fun retrieveAllIncomesByDescriptionBetweenDates(username: String, startDate: LocalDate, endDate: LocalDate, description: String): List<Income>

    @Query("""
        SELECT *
        FROM incomes
        WHERE category_id = :categoryId
    """)
    suspend fun retrieveAllIncomesForCategory(categoryId: Long): List<Income>

    @Query("""
        SELECT *
        FROM incomes
        WHERE category_id = :categoryId AND description = :description
    """)
    suspend fun retrieveAllIncomesByDescriptionForCategory(categoryId: Long, description: String): List<Income>

    @Query("""
        SELECT *
        FROM incomes
        WHERE category_id = :categoryId AND date = :date
    """)
    suspend fun retrieveAllIncomesOnDateForCategory(categoryId: Long, date: LocalDate): List<Income>

    @Query("""
        SELECT *
        FROM incomes
        WHERE category_id = :categoryId AND description = :description AND date = :date
    """)
    suspend fun retrieveAllIncomesByDescriptionOnDateForCategory(categoryId: Long, description: String, date: LocalDate): List<Income>

    @Query("""
        SELECT *
        FROM incomes
        WHERE category_id = :categoryId AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun retrieveAllIncomesBetweenDatesForCategory(categoryId: Long, startDate: LocalDate, endDate: LocalDate): List<Income>

    @Query("""
        SELECT *
        FROM incomes
        WHERE category_id = :categoryId AND description = :description AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun retrieveAllIncomesByDescriptionBetweenDatesForCategory(categoryId: Long, description: String, startDate: LocalDate, endDate: LocalDate): List<Income>

    @Delete
    suspend fun deleteIncome(income: Income): Int
}
