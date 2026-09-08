package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createBudget_success() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = budgetDatabaseSystem.createBudget(category, "ZAR", 1000.0)
        assertTrue(result.wasSuccessful)
        assertEquals(1000.0, result.budget?.amount)
    }

    @Test
    fun createBudget_secondBudgetForSameCategory_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        budgetDatabaseSystem.createBudget(category, "ZAR", 1000.0)
        val result = budgetDatabaseSystem.createBudget(category, "ZAR", 500.0)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createBudget_forDifferentCategories_bothSucceed() = runBlocking {
        val user = createTestUser()
        val groceries = createTestCategory(user, "Groceries")
        val transport = createTestCategory(user, "Transport")
        val groceriesResult = budgetDatabaseSystem.createBudget(groceries, "ZAR", 1000.0)
        val transportResult = budgetDatabaseSystem.createBudget(transport, "ZAR", 500.0)
        assertTrue(groceriesResult.wasSuccessful)
        assertTrue(transportResult.wasSuccessful)
    }

    @Test
    fun createBudget_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = budgetDatabaseSystem.createBudget(category, "ZAR", -100.0)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun findBudgetForCategory_afterCreate_succeeds() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        budgetDatabaseSystem.createBudget(category, "ZAR", 1000.0)
        val result = budgetDatabaseSystem.findBudgetForCategory(category)
        assertTrue(result.wasSuccessful)
        assertEquals(1000.0, result.budget?.amount)
    }
}
