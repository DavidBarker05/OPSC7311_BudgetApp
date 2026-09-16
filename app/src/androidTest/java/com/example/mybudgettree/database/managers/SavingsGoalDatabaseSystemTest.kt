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
class SavingsGoalDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createGoal_success() = runBlocking {
        val user = createTestUser()
        val result = savingsGoalDatabaseSystem.createGoal(user, "Wedding", "wedding", 5000.0)
        assertTrue(result.wasSuccessful)
        assertEquals("Wedding", result.goal?.goalName)
        assertEquals("wedding", result.goal?.iconKey)
        assertEquals(5000.0, result.goal?.targetAmount)
    }

    @Test
    fun createGoal_hasNoTargetByDefault() = runBlocking {
        val user = createTestUser()
        val result = savingsGoalDatabaseSystem.createGoal(user, "Wedding")
        assertNull(result.goal?.targetAmount)
        assertNull(result.goal?.iconKey)
    }

    @Test
    fun createGoal_negativeTarget_fails() = runBlocking {
        val user = createTestUser()
        val result = savingsGoalDatabaseSystem.createGoal(user, "Wedding", targetAmount = -100.0)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createGoal_blankName_fails() = runBlocking {
        val user = createTestUser()
        val result = savingsGoalDatabaseSystem.createGoal(user, "")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createGoal_duplicateNameForSameUser_fails() = runBlocking {
        val user = createTestUser()
        savingsGoalDatabaseSystem.createGoal(user, "Wedding")
        val result = savingsGoalDatabaseSystem.createGoal(user, "Wedding")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createGoal_sameNameDifferentUser_succeeds() = runBlocking {
        val userA = createTestUser("usera")
        val userB = createTestUser("userb")
        savingsGoalDatabaseSystem.createGoal(userA, "Wedding")
        val result = savingsGoalDatabaseSystem.createGoal(userB, "Wedding")
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun getAllGoalsForUser_returnsCreatedGoal() = runBlocking {
        val user = createTestUser()
        createTestGoal(user)
        val result = savingsGoalDatabaseSystem.getAllGoalsForUser(user)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.goals?.size)
    }

    @Test
    fun updateGoalName_toUnusedName_succeeds() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsGoalDatabaseSystem.updateGoalName(goal, "Car")
        assertEquals(SavingsGoalDatabaseSystem.UpdateGoalReturnStatus.Succeeded, result.status)
        assertEquals("Car", result.goal?.goalName)
    }

    @Test
    fun updateGoalName_toExistingName_fails() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user, "Wedding")
        createTestGoal(user, "Car")
        val result = savingsGoalDatabaseSystem.updateGoalName(goal, "Car")
        assertEquals(SavingsGoalDatabaseSystem.UpdateGoalReturnStatus.Failed, result.status)
    }

    @Test
    fun updateGoalIcon_setsKey_succeeds() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsGoalDatabaseSystem.updateGoalIcon(goal, "car")
        assertEquals(SavingsGoalDatabaseSystem.UpdateGoalReturnStatus.Succeeded, result.status)
        assertEquals("car", result.goal?.iconKey)
    }

    @Test
    fun updateGoalTarget_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsGoalDatabaseSystem.updateGoalTarget(goal, -50.0)
        assertEquals(SavingsGoalDatabaseSystem.UpdateGoalReturnStatus.Failed, result.status)
    }

    @Test
    fun updateGoalTarget_toNull_clearsTarget() = runBlocking {
        val user = createTestUser()
        val goal = savingsGoalDatabaseSystem.createGoal(user, "Wedding", targetAmount = 5000.0).goal!!
        val result = savingsGoalDatabaseSystem.updateGoalTarget(goal, null)
        assertEquals(SavingsGoalDatabaseSystem.UpdateGoalReturnStatus.Succeeded, result.status)
        assertNull(result.goal?.targetAmount)
    }

    @Test
    fun deleteGoal_success() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val status = savingsGoalDatabaseSystem.deleteGoal(goal)
        assertEquals(SavingsGoalDatabaseSystem.GoalDeleteReturnStatus.Deleted, status)
    }

    @Test
    fun deleteGoal_alreadyDeleted_doesNotExist() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        savingsGoalDatabaseSystem.deleteGoal(goal)
        val status = savingsGoalDatabaseSystem.deleteGoal(goal)
        assertEquals(SavingsGoalDatabaseSystem.GoalDeleteReturnStatus.DoesNotExist, status)
    }
}
