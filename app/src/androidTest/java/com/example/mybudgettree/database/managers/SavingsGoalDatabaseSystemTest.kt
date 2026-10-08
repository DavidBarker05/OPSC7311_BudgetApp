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

@RunWith(AndroidJUnit4::class)
class SavingsGoalDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createGoal_success() = runBlocking {
        val user = createTestUser()
        val result = savingsGoalDatabaseSystem.createGoal(user, "Wedding", "wedding", 5000.0)
        assertTrue(result.wasSuccessful)
        assertEquals("Wedding", result.value?.goalName)
        assertEquals("wedding", result.value?.iconKey)
        assertEquals(5000.0, result.value?.targetAmount)
        assertTrue(result.value!!.id.isNotBlank())
    }

    @Test
    fun createGoal_hasNoTargetByDefault() = runBlocking {
        val user = createTestUser()
        val result = savingsGoalDatabaseSystem.createGoal(user, "Wedding")
        assertNull(result.value?.targetAmount)
        assertNull(result.value?.iconKey)
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
        savingsGoalDatabaseSystem.createGoal(userA, "Wedding")
        val userB = createTestUser("userb")
        val result = savingsGoalDatabaseSystem.createGoal(userB, "Wedding")
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun isGoalNameTaken_reflectsExistingGoals() = runBlocking {
        val user = createTestUser()
        createTestGoal(user, "Wedding")
        assertTrue(savingsGoalDatabaseSystem.isGoalNameTaken(user, "Wedding"))
        assertFalse(savingsGoalDatabaseSystem.isGoalNameTaken(user, "Car"))
    }

    @Test
    fun findGoal_byId_andByName() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user, "Wedding")
        assertEquals(goal, savingsGoalDatabaseSystem.findGoal(goal.id).value)
        assertEquals(goal.id, savingsGoalDatabaseSystem.findGoal(user, "Wedding").value?.id)
        assertFalse(savingsGoalDatabaseSystem.findGoal("doesNotExist").wasSuccessful)
        assertFalse(savingsGoalDatabaseSystem.findGoal(user, "Nothing").wasSuccessful)
    }

    @Test
    fun getAllGoalsForUser_returnsCreatedGoal() = runBlocking {
        val user = createTestUser()
        createTestGoal(user)
        val result = savingsGoalDatabaseSystem.getAllGoalsForUser(user)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.values?.size)
    }

    @Test
    fun getAllGoalsForUser_onlyReturnsThatUsersGoals() = runBlocking {
        val userA = createTestUser("usera")
        createTestGoal(userA, "Wedding")
        val userB = createTestUser("userb")
        createTestGoal(userB, "Car")
        val result = savingsGoalDatabaseSystem.getAllGoalsForUser(userB)
        assertEquals(listOf("Car"), result.values?.map { it.goalName })
    }

    @Test
    fun updateGoalName_toUnusedName_succeeds() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsGoalDatabaseSystem.updateGoalName(goal, "Car")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("Car", result.value?.goalName)
        assertEquals("Car", savingsGoalDatabaseSystem.findGoal(goal.id).value?.goalName)
    }

    @Test
    fun updateGoalName_toExistingName_fails() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user, "Wedding")
        createTestGoal(user, "Car")
        val result = savingsGoalDatabaseSystem.updateGoalName(goal, "Car")
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateGoalIcon_setsKey_succeeds() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsGoalDatabaseSystem.updateGoalIcon(goal, "car")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("car", result.value?.iconKey)
    }

    @Test
    fun updateGoalTarget_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsGoalDatabaseSystem.updateGoalTarget(goal, -50.0)
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateGoalTarget_toNull_clearsTarget() = runBlocking {
        val user = createTestUser()
        val goal = savingsGoalDatabaseSystem.createGoal(user, "Wedding", targetAmount = 5000.0).value!!
        val result = savingsGoalDatabaseSystem.updateGoalTarget(goal, null)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertNull(result.value?.targetAmount)
        assertNull(savingsGoalDatabaseSystem.findGoal(goal.id).value?.targetAmount)
    }

    @Test
    fun deleteGoal_success() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val status = savingsGoalDatabaseSystem.deleteGoal(goal)
        assertEquals(DeleteReturnStatus.Deleted, status)
        assertFalse(savingsGoalDatabaseSystem.findGoal(goal.id).wasSuccessful)
    }

    @Test
    fun deleteGoal_alreadyDeleted_doesNotExist() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        savingsGoalDatabaseSystem.deleteGoal(goal)
        val status = savingsGoalDatabaseSystem.deleteGoal(goal)
        assertEquals(DeleteReturnStatus.DoesNotExist, status)
    }
}
