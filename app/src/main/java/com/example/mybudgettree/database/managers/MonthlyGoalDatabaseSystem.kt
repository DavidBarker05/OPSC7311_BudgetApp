package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.daos.MonthlyGoalDao
import com.example.mybudgettree.database.entries.MonthlyGoal
import com.example.mybudgettree.database.entries.User
import java.time.YearMonth

/**
 * This system manages the user's overall monthly minimum/maximum spending goal,
 * distinct from per-category budgets
 *
 * @property monthlyGoalDao The underlying Data Access Object managing RoomDB operations
 */
class MonthlyGoalDatabaseSystem(private val monthlyGoalDao: MonthlyGoalDao) {

    companion object {
        private const val TAG = "MonthlyGoalDatabaseSystem"
    }

    data class SaveGoalReturnInfo(
        val wasSuccessful: Boolean,
        val goal: MonthlyGoal? = null,
        val errMsg: String? = null
    )

    /**
     * Finds the user's monthly goal for the given period, if one has been set
     *
     * @param user The [User] to look up
     * @param period The year and month to look up
     * @return The [MonthlyGoal] record, or null if none is set
     */
    suspend fun getGoal(user: User, period: YearMonth): MonthlyGoal? = monthlyGoalDao.findGoal(user.username, period)

    /**
     * Creates or replaces the user's monthly goal for the given period
     *
     * @param user The [User] the goal belongs to
     * @param period The year and month this goal applies to
     * @param minGoal The minimum amount the user intends to spend this month, cannot be negative
     * @param maxGoal The maximum amount the user intends to spend this month, cannot be less than [minGoal]
     * @return A [SaveGoalReturnInfo] indicating what happened with the save
     */
    suspend fun saveGoal(user: User, period: YearMonth, minGoal: Double, maxGoal: Double): SaveGoalReturnInfo {
        val result = run {
            if (minGoal < 0.0) return@run SaveGoalReturnInfo(wasSuccessful = false, errMsg = "Minimum goal cannot be negative")
            if (maxGoal < minGoal) return@run SaveGoalReturnInfo(wasSuccessful = false, errMsg = "Maximum goal cannot be less than the minimum goal")
            val goal = MonthlyGoal(username = user.username, period = period, minGoal = minGoal, maxGoal = maxGoal)
            monthlyGoalDao.upsertGoal(goal)
            SaveGoalReturnInfo(wasSuccessful = true, goal = goal)
        }
        if (result.wasSuccessful) Log.i(TAG, "Saved monthly goal for user '${user.username}', period $period")
        else Log.w(TAG, "Failed to save monthly goal for user '${user.username}': ${result.errMsg}")
        return result
    }
}
