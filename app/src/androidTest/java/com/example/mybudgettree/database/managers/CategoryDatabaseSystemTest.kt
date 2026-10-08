package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class CategoryDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createCategory_success() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertTrue(result.wasSuccessful)
        assertEquals("Groceries", result.value?.categoryName)
    }

    @Test
    fun createCategory_getsGeneratedId() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertTrue(result.value!!.id.isNotBlank())
    }

    @Test
    fun createCategory_hasNoBudgetByDefault() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertNull(result.value?.budgetAmount)
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
        categoryDatabaseSystem.createCategory(userA, "Groceries")
        val userB = createTestUser("userb")
        val result = categoryDatabaseSystem.createCategory(userB, "Groceries")
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun createCategory_forUserWhoIsNotSignedIn_fails() = runBlocking {
        val userA = createTestUser("usera")
        createTestUser("userb")
        val result = categoryDatabaseSystem.createCategory(userA, "Groceries")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createCategory_blankName_fails() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun findCategory_byId_returnsCategory() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = categoryDatabaseSystem.findCategory(category.id)
        assertTrue(result.wasSuccessful)
        assertEquals(category, result.value)
    }

    @Test
    fun findCategory_byId_unknownId_fails() = runBlocking {
        createTestUser()
        val result = categoryDatabaseSystem.findCategory("doesNotExist")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun findCategory_byName_returnsCategory() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Groceries")
        val result = categoryDatabaseSystem.findCategory(user, "Groceries")
        assertTrue(result.wasSuccessful)
        assertEquals(category.id, result.value?.id)
    }

    @Test
    fun findCategory_byName_unknownName_fails() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.findCategory(user, "Nothing")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun getAllCategoriesForUser_returnsOnlyThatUsersCategories() = runBlocking {
        val userA = createTestUser("usera")
        createTestCategory(userA, "Groceries")
        createTestCategory(userA, "Fuel")
        val userB = createTestUser("userb")
        createTestCategory(userB, "Rent")
        val result = categoryDatabaseSystem.getAllCategoriesForUser(userB)
        assertTrue(result.wasSuccessful)
        assertEquals(listOf("Rent"), result.values?.map { it.categoryName })
    }

    @Test
    fun updateCategoryName_changesName_succeeds() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Groceries")
        val result = categoryDatabaseSystem.updateCategoryName(category, "Food")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("Food", result.value?.categoryName)
        assertEquals("Food", categoryDatabaseSystem.findCategory(category.id).value?.categoryName)
    }

    @Test
    fun updateCategoryName_nameAlreadyUsed_fails() = runBlocking {
        val user = createTestUser()
        createTestCategory(user, "Food")
        val category = createTestCategory(user, "Groceries")
        val result = categoryDatabaseSystem.updateCategoryName(category, "Food")
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateCategoryBudget_setsAmount_succeeds() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(1000.0, result.value?.budgetAmount)
        assertEquals(1000.0, categoryDatabaseSystem.findCategory(category.id).value?.budgetAmount)
    }

    @Test
    fun updateCategoryBudget_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = categoryDatabaseSystem.updateCategoryBudget(category, -100.0)
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateCategoryBudget_sameAmount_noChange() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val withBudget = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0).value!!
        val result = categoryDatabaseSystem.updateCategoryBudget(withBudget, 1000.0)
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateCategoryBudget_setToNull_removesBudget() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val withBudget = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0).value!!
        val result = categoryDatabaseSystem.updateCategoryBudget(withBudget, null)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertNull(result.value?.budgetAmount)
    }

    @Test
    fun updateCategoryBudget_categoryDoesNotExist_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        categoryDatabaseSystem.deleteCategory(category)
        val result = categoryDatabaseSystem.updateCategoryBudget(category, 1000.0)
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun createCategory_hasNoIconByDefault() = runBlocking {
        val user = createTestUser()
        val result = categoryDatabaseSystem.createCategory(user, "Groceries")
        assertNull(result.value?.iconKey)
    }

    @Test
    fun updateCategoryIcon_setsKey_succeeds() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = categoryDatabaseSystem.updateCategoryIcon(category, "groceries")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("groceries", result.value?.iconKey)
    }

    @Test
    fun updateCategoryIcon_sameKey_noChange() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val withIcon = categoryDatabaseSystem.updateCategoryIcon(category, "groceries").value!!
        val result = categoryDatabaseSystem.updateCategoryIcon(withIcon, "groceries")
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateCategoryIcon_setToNull_clearsIcon() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val withIcon = categoryDatabaseSystem.updateCategoryIcon(category, "groceries").value!!
        val result = categoryDatabaseSystem.updateCategoryIcon(withIcon, null)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertNull(result.value?.iconKey)
    }

    @Test
    fun updateCategoryIcon_categoryDoesNotExist_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        categoryDatabaseSystem.deleteCategory(category)
        val result = categoryDatabaseSystem.updateCategoryIcon(category, "groceries")
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun deleteCategory_removesCategory() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        assertEquals(DeleteReturnStatus.Deleted, categoryDatabaseSystem.deleteCategory(category))
        assertFalse(categoryDatabaseSystem.findCategory(category.id).wasSuccessful)
    }

    @Test
    fun deleteCategory_alreadyDeleted_doesNotExist() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        categoryDatabaseSystem.deleteCategory(category)
        assertEquals(DeleteReturnStatus.DoesNotExist, categoryDatabaseSystem.deleteCategory(category))
    }

    @Test
    fun deleteCategory_alsoDeletesItsExpensesAndIncomes_butNotOtherCategories() = runBlocking {
        val user = createTestUser()
        val doomed = createTestCategory(user, "Doomed")
        val kept = createTestCategory(user, "Kept")
        val date = LocalDate.of(2026, 10, 5)
        val time = LocalTime.of(12, 0)
        expenseDatabaseSystem.createExpense(doomed, "Lunch", 50.0, date, time, time)
        incomeDatabaseSystem.createIncome(doomed, "Refund", 20.0, date, time, time)
        expenseDatabaseSystem.createExpense(kept, "Dinner", 80.0, date, time, time)

        categoryDatabaseSystem.deleteCategory(doomed)

        assertEquals(listOf("Dinner"), expenseDatabaseSystem.retrieveAllExpenses(user).values?.map { it.description })
        assertTrue(incomeDatabaseSystem.retrieveAllIncomes(user).values!!.isEmpty())
    }
}
