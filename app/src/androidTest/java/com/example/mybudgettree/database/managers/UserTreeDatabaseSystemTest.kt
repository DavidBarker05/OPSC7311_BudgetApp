package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class UserTreeDatabaseSystemTest : DatabaseTestBase() {

    // createUser already makes a tree, so tests that need the user to have none remove it first
    private suspend fun deleteTree(user: User) {
        firestore.collection("users").document(user.uid).collection("userTree").document(UserTree.DOCUMENT_ID).delete().await()
    }

    private suspend fun currentTree(user: User): UserTree = userTreeDatabaseSystem.findUserTree(user).value!!

    // ---- the tree every new user gets

    @Test
    fun newUser_alreadyHasATreeAtLevelOneForThisMonth() = runBlocking {
        val user = createTestUser()
        val tree = userTreeDatabaseSystem.findUserTree(user).value
        assertEquals(1, tree?.treeLevel)
        assertEquals(YearMonth.now(), tree?.yearMonthAsYearMonth())
    }

    // ---- createOrGetUserTree

    @Test
    fun createOrGetUserTree_afterTheTreeWasRemoved_createsItAtLevelOne() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        val result = userTreeDatabaseSystem.createOrGetUserTree(user, YearMonth.of(2026, 1))
        assertTrue(result.wasSuccessful)
        assertEquals(1, result.value?.treeLevel)
        assertEquals(1, currentTree(user).treeLevel)
    }

    @Test
    fun createOrGetUserTree_storesGivenPeriod() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        userTreeDatabaseSystem.createOrGetUserTree(user, YearMonth.of(2026, 3))
        assertEquals(YearMonth.of(2026, 3), currentTree(user).yearMonthAsYearMonth())
    }

    @Test
    fun createOrGetUserTree_hasNoLastWateringTimeByDefault() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        val result = userTreeDatabaseSystem.createOrGetUserTree(user, YearMonth.of(2026, 1))
        assertNull(result.value?.lastWateringTime)
        assertNull(result.value?.lastWateringTimeAsNullableLocalDateTime())
    }

    @Test
    fun createOrGetUserTree_whenOneAlreadyExists_returnsItWithoutOverwriting() = runBlocking {
        val user = createTestUser()
        userTreeDatabaseSystem.updateTreeLevel(currentTree(user), 5)
        val result = userTreeDatabaseSystem.createOrGetUserTree(user, YearMonth.of(2020, 1))
        assertTrue(result.wasSuccessful)
        assertEquals(5, result.value?.treeLevel)
        assertEquals(YearMonth.now(), result.value?.yearMonthAsYearMonth())
        assertEquals(5, currentTree(user).treeLevel)
    }

    @Test
    fun createOrGetUserTree_forUserWhoIsNotSignedIn_fails() = runBlocking {
        val first = createTestUser("first")
        createTestUser("second")
        val result = userTreeDatabaseSystem.createOrGetUserTree(first, YearMonth.of(2026, 1))
        assertFalse(result.wasSuccessful)
    }

    // ---- findUserTree

    @Test
    fun findUserTree_notCreated_fails() = runBlocking {
        val user = createTestUser()
        deleteTree(user)
        val result = userTreeDatabaseSystem.findUserTree(user)
        assertFalse(result.wasSuccessful)
        assertNull(result.value)
    }

    @Test
    fun findUserTree_forUserWhoIsNotSignedIn_fails() = runBlocking {
        val first = createTestUser("first")
        createTestUser("second")
        assertFalse(userTreeDatabaseSystem.findUserTree(first).wasSuccessful)
    }

    @Test
    fun findUserTree_isPerUser() = runBlocking {
        val first = createTestUser("first")
        userTreeDatabaseSystem.updateTreeLevel(currentTree(first), 7)
        val second = createTestUser("second")
        assertEquals(1, currentTree(second).treeLevel)
    }

    // ---- updateTreeLevel

    @Test
    fun updateTreeLevel_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val result = userTreeDatabaseSystem.updateTreeLevel(currentTree(user), 3)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(3, result.value?.treeLevel)
        assertEquals(3, currentTree(user).treeLevel)
    }

    @Test
    fun updateTreeLevel_sameLevel_noChange() = runBlocking {
        val user = createTestUser()
        val result = userTreeDatabaseSystem.updateTreeLevel(currentTree(user), 1)
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateTreeLevel_belowOne_fails_andLeavesTheLevelAlone() = runBlocking {
        val user = createTestUser()
        assertEquals(UpdateReturnStatus.Failed, userTreeDatabaseSystem.updateTreeLevel(currentTree(user), 0).status)
        assertEquals(UpdateReturnStatus.Failed, userTreeDatabaseSystem.updateTreeLevel(currentTree(user), -4).status)
        assertEquals(1, currentTree(user).treeLevel)
    }

    @Test
    fun updateTreeLevel_whenTheTreeNoLongerExists_fails() = runBlocking {
        val user = createTestUser()
        val tree = currentTree(user)
        deleteTree(user)
        assertEquals(UpdateReturnStatus.Failed, userTreeDatabaseSystem.updateTreeLevel(tree, 4).status)
    }

    @Test
    fun updateTreeLevel_whenNobodyIsSignedIn_fails() = runBlocking {
        val user = createTestUser()
        val tree = currentTree(user)
        userDatabaseSystem.logout()
        assertEquals(UpdateReturnStatus.Failed, userTreeDatabaseSystem.updateTreeLevel(tree, 4).status)
    }

    // ---- updateYearMonth

    @Test
    fun updateYearMonth_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val result = userTreeDatabaseSystem.updateYearMonth(currentTree(user), YearMonth.of(2030, 12))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(YearMonth.of(2030, 12), result.value?.yearMonthAsYearMonth())
        assertEquals(YearMonth.of(2030, 12), currentTree(user).yearMonthAsYearMonth())
    }

    @Test
    fun updateYearMonth_sameMonth_noChange() = runBlocking {
        val user = createTestUser()
        val result = userTreeDatabaseSystem.updateYearMonth(currentTree(user), YearMonth.now())
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateYearMonth_whenTheTreeNoLongerExists_fails() = runBlocking {
        val user = createTestUser()
        val tree = currentTree(user)
        deleteTree(user)
        assertEquals(UpdateReturnStatus.Failed, userTreeDatabaseSystem.updateYearMonth(tree, YearMonth.of(2030, 1)).status)
    }

    // ---- updateLastWateringTime

    @Test
    fun updateLastWateringTime_succeeds_andIsSaved() = runBlocking {
        val user = createTestUser()
        val time = LocalDateTime.of(2026, 10, 5, 14, 30, 15)
        val result = userTreeDatabaseSystem.updateLastWateringTime(currentTree(user), time)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(time, result.value?.lastWateringTimeAsNullableLocalDateTime())
        assertEquals(time, currentTree(user).lastWateringTimeAsNullableLocalDateTime())
    }

    @Test
    fun updateLastWateringTime_sameTime_noChange() = runBlocking {
        val user = createTestUser()
        val time = LocalDateTime.of(2026, 10, 5, 14, 30)
        val watered = userTreeDatabaseSystem.updateLastWateringTime(currentTree(user), time).value!!
        val result = userTreeDatabaseSystem.updateLastWateringTime(watered, time)
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateLastWateringTime_laterTime_replacesTheEarlierOne() = runBlocking {
        val user = createTestUser()
        val first = userTreeDatabaseSystem.updateLastWateringTime(currentTree(user), LocalDateTime.of(2026, 10, 5, 9, 0)).value!!
        val later = LocalDateTime.of(2026, 10, 6, 9, 0)
        userTreeDatabaseSystem.updateLastWateringTime(first, later)
        assertEquals(later, currentTree(user).lastWateringTimeAsNullableLocalDateTime())
    }

    @Test
    fun updateLastWateringTime_whenTheTreeNoLongerExists_fails() = runBlocking {
        val user = createTestUser()
        val tree = currentTree(user)
        deleteTree(user)
        val result = userTreeDatabaseSystem.updateLastWateringTime(tree, LocalDateTime.of(2026, 10, 5, 9, 0))
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updates_toOneFieldLeaveTheOthersAlone() = runBlocking {
        val user = createTestUser()
        val time = LocalDateTime.of(2026, 10, 5, 9, 0)
        val leveled = userTreeDatabaseSystem.updateTreeLevel(currentTree(user), 4).value!!
        userTreeDatabaseSystem.updateLastWateringTime(leveled, time)
        val tree = currentTree(user)
        assertEquals(4, tree.treeLevel)
        assertEquals(time, tree.lastWateringTimeAsNullableLocalDateTime())
        assertEquals(YearMonth.now(), tree.yearMonthAsYearMonth())
    }

    // ---- deleteUserTree

    @Test
    fun deleteUserTree_removesIt() = runBlocking {
        val user = createTestUser()
        val status = userTreeDatabaseSystem.deleteUserTree()
        assertEquals(DeleteReturnStatus.Deleted, status)
        assertFalse(userTreeDatabaseSystem.findUserTree(user).wasSuccessful)
    }

    @Test
    fun deleteUserTree_alreadyDeleted_doesNotExist() = runBlocking {
        createTestUser()
        userTreeDatabaseSystem.deleteUserTree()
        assertEquals(DeleteReturnStatus.DoesNotExist, userTreeDatabaseSystem.deleteUserTree())
    }

    @Test
    fun deleteUserTree_whenNobodyIsSignedIn_failsToReauthenticate() = runBlocking {
        createTestUser()
        userDatabaseSystem.logout()
        assertEquals(DeleteReturnStatus.ReauthenticationFailed, userTreeDatabaseSystem.deleteUserTree())
    }

    @Test
    fun deleteUserTree_thenCreateOrGet_startsAgainAtLevelOne() = runBlocking {
        val user = createTestUser()
        userTreeDatabaseSystem.updateTreeLevel(currentTree(user), 6)
        userTreeDatabaseSystem.deleteUserTree()
        val result = userTreeDatabaseSystem.createOrGetUserTree(user, YearMonth.of(2026, 1))
        assertNotNull(result.value)
        assertEquals(1, result.value?.treeLevel)
    }

    @Test
    fun deletingAUser_doesNotTouchAnotherUsersTree() = runBlocking {
        val doomed = createTestUser("doomed")
        userDatabaseSystem.deleteUser(doomed, "password123")
        val other = createTestUser("other")
        userTreeDatabaseSystem.updateTreeLevel(currentTree(other), 3)
        assertEquals(3, currentTree(other).treeLevel)
    }
}
