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

@RunWith(AndroidJUnit4::class)
class SavingsContributionDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createContribution_success() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15))
        assertTrue(result.wasSuccessful)
        assertEquals(250.0, result.contribution?.amount)
        assertEquals(goal.id, result.contribution?.goalId)
    }

    @Test
    fun createContribution_stampsCreatedAt() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15))
        assertTrue(result.wasSuccessful)
        assertTrue(result.contribution != null)
    }

    @Test
    fun createContribution_negativeAmount_fails() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsContributionDatabaseSystem.createContribution(goal, -50.0, LocalDate.of(2026, 1, 15))
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createContribution_goalDoesNotExist_fails() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        savingsGoalDatabaseSystem.deleteGoal(goal)
        val result = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15))
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun retrieveAllContributionsForGoal_returnsCreatedContribution() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15))
        val result = savingsContributionDatabaseSystem.retrieveAllContributionsForGoal(goal)
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.contributions?.size)
    }

    @Test
    fun deleteContribution_success() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val contribution = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15)).contribution!!
        val status = savingsContributionDatabaseSystem.deleteContribution(contribution)
        assertEquals(SavingsContributionDatabaseSystem.ContributionDeleteReturnStatus.Deleted, status)
    }

    @Test
    fun deleteGoal_cascadesContributions() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15))
        savingsGoalDatabaseSystem.deleteGoal(goal)
        val result = savingsContributionDatabaseSystem.retrieveAllContributionsForGoal(goal)
        assertEquals(0, result.contributions?.size)
    }
}
