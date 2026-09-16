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
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class MonthlyGoalDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun saveGoal_success() = runBlocking {
        val user = createTestUser()
        val result = monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), 1000.0, 5000.0)
        assertTrue(result.wasSuccessful)
        assertEquals(1000.0, result.goal?.minGoal)
        assertEquals(5000.0, result.goal?.maxGoal)
    }

    @Test
    fun saveGoal_maxLessThanMin_fails() = runBlocking {
        val user = createTestUser()
        val result = monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), 5000.0, 1000.0)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun saveGoal_negativeMin_fails() = runBlocking {
        val user = createTestUser()
        val result = monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), -100.0, 1000.0)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun getGoal_notSet_returnsNull() = runBlocking {
        val user = createTestUser()
        val goal = monthlyGoalDatabaseSystem.getGoal(user, YearMonth.of(2026, 1))
        assertNull(goal)
    }

    @Test
    fun getGoal_afterSave_returnsSavedGoal() = runBlocking {
        val user = createTestUser()
        monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), 1000.0, 5000.0)
        val goal = monthlyGoalDatabaseSystem.getGoal(user, YearMonth.of(2026, 1))
        assertEquals(1000.0, goal?.minGoal)
        assertEquals(5000.0, goal?.maxGoal)
    }

    @Test
    fun saveGoal_samePeriod_replacesPreviousGoal() = runBlocking {
        val user = createTestUser()
        monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), 1000.0, 5000.0)
        monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), 2000.0, 6000.0)
        val goal = monthlyGoalDatabaseSystem.getGoal(user, YearMonth.of(2026, 1))
        assertEquals(2000.0, goal?.minGoal)
        assertEquals(6000.0, goal?.maxGoal)
    }

    @Test
    fun saveGoal_differentPeriodsForSameUser_bothStored() = runBlocking {
        val user = createTestUser()
        monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 1), 1000.0, 5000.0)
        monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.of(2026, 2), 1500.0, 5500.0)
        val january = monthlyGoalDatabaseSystem.getGoal(user, YearMonth.of(2026, 1))
        val february = monthlyGoalDatabaseSystem.getGoal(user, YearMonth.of(2026, 2))
        assertEquals(5000.0, january?.maxGoal)
        assertEquals(5500.0, february?.maxGoal)
    }
}
