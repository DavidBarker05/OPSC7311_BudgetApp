package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.daos.SavingsContributionDao
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.entries.SavingsContribution
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * This system manages savings contribution creation, discovery, and removal
 *
 * @property savingsContributionDao The underlying Data Access Object managing RoomDB operations
 * @property savingsGoalDatabaseSystem Used to validate that the owning goal exists before contribution operations proceed
 */
class SavingsContributionDatabaseSystem(
    private val savingsContributionDao: SavingsContributionDao,
    private val savingsGoalDatabaseSystem: SavingsGoalDatabaseSystem
) {

    companion object {
        private const val TAG = "SavingsContributionDatabaseSystem"
    }

    data class CreateContributionReturnInfo(
        val wasSuccessful: Boolean,
        val contribution: SavingsContribution? = null,
        val errMsg: String? = null
    )

    data class FindAllContributionsReturnInfo(
        val wasSuccessful: Boolean,
        val contributions: List<SavingsContribution>? = null,
        val errMsg: String? = null
    )

    enum class ContributionDeleteReturnStatus { DoesNotExist, Deleted }

    /**
     * Records a new deposit against the goal
     *
     * @param goal The [SavingsGoal] the deposit applies to
     * @param amount The amount deposited, cannot be negative
     * @param date The date the deposit was made
     * @return A [CreateContributionReturnInfo] indicating what happened with the creation
     */
    suspend fun createContribution(goal: SavingsGoal, amount: Double, date: LocalDate): CreateContributionReturnInfo {
        val result = run {
            if (amount < 0.0) return@run CreateContributionReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
            if (!savingsGoalDatabaseSystem.isGoalStillValid(goal)) return@run CreateContributionReturnInfo(wasSuccessful = false, errMsg = "Goal does not exist")
            val contribution = SavingsContribution(goalId = goal.id, amount = amount, date = date, createdAt = LocalDateTime.now())
            val id = savingsContributionDao.insertContribution(contribution)
            CreateContributionReturnInfo(wasSuccessful = true, contribution = contribution.copy(id = id))
        }
        if (result.wasSuccessful) Log.i(TAG, "Successfully added contribution to goal '${goal.goalName}'")
        else Log.w(TAG, "Failed to add contribution to goal '${goal.goalName}': ${result.errMsg}")
        return result
    }

    /**
     * Retrieves every contribution made against the goal
     *
     * @param goal The [SavingsGoal] to retrieve contributions for
     * @return A [FindAllContributionsReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllContributionsForGoal(goal: SavingsGoal): FindAllContributionsReturnInfo =
        FindAllContributionsReturnInfo(wasSuccessful = true, contributions = savingsContributionDao.retrieveAllContributionsForGoal(goal.id))

    /**
     * Deletes the contribution from the database
     *
     * @param contribution The [SavingsContribution] to delete
     * @return A status reflection from [ContributionDeleteReturnStatus]
     */
    suspend fun deleteContribution(contribution: SavingsContribution): ContributionDeleteReturnStatus {
        val status = if (savingsContributionDao.deleteContribution(contribution) == 1) ContributionDeleteReturnStatus.Deleted else ContributionDeleteReturnStatus.DoesNotExist
        if (status == ContributionDeleteReturnStatus.Deleted) Log.i(TAG, "Successfully deleted contribution ${contribution.id}")
        else Log.w(TAG, "Failed to delete contribution ${contribution.id}: does not exist")
        return status
    }
}
