package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class UserTreeDatabaseSystemTest : DatabaseTestBase() {

    // createUser already makes a tree, so tests of creating one start by removing it
    private suspend fun deleteTree(user: User) {
        firestore.collection("users").document(user.uid).collection("userTree").document(UserTree.DOCUMENT_ID).delete().await()
    }

    @Test
    fun newUser_alreadyHasATreeAtLevelOneForThisMonth() = runBlocking {
        val user = createTestUser()
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertEquals(1, tree?.treeLevel)
        assertEquals(YearMonth.now(), tree?.yearMonthAsYearMonth())
    }

    @Test
    fun createUserTree_afterTheTreeWasRemoved_startsAtLevelOne() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2026, 1))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertEquals(1, tree?.treeLevel)
    }

    @Test
    fun createUserTree_storesGivenPeriod() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2026, 3))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertEquals(YearMonth.of(2026, 3), tree?.yearMonthAsYearMonth())
    }

    @Test
    fun createUserTree_hasNoLastWateringTimeByDefault() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2026, 1))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertNull(tree?.lastWateringTime)
        assertNull(tree?.lastWateringTimeAsNullableLocalDateTime())
    }

    @Test
    fun createUserTree_whenOneAlreadyExists_doesNotOverwriteIt() = runBlocking {
        val user = createTestUser()
        userTreeDatabaseSystem.createUserTree(user, YearMonth.of(2020, 1))
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertEquals(YearMonth.now(), tree?.yearMonthAsYearMonth())
    }

    @Test
    fun findUserTree_notCreated_returnsNull() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        val tree = userTreeDatabaseSystem.findUserTree(user)
        assertNull(tree)
    }
}
