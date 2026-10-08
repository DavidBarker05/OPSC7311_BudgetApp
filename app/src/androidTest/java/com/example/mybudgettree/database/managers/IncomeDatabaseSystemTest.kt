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
class IncomeDatabaseSystemTest : DatabaseTestBase() {

    private suspend fun addIncome(
        category: com.example.mybudgettree.database.entries.Category,
        description: String = "Salary",
        amount: Double = 25.50,
        date: LocalDate = LocalDate.of(2026, 1, 1)
    ) = incomeDatabaseSystem.createIncome(
        category = category,
        description = description,
        amount = amount,
        date = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(9, 5)
    )

    @Test
    fun createIncome_success() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addIncome(category)
        assertTrue(result.wasSuccessful)
        assertEquals("Salary", result.value?.description)
        assertEquals(category.id, result.value?.categoryId)
        assertTrue(result.value!!.id.isNotBlank())
    }

    @Test
    fun createIncome_hasNoImageByDefault() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addIncome(category)
        assertTrue(result.value!!.imagePaths.isEmpty())
        assertFalse(result.value!!.hasImage())
    }

    @Test
    fun createIncome_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addIncome(category, amount = -25.50)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createIncome_blankDescription_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = addIncome(category, description = " ")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createIncome_endTimeBeforeStartTime_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val result = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Salary",
            amount = 25.50,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 5),
            endTime = LocalTime.of(9, 0)
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createIncome_categoryDoesNotExist_fails() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        categoryDatabaseSystem.deleteCategory(category)
        val result = addIncome(category)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllIncomes_returnsCreatedIncome() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        addIncome(category)
        val result = incomeDatabaseSystem.retrieveAllIncomes(user)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.values?.size)
    }

    @Test
    fun retrieveAllIncomes_onlyReturnsTheSignedInUsersIncomes() = runBlocking {
        val userA = createTestUser("usera")
        addIncome(createTestCategory(userA))
        val userB = createTestUser("userb")
        addIncome(createTestCategory(userB), description = "Bonus")
        val result = incomeDatabaseSystem.retrieveAllIncomes(userB)
        assertEquals(listOf("Bonus"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllIncomesOnDate_onlyReturnsThatDate() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        addIncome(category, "On the day", date = LocalDate.of(2026, 1, 5))
        addIncome(category, "Day before", date = LocalDate.of(2026, 1, 4))
        val result = incomeDatabaseSystem.retrieveAllIncomesOnDate(user, LocalDate.of(2026, 1, 5))
        assertEquals(listOf("On the day"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllIncomesBetweenDates_includesBothEndDates() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        addIncome(category, "Before", date = LocalDate.of(2026, 1, 9))
        addIncome(category, "Start", date = LocalDate.of(2026, 1, 10))
        addIncome(category, "Middle", date = LocalDate.of(2026, 1, 15))
        addIncome(category, "End", date = LocalDate.of(2026, 1, 20))
        addIncome(category, "After", date = LocalDate.of(2026, 1, 21))
        val result = incomeDatabaseSystem.retrieveAllIncomesBetweenDates(user, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20))
        assertEquals(setOf("Start", "Middle", "End"), result.values?.map { it.description }?.toSet())
    }

    @Test
    fun retrieveAllIncomesBetweenDates_startAfterEnd_fails() = runBlocking {
        val user = createTestUser()
        val result = incomeDatabaseSystem.retrieveAllIncomesBetweenDates(user, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1))
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllIncomesForCategory_onlyReturnsThatCategory() = runBlocking {
        val user = createTestUser()
        val food = createTestCategory(user, "Food")
        val fuel = createTestCategory(user, "Fuel")
        addIncome(food, "Salary")
        addIncome(fuel, "Petrol")
        val result = incomeDatabaseSystem.retrieveAllIncomesForCategory(food)
        assertEquals(listOf("Salary"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllIncomesOnDateForCategory_filtersByBoth() = runBlocking {
        val user = createTestUser()
        val food = createTestCategory(user, "Food")
        val fuel = createTestCategory(user, "Fuel")
        val day = LocalDate.of(2026, 1, 5)
        addIncome(food, "Salary", date = day)
        addIncome(food, "Old milk", date = day.minusDays(1))
        addIncome(fuel, "Petrol", date = day)
        val result = incomeDatabaseSystem.retrieveAllIncomesOnDateForCategory(food, day)
        assertEquals(listOf("Salary"), result.values?.map { it.description })
    }

    @Test
    fun retrieveAllIncomesBetweenDatesForCategory_filtersByBoth() = runBlocking {
        val user = createTestUser()
        val food = createTestCategory(user, "Food")
        val fuel = createTestCategory(user, "Fuel")
        addIncome(food, "In range", date = LocalDate.of(2026, 1, 15))
        addIncome(food, "Out of range", date = LocalDate.of(2026, 3, 1))
        addIncome(fuel, "Other category", date = LocalDate.of(2026, 1, 15))
        val result = incomeDatabaseSystem.retrieveAllIncomesBetweenDatesForCategory(food, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        assertEquals(listOf("In range"), result.values?.map { it.description })
    }

    @Test
    fun updateIncomeDescription_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeDescription(income, "Bonus")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("Bonus", incomeDatabaseSystem.retrieveAllIncomes(user).values?.single()?.description)
    }

    @Test
    fun updateIncomeAmount_succeeds_andNegativeFails() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeAmount(income, 99.0)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(99.0, result.value?.amount)
        assertEquals(UpdateReturnStatus.Failed, incomeDatabaseSystem.updateIncomeAmount(income, -1.0).status)
    }

    @Test
    fun updateIncomeAmount_sameAmount_noChange() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeAmount(income, income.amount)
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateIncomeDate_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeDate(income, LocalDate.of(2026, 6, 6))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(LocalDate.of(2026, 6, 6), incomeDatabaseSystem.retrieveAllIncomes(user).values?.single()?.dateAsLocalDate())
    }

    @Test
    fun updateIncomeStartTime_afterEndTime_fails() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeStartTime(income, LocalTime.of(10, 0))
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateIncomeEndTime_beforeStartTime_fails() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeEndTime(income, LocalTime.of(8, 0))
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateIncomeEndTime_validTime_succeeds() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        val result = incomeDatabaseSystem.updateIncomeEndTime(income, LocalTime.of(10, 0))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
    }

    @Test
    fun deleteIncome_removesIt() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        assertEquals(DeleteReturnStatus.Deleted, incomeDatabaseSystem.deleteIncome(income))
        assertTrue(incomeDatabaseSystem.retrieveAllIncomes(user).values!!.isEmpty())
    }

    @Test
    fun deleteIncome_alreadyDeleted_doesNotExist() = runBlocking {
        val user = createTestUser()
        val income = addIncome(createTestCategory(user)).value!!
        incomeDatabaseSystem.deleteIncome(income)
        assertEquals(DeleteReturnStatus.DoesNotExist, incomeDatabaseSystem.deleteIncome(income))
    }
}
