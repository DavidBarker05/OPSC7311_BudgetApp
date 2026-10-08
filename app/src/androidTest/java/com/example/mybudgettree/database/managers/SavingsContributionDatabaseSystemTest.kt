package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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
        assertEquals(250.0, result.value?.amount)
        assertEquals(goal.id, result.value?.goalId)
        assertTrue(result.value!!.id.isNotBlank())
    }

    @Test
    fun createContribution_stampsCreatedAt() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val result = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15))
        assertTrue(result.wasSuccessful)
        assertTrue(result.value!!.createdAt.isNotBlank())
        // Reading it back as a date-time proves it was stored in a parseable format
        result.value!!.createdAtAsLocalDateTime()
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
        assertEquals(1, result.values?.size)
    }

    @Test
    fun retrieveAllContributionsForGoal_onlyReturnsThatGoalsContributions() = runBlocking {
        val user = createTestUser()
        val wedding = createTestGoal(user, "Wedding")
        val car = createTestGoal(user, "Car")
        savingsContributionDatabaseSystem.createContribution(wedding, 100.0, LocalDate.of(2026, 1, 15))
        savingsContributionDatabaseSystem.createContribution(car, 200.0, LocalDate.of(2026, 1, 15))
        val result = savingsContributionDatabaseSystem.retrieveAllContributionsForGoal(car)
        assertEquals(listOf(200.0), result.values?.map { it.amount })
    }

    @Test
    fun deleteContribution_success() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val contribution = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15)).value!!
        val status = savingsContributionDatabaseSystem.deleteContribution(contribution)
        assertEquals(DeleteReturnStatus.Deleted, status)
        assertTrue(savingsContributionDatabaseSystem.retrieveAllContributionsForGoal(goal).values!!.isEmpty())
    }

    @Test
    fun deleteContribution_alreadyDeleted_doesNotExist() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val contribution = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.of(2026, 1, 15)).value!!
        savingsContributionDatabaseSystem.deleteContribution(contribution)
        assertEquals(DeleteReturnStatus.DoesNotExist, savingsContributionDatabaseSystem.deleteContribution(contribution))
    }

    @Test
    fun deleteGoal_cascadesContributions_butNotOtherGoalsContributions() = runBlocking {
        val user = createTestUser()
        val doomed = createTestGoal(user, "Doomed")
        val kept = createTestGoal(user, "Kept")
        savingsContributionDatabaseSystem.createContribution(doomed, 250.0, LocalDate.of(2026, 1, 15))
        savingsContributionDatabaseSystem.createContribution(kept, 100.0, LocalDate.of(2026, 1, 15))

        savingsGoalDatabaseSystem.deleteGoal(doomed)

        // The goal is gone, so ask Firestore directly for what is left in the contributions collection
        val remaining = firestore.collection("users").document(user.uid).collection("savingsContributions")
            .get().await().documents.map { it.getString("goalId") }
        assertEquals(listOf(kept.id), remaining)
    }
}
