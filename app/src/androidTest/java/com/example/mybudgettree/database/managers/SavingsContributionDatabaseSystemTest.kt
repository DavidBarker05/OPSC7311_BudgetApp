package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class SavingsContributionDatabaseSystemTest : DatabaseTestBase() {

    // createContribution stamps createdAt with the current time, which a test cannot choose, so the tests of the
    // "after date time" query add contributions with a createdAt of their own straight into Firestore
    private suspend fun addContributionCreatedAt(goal: SavingsGoal, createdAt: LocalDateTime, amount: Double = 10.0) {
        firestore.collection("users").document(auth.uid!!).collection("savingsContributions").add(
            SavingsContribution(
                goalId = goal.id,
                amount = amount,
                date = createdAt.toLocalDate().toString(),
                createdAt = createdAt.toString()
            )
        ).await()
    }

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
        assertNotNull(result.value!!.createdAtAsLocalDateTime())
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

    // ---- retrieveAllContributionsAfterDateTime

    @Test
    fun retrieveAllContributionsAfterDateTime_returnsOnlyThoseCreatedAfterTheGivenTime() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 1, 1, 10, 0), amount = 1.0)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 3, 1, 10, 0), amount = 3.0)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 4, 1, 10, 0), amount = 4.0)

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2026, 2, 1, 0, 0))

        assertTrue(result.wasSuccessful)
        assertEquals(setOf(3.0, 4.0), result.values?.map { it.amount }?.toSet())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_includesOneCreatedAtExactlyTheGivenTime() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val moment = LocalDateTime.of(2026, 2, 1, 12, 30, 15)
        addContributionCreatedAt(goal, moment, amount = 5.0)

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(moment)

        assertEquals(listOf(5.0), result.values?.map { it.amount })
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_excludesOneCreatedASecondBeforeTheGivenTime() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val moment = LocalDateTime.of(2026, 2, 1, 12, 30, 15)
        addContributionCreatedAt(goal, moment.minusSeconds(1))

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(moment)

        assertTrue(result.wasSuccessful)
        assertTrue(result.values!!.isEmpty())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_nothingMatches_isAnEmptySuccess() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 1, 1, 10, 0))

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2027, 1, 1, 0, 0))

        assertTrue(result.wasSuccessful)
        assertTrue(result.values!!.isEmpty())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_userWithNoContributions_isAnEmptySuccess() = runBlocking {
        createTestUser()
        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2000, 1, 1, 0, 0))
        assertTrue(result.wasSuccessful)
        assertTrue(result.values!!.isEmpty())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_comparesTimesCorrectlyWhenTheSecondsAreLeftOut() = runBlocking {
        // LocalDateTime.toString() drops zero seconds ("10:00") but keeps others ("10:00:30"). The stored text is
        // compared as text, so check that mixing the two shapes still orders by the real time
        val user = createTestUser()
        val goal = createTestGoal(user)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 2, 1, 10, 0), amount = 1.0)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 2, 1, 10, 0, 30), amount = 2.0)
        addContributionCreatedAt(goal, LocalDateTime.of(2026, 2, 1, 10, 1), amount = 3.0)

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2026, 2, 1, 10, 0, 15))

        assertEquals(setOf(2.0, 3.0), result.values?.map { it.amount }?.toSet())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_includesContributionsFromEveryGoal() = runBlocking {
        val user = createTestUser()
        val wedding = createTestGoal(user, "Wedding")
        val car = createTestGoal(user, "Car")
        addContributionCreatedAt(wedding, LocalDateTime.of(2026, 3, 1, 10, 0), amount = 1.0)
        addContributionCreatedAt(car, LocalDateTime.of(2026, 3, 2, 10, 0), amount = 2.0)

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2026, 1, 1, 0, 0))

        assertEquals(setOf(wedding.id, car.id), result.values?.map { it.goalId }?.toSet())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_returnsTheFullContribution() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val createdAt = LocalDateTime.of(2026, 3, 1, 10, 0, 45)
        addContributionCreatedAt(goal, createdAt, amount = 42.5)

        val found = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2026, 1, 1, 0, 0)).values!!.single()

        assertTrue(found.id.isNotBlank())
        assertEquals(goal.id, found.goalId)
        assertEquals(42.5, found.amount, 0.0)
        assertEquals(createdAt, found.createdAtAsLocalDateTime())
        assertEquals(LocalDate.of(2026, 3, 1), found.dateAsLocalDate())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_findsAContributionMadeThroughTheSystem() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        val before = LocalDateTime.now().minusMinutes(1)
        val created = savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.now()).value!!

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(before)

        assertEquals(listOf(created.id), result.values?.map { it.id })
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_skipsAContributionMadeBeforeAFutureTime() = runBlocking {
        val user = createTestUser()
        val goal = createTestGoal(user)
        savingsContributionDatabaseSystem.createContribution(goal, 250.0, LocalDate.now())

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.now().plusDays(1))

        assertTrue(result.wasSuccessful)
        assertTrue(result.values!!.isEmpty())
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_onlyReturnsTheSignedInUsersContributions() = runBlocking {
        val first = createTestUser("first")
        addContributionCreatedAt(createTestGoal(first), LocalDateTime.of(2026, 3, 1, 10, 0), amount = 1.0)
        val second = createTestUser("second")
        addContributionCreatedAt(createTestGoal(second), LocalDateTime.of(2026, 3, 1, 10, 0), amount = 2.0)

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2026, 1, 1, 0, 0))

        assertEquals(listOf(2.0), result.values?.map { it.amount })
    }

    @Test
    fun retrieveAllContributionsAfterDateTime_whenNobodyIsSignedIn_fails() = runBlocking {
        val user = createTestUser()
        addContributionCreatedAt(createTestGoal(user), LocalDateTime.of(2026, 3, 1, 10, 0))
        userDatabaseSystem.logout()

        val result = savingsContributionDatabaseSystem.retrieveAllContributionsAfterDateTime(LocalDateTime.of(2026, 1, 1, 0, 0))

        assertFalse(result.wasSuccessful)
        assertNull(result.values)
    }
}
