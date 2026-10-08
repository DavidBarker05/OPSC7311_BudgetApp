package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * This system manages savings contribution creation, discovery, and removal
 *
 * Contributions are stored in Firestore at `users/{uid}/savingsContributions/{id}` and point at their goal by `goalId`.
 * Only the signed-in user's data can be reached. Contributions can't be edited, only added and deleted
 *
 * @property auth The Firebase Authentication instance used to find the signed-in user
 * @property db The Firestore instance that holds the contributions
 */
class SavingsContributionDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    companion object {
        private const val TAG = "SavingsContributionDatabaseSystem"
    }

    private fun contributions(uid: String): CollectionReference = db.collection("users").document(uid).collection("savingsContributions")

    /**
     * Records a new deposit against the goal
     *
     * @param goal The [SavingsGoal] the deposit applies to
     * @param amount The amount deposited, cannot be negative
     * @param date The date the deposit was made
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createContribution(goal: SavingsGoal, amount: Double, date: LocalDate): CreateReturnInfo<SavingsContribution> {
        val result = tryCreateContribution(goal, amount, date)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "added",
            verbOnFailure = "to add",
            messageDetails = "contribution to goal '${goal.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateContribution(goal: SavingsGoal, amount: Double, date: LocalDate): CreateReturnInfo<SavingsContribution> {
        if (goal.id.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Goal id is empty")
        if (amount < 0.0) return CreateReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        val uid = auth.uid ?: return CreateReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (!db .collection("users")
                .document(uid)
                .collection("savingsGoals")
                .document(goal.id)
                .get()
                .await()
                .exists())
            return CreateReturnInfo(wasSuccessful = false, errMsg = "Goal does not exist in the database")
        return try {
            val contributions = contributions(uid)
            val contribution = SavingsContribution(
                goalId = goal.id,
                amount = amount,
                date = date.toString(),
                createdAt = LocalDateTime.now().toString()
            )
            val ref = contributions.add(contribution).await()
            CreateReturnInfo(wasSuccessful = true, value = contribution.copy(id = ref.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create contribution")
        }
    }

    /**
     * Retrieves every contribution made against the goal
     *
     * @param goal The [SavingsGoal] to retrieve contributions for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllContributionsForGoal(goal: SavingsGoal): FindAllReturnInfo<SavingsContribution> {
        if (goal.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Goal id is empty")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (!db .collection("users")
                .document(uid)
                .collection("savingsGoals")
                .document(goal.id)
                .get()
                .await()
                .exists())
            return FindAllReturnInfo(wasSuccessful = false, errMsg = "Goal does not exist in the database")
        return try {
            val allContributions = contributions(uid)
                .whereEqualTo("goalId", goal.id)
                .get()
                .await()
                .toObjects<SavingsContribution>()
            FindAllReturnInfo(wasSuccessful = true, values = allContributions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load contributions")
        }
    }

    /**
     * Deletes the contribution from the database
     *
     * @param contribution The [SavingsContribution] to delete
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteContribution(contribution: SavingsContribution): DeleteReturnStatus {
        val uid = auth.uid
        val result =
            if (uid == null) DeleteReturnStatus.ReauthenticationFailed
            else deleteDocument(
                db = db,
                collection = contributions(uid),
                id = contribution.id,
                subcollections = emptyList(),
                relatedCollections = emptyList(),
                batchSize = 0L
            )
        val wasSuccessful = result == DeleteReturnStatus.Deleted
        val errMsg: String? =
            if (wasSuccessful) null
            else when (result) {
                DeleteReturnStatus.DoesNotExist -> "Contribution does not exist"
                DeleteReturnStatus.ReauthenticationFailed -> "No user currently signed in"
                else -> "Unknown reason"
            }
        logOutcome(
            tag = TAG,
            wasSuccessful = wasSuccessful,
            verbOnSuccess = "deleted",
            verbOnFailure = "to delete",
            messageDetails = "contribution '${contribution.id}'",
            errMsg = errMsg
        )
        return result
    }
}
