package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
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

    private suspend fun addExpense(
        category: com.example.mybudgettree.database.entries.Category,
        description: String = "Milk",
        amount: Double = 25.50,
        date: LocalDate = LocalDate.of(2026, 1, 1)
    ) = expenseDatabaseSystem.createExpense(
        category = category,
        description = description,
        amount = amount,
        date = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 5)
    )

    @Test
    fun createExpense_success() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addExpense(category)
        assertTrue(result.wasSuccessful)
        assertEquals("Milk", result.value?.description)
        assertEquals(category.id, result.value?.categoryId)
        assertTrue(result.value!!.id.isNotBlank())
    }

    @Test
    fun createExpense_hasNoImageByDefault() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addExpense(category)
        assertTrue(result.value!!.imagePaths.isEmpty())
        assertFalse(result.value!!.hasImage())
    }

    @Test
    fun createExpense_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addExpense(category, amount = -25.50)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createExpense_blankDescription_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addExpense(category, description = " ")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createExpense_endTimeBeforeStartTime_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Milk",
            amount = 25.50,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 5),
            endTime = LocalTime.of(9, 0)
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createExpense_categoryDoesNotExist_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        categoryDatabaseSystem.deleteCategory(category)
        val result = addExpense(category)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllExpenses_returnsCreatedExpense() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        addExpense(category)
        val result = expenseDatabaseSystem.retrieveAllExpenses(user)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.values?.size)
    }

    @Test
    fun retrieveAllExpenses_onlyReturnsTheSignedInUsersExpenses() = runBlocking {
        val userA = createTestUser("usera")
        addExpense(createTestCategory(userA))
        val userB = createTestUser("userb")
        addExpense(createTestCategory(userB), description = "Bread")
        val result = expenseDatabaseSystem.retrieveAllExpenses(userB)
        assertEquals(listOf("Bread"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllExpensesOnDate_onlyReturnsThatDate() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        addExpense(category, "On the day", date = LocalDate.of(2026, 1, 5))
        addExpense(category, "Day before", date = LocalDate.of(2026, 1, 4))
        val result = expenseDatabaseSystem.retrieveAllExpensesOnDate(user, LocalDate.of(2026, 1, 5))
        assertEquals(listOf("On the day"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllExpensesBetweenDates_includesBothEndDates() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        addExpense(category, "Before", date = LocalDate.of(2026, 1, 9))
        addExpense(category, "Start", date = LocalDate.of(2026, 1, 10))
        addExpense(category, "Middle", date = LocalDate.of(2026, 1, 15))
        addExpense(category, "End", date = LocalDate.of(2026, 1, 20))
        addExpense(category, "After", date = LocalDate.of(2026, 1, 21))
        val result = expenseDatabaseSystem.retrieveAllExpensesBetweenDates(user, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20))
        assertEquals(setOf("Start", "Middle", "End"), result.values?.map { it.description }?.toSet())
    }

    @Test
    fun retrieveAllExpensesBetweenDates_startAfterEnd_fails() = runBlocking {
        val user = createTestUser()
        val result = expenseDatabaseSystem.retrieveAllExpensesBetweenDates(user, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1))
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllExpensesForCategory_onlyReturnsThatCategory() = runBlocking {
        val user = createTestUser()
        val food = createTestCategory(user, "Food")
        val fuel = createTestCategory(user, "Fuel")
        addExpense(food, "Milk")
        addExpense(fuel, "Petrol")
        val result = expenseDatabaseSystem.retrieveAllExpensesForCategory(food)
        assertEquals(listOf("Milk"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllExpensesOnDateForCategory_filtersByBoth() = runBlocking {
        val user = createTestUser()
        val food = createTestCategory(user, "Food")
        val fuel = createTestCategory(user, "Fuel")
        val day = LocalDate.of(2026, 1, 5)
        addExpense(food, "Milk", date = day)
        addExpense(food, "Old milk", date = day.minusDays(1))
        addExpense(fuel, "Petrol", date = day)
        val result = expenseDatabaseSystem.retrieveAllExpensesOnDateForCategory(food, day)
        assertEquals(listOf("Milk"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllExpensesBetweenDatesForCategory_filtersByBoth() = runBlocking {
        val user = createTestUser()
        val food = createTestCategory(user, "Food")
        val fuel = createTestCategory(user, "Fuel")
        addExpense(food, "In range", date = LocalDate.of(2026, 1, 15))
        addExpense(food, "Out of range", date = LocalDate.of(2026, 3, 1))
        addExpense(fuel, "Other category", date = LocalDate.of(2026, 1, 15))
        val result = expenseDatabaseSystem.retrieveAllExpensesBetweenDatesForCategory(food, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        assertEquals(listOf("In range"), result.values?.map { it.description })
    }

    @Test
    fun updateExpenseDescription_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseDescription(expense, "Bread")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("Bread", expenseDatabaseSystem.retrieveAllExpenses(user).values?.single()?.description)
    }

    @Test
    fun updateExpenseAmount_succeeds_andNegativeFails() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseAmount(expense, 99.0)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(99.0, result.value?.amount)
        assertEquals(UpdateReturnStatus.Failed, expenseDatabaseSystem.updateExpenseAmount(expense, -1.0).status)
    }

    @Test
    fun updateExpenseAmount_sameAmount_noChange() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseAmount(expense, expense.amount)
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateExpenseDate_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseDate(expense, LocalDate.of(2026, 6, 6))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(LocalDate.of(2026, 6, 6), expenseDatabaseSystem.retrieveAllExpenses(user).values?.single()?.dateAsLocalDate())
    }

    @Test
    fun updateExpenseStartTime_afterEndTime_fails() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseStartTime(expense, LocalTime.of(10, 0))
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateExpenseEndTime_beforeStartTime_fails() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseEndTime(expense, LocalTime.of(8, 0))
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateExpenseEndTime_validTime_succeeds() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        val result = expenseDatabaseSystem.updateExpenseEndTime(expense, LocalTime.of(10, 0))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
    }

    @Test
    fun deleteExpense_removesIt() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        assertEquals(DeleteReturnStatus.Deleted, expenseDatabaseSystem.deleteExpense(expense))
        assertTrue(expenseDatabaseSystem.retrieveAllExpenses(user).values!!.isEmpty())
    }

    @Test
    fun deleteExpense_alreadyDeleted_doesNotExist() = runBlocking {
        val user = createTestUser()
        val expense = addExpense(createTestCategory(user)).value!!
        expenseDatabaseSystem.deleteExpense(expense)
        assertEquals(DeleteReturnStatus.DoesNotExist, expenseDatabaseSystem.deleteExpense(expense))
    }
}
