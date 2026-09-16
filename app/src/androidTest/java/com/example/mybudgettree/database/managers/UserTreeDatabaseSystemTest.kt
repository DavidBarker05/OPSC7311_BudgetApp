package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class UserTreeDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createUserTree_startsAtLevelOne() = runBlocking {
        val user = createTestUser()
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2026, 1))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertEquals(1, tree?.treeLevel)
    }

    @Test
    fun createUserTree_storesGivenPeriod() = runBlocking {
        val user = createTestUser()
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2026, 3))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertEquals(YearMonth.of(2026, 3), tree?.yearMonth)
    }

    @Test
    fun createUserTree_hasNoLastWateringTimeByDefault() = runBlocking {
        val user = createTestUser()
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2026, 1))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertNull(tree?.lastWateringTime)
    }

    @Test
    fun findUserTree_notCreated_returnsNull() = runBlocking {
        val user = createTestUser()
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertNull(tree)
    }
}
