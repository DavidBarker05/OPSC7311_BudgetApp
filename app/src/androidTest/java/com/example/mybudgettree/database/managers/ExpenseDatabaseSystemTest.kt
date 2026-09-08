package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ExpenseDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createExpense_success() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Milk",
            currencyAtTime = "ZAR",
            amount = 25.50,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )
        assertTrue(result.wasSuccessful)
        assertEquals("Milk", result.expense?.description)
    }

    @Test
    fun createExpense_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Milk",
            currencyAtTime = "ZAR",
            amount = -25.50,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createExpense_endTimeBeforeStartTime_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Milk",
            currencyAtTime = "ZAR",
            amount = 25.50,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 5),
            endTime = LocalTime.of(9, 0)
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllExpenses_returnsCreatedExpense() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        expenseDatabaseSystem.createExpense(
            category = category,
            description = "Milk",
            currencyAtTime = "ZAR",
            amount = 25.50,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )
        val result = expenseDatabaseSystem.retrieveAllExpenses(user)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.expenses?.size)
    }
}
