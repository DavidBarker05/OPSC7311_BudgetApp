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
class IncomeDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createIncome_success() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")
        val result = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Monthly salary",
            amount = 15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )
        assertTrue(result.wasSuccessful)
        assertEquals("Monthly salary", result.income?.description)
    }

    @Test
    fun createIncome_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")
        val result = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Monthly salary",
            amount = -15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllIncomes_returnsCreatedIncome() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")
        incomeDatabaseSystem.createIncome(
            category = category,
            description = "Monthly salary",
            amount = 15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )
        val result = incomeDatabaseSystem.retrieveAllIncomes(user)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.incomes?.size)
    }
}
