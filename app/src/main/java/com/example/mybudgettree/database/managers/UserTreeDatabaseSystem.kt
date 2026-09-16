package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.daos.UserTreeDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import java.time.YearMonth

/**
 * This system manages the money-tree gamification state for users. It is currently
 * insert-only: nothing reads or updates the tree state yet, this only exists so the
 * schema is in place ahead of the final PoE
 *
 * @property userTreeDao The underlying Data Access Object managing RoomDB operations
 */
class UserTreeDatabaseSystem(private val userTreeDao: UserTreeDao) {

    companion object {
        private const val TAG = "UserTreeDatabaseSystem"
    }

    /**
     * Creates the initial tree state for a newly created user
     *
     * @param user The [User] the tree state belongs to
     * @param yearMonth The year and month the starting tree level applies to
     */
    suspend fun createUserTree(user: User, yearMonth: YearMonth) {
        val id = userTreeDao.insertUserTree(UserTree(username = user.username, yearMonth = yearMonth))
        if (id == -1L) Log.w(TAG, "User '${user.username}' already has a tree state")
        else Log.i(TAG, "Created tree state for user '${user.username}'")
    }

    /**
     * Find the user's tree state if it exists
     *
     * @param user The [User] to search for
     * @return The [UserTree] record, or null if not found
     */
    suspend fun findUserTree(user: User): UserTree? = userTreeDao.findUserTree(user.username)
}
