package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.daos.SavingsGoalDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.SavingsGoal

/**
 * This system manages savings goal state, uniqueness validation, discovery, updates, and removals
 *
 * @property savingsGoalDao The underlying Data Access Object managing RoomDB operations
 * @property userDatabaseSystem Used to validate that the owning user exists before goal operations proceed
 */
class SavingsGoalDatabaseSystem(
    private val savingsGoalDao: SavingsGoalDao,
    private val userDatabaseSystem: UserDatabaseSystem
) {

    companion object {
        private const val TAG = "SavingsGoalDatabaseSystem"

        private fun logUpdateOutcome(action: String, status: UpdateGoalReturnStatus, errMsg: String?) {
            when (status) {
                UpdateGoalReturnStatus.Succeeded -> Log.i(TAG, "Successfully $action")
                UpdateGoalReturnStatus.Failed -> Log.w(TAG, "Failed to $action: $errMsg")
                UpdateGoalReturnStatus.NoChange -> Log.d(TAG, "No change $action")
            }
        }

        private fun logCreateOutcome(action: String, wasSuccessful: Boolean, errMsg: String?) {
            if (wasSuccessful) Log.i(TAG, "Successfully $action") else Log.w(TAG, "Failed to $action: $errMsg")
        }
    }

    data class CreateGoalReturnInfo(
        val wasSuccessful: Boolean,
        val goal: SavingsGoal? = null,
        val errMsg: String? = null
    )

    data class FindGoalReturnInfo(
        val wasSuccessful: Boolean,
        val goal: SavingsGoal? = null,
        val errMsg: String? = null
    )

    data class FindAllGoalsReturnInfo(
        val wasSuccessful: Boolean,
        val goals: List<SavingsGoal>? = null,
        val errMsg: String? = null
    )

    enum class UpdateGoalReturnStatus { Failed, NoChange, Succeeded }

    data class UpdateGoalReturnInfo(
        val status: UpdateGoalReturnStatus,
        val goal: SavingsGoal? = null,
        val errMsg: String? = null
    )

    enum class GoalDeleteReturnStatus { DoesNotExist, Deleted }

    /**
     * Creates a new savings goal for the user after validating the name and confirming it isn't already in use
     *
     * @param user The [User] the goal belongs to
     * @param goalName The desired name for the goal, must be unique for the user
     * @param iconKey The [com.example.mybudgettree.IconCatalog] key for the goal's icon
     * @param targetAmount The amount the user is aiming to save, or null if no target is set
     * @return A [CreateGoalReturnInfo] indicating what happened with the creation
     */
    suspend fun createGoal(user: User, goalName: String, iconKey: String? = null, targetAmount: Double? = null): CreateGoalReturnInfo {
        val result = run {
            if (goalName.isBlank()) return@run CreateGoalReturnInfo(wasSuccessful = false, errMsg = "Goal name is empty")
            if (targetAmount != null && targetAmount < 0.0) return@run CreateGoalReturnInfo(wasSuccessful = false, errMsg = "Target amount cannot be negative")
            val userStatus = userDatabaseSystem.findUser(user.username)
            if (!userStatus.wasSuccessful) return@run CreateGoalReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
            val goal = SavingsGoal(username = user.username, goalName = goalName, iconKey = iconKey, targetAmount = targetAmount)
            val id = savingsGoalDao.insertGoal(goal)
            if (id == -1L) return@run CreateGoalReturnInfo(wasSuccessful = false, errMsg = "User already has a goal with name \"$goalName\"")
            CreateGoalReturnInfo(wasSuccessful = true, goal = goal.copy(id = id))
        }
        logCreateOutcome("create goal '$goalName' for user '${user.username}'", result.wasSuccessful, result.errMsg)
        return result
    }

    suspend fun findGoal(goalId: Long): FindGoalReturnInfo {
        val goal = savingsGoalDao.findGoal(goalId) ?: return FindGoalReturnInfo(wasSuccessful = false, errMsg = "Goal does not exist")
        return FindGoalReturnInfo(wasSuccessful = true, goal = goal)
    }

    suspend fun isGoalStillValid(goal: SavingsGoal): Boolean = findGoal(goal.id).wasSuccessful

    suspend fun getAllGoalsForUser(user: User): FindAllGoalsReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return FindAllGoalsReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return FindAllGoalsReturnInfo(wasSuccessful = true, goals = savingsGoalDao.retrieveAllGoals(user.username))
    }

    suspend fun updateGoalName(goal: SavingsGoal, newGoalName: String): UpdateGoalReturnInfo {
        val result = run {
            if (newGoalName.isBlank()) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Failed, errMsg = "Goal name is empty")
            if (goal.goalName == newGoalName) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.NoChange, goal = goal)
            if (!isGoalStillValid(goal)) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Failed, errMsg = "Goal does not exist")
            if (savingsGoalDao.findGoal(goal.username, newGoalName) != null) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Failed, errMsg = "Goal name already in use")
            savingsGoalDao.updateGoalName(goal.id, newGoalName)
            UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Succeeded, goal = goal.copy(goalName = newGoalName))
        }
        logUpdateOutcome("update name for goal '${goal.goalName}'", result.status, result.errMsg)
        return result
    }

    suspend fun updateGoalIcon(goal: SavingsGoal, newIconKey: String?): UpdateGoalReturnInfo {
        val result = run {
            if (goal.iconKey == newIconKey) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.NoChange, goal = goal)
            if (!isGoalStillValid(goal)) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Failed, errMsg = "Goal does not exist")
            savingsGoalDao.updateGoalIcon(goal.id, newIconKey)
            UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Succeeded, goal = goal.copy(iconKey = newIconKey))
        }
        logUpdateOutcome("update icon for goal '${goal.goalName}'", result.status, result.errMsg)
        return result
    }

    suspend fun updateGoalTarget(goal: SavingsGoal, newTargetAmount: Double?): UpdateGoalReturnInfo {
        val result = run {
            if (goal.targetAmount == newTargetAmount) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.NoChange, goal = goal)
            if (newTargetAmount != null && newTargetAmount < 0.0) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Failed, errMsg = "Target amount cannot be negative")
            if (!isGoalStillValid(goal)) return@run UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Failed, errMsg = "Goal does not exist")
            savingsGoalDao.updateGoalTarget(goal.id, newTargetAmount)
            UpdateGoalReturnInfo(status = UpdateGoalReturnStatus.Succeeded, goal = goal.copy(targetAmount = newTargetAmount))
        }
        logUpdateOutcome("update target for goal '${goal.goalName}'", result.status, result.errMsg)
        return result
    }

    suspend fun deleteGoal(goal: SavingsGoal): GoalDeleteReturnStatus {
        val status = if (savingsGoalDao.deleteGoal(goal) == 1) GoalDeleteReturnStatus.Deleted else GoalDeleteReturnStatus.DoesNotExist
        if (status == GoalDeleteReturnStatus.Deleted) Log.i(TAG, "Successfully deleted goal '${goal.goalName}'")
        else Log.w(TAG, "Failed to delete goal '${goal.goalName}': goal does not exist")
        return status
    }
}
