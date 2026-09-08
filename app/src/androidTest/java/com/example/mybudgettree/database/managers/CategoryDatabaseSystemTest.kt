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
class CategoryDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createCategory_success() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertTrue(result.wasSuccessful)
        assertEquals("Groceries", result.category?.categoryName)
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
}
