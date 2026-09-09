package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createCategory_success() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertTrue(result.wasSuccessful)
        assertEquals("Groceries", result.category?.categoryName)
    }

    @Test
    fun createCategory_hasNoBudgetByDefault() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertNull(result.category?.budgetAmount)
    }

    @Test
    fun createCategory_duplicateNameForSameUser_fails() = runBlocking {
        val user = createTestUser()
        categoryDatabaseSystem.createCategory(user, "Groceries")
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createCategory_sameNameDifferentUser_succeeds() = runBlocking {
        val userA = createTestUser("usera")
        val userB = createTestUser("userb")
        categoryDatabaseSystem.createCategory(userA, "Groceries")
        val result = categoryDatabaseSystem.createCategory(userB, "Groceries")
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun createCategory_blankName_fails() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun updateCategoryBudget_setsAmount_succeeds() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0)
        assertEquals(CategoryDatabaseSystem.UpdateCategoryReturnStatus.Succeeded, result.status)
        assertEquals(1000.0, result.category?.budgetAmount)
    }

    @Test
    fun updateCategoryBudget_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = categoryDatabaseSystem.updateCategoryBudget(category, -100.0)
        assertEquals(CategoryDatabaseSystem.UpdateCategoryReturnStatus.Failed, result.status)
    }

    @Test
    fun updateCategoryBudget_sameAmount_noChange() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val withBudget = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0).category!!
        val result = categoryDatabaseSystem.updateCategoryBudget(withBudget, 1000.0)
        assertEquals(CategoryDatabaseSystem.UpdateCategoryReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateCategoryBudget_setToNull_removesBudget() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val withBudget = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0).category!!
        val result = categoryDatabaseSystem.updateCategoryBudget(withBudget, null)
        assertEquals(CategoryDatabaseSystem.UpdateCategoryReturnStatus.Succeeded, result.status)
        assertNull(result.category?.budgetAmount)
    }

    @Test
    fun updateCategoryBudget_categoryDoesNotExist_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        categoryDatabaseSystem.deleteCategory(category)
        val result = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0)
        assertEquals(CategoryDatabaseSystem.UpdateCategoryReturnStatus.Failed, result.status)
    }
}
