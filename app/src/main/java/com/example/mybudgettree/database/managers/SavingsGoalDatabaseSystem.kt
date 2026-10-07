package com.example.mybudgettree.database.managers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.CollectionReference
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.SavingsGoal
import com.google.firebase.firestore.toObjects

/**
 * This system manages savings goal state, uniqueness validation, discovery, updates, and removals
 *
 */
class SavingsGoalDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "SavingsGoalDatabaseSystem"

        private const val BATCH_SIZE = 400L

        private val SAVINGS_GOAL_RELATED_COLLECTIONS = listOf(
            RelatedCollection(collectionName = "savingsContributions", referenceField = "goalId"),
        )
    }

    private fun goals(uid: String): CollectionReference = db.collection("users").document(uid).collection("savingsGoals")

    /**
     * Creates a new savings goal for the user after validating the name and confirming it isn't already in use
     *
     * @param user The [User] the goal belongs to
     * @param goalName The desired name for the goal, must be unique for the user
     * @param iconKey The [com.example.mybudgettree.IconCatalog] key for the goal's icon
     * @param targetAmount The amount the user is aiming to save, or null if no target is set
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createGoal(user: User, goalName: String, iconKey: String? = null, targetAmount: Double? = null): CreateReturnInfo<SavingsGoal> {
        val result = tryCreateGoal(user, goalName, iconKey, targetAmount)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "created",
            verbOnFailure = "to create",
            messageDetails = "goal '$goalName' for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateGoal(user: User, goalName: String, iconKey: String? = null, targetAmount: Double? = null): CreateReturnInfo<SavingsGoal> {
        if (goalName.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Goal name is empty")
        if (auth.currentUser?.uid != user.uid) return CreateReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val goals = goals(user.uid)
            val nameInUse = !goals.whereEqualTo("goalName", goalName).limit(1).get().await().isEmpty
            if (nameInUse) return CreateReturnInfo(wasSuccessful = false, errMsg = "User already has a category with name \"$goalName\"")
            val goal = SavingsGoal(
                goalName = goalName,
                iconKey = iconKey,
                targetAmount = targetAmount
            )
            val ref = goals.add(goal).await()
            CreateReturnInfo(wasSuccessful = true, value = goal.copy(id = ref.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create category")
        }
    }

    suspend fun isGoalNameTaken(user: User, goalName: String): Boolean {
        if (goalName.isBlank() || auth.currentUser?.uid != user.uid) return false
        return !goals(user.uid)
            .whereEqualTo("goalName", goalName)
            .limit(1)
            .get().await()
            .isEmpty
    }

    suspend fun isGoalNameTaken(uid: String, goalName: String): Boolean {
        if (goalName.isBlank() || auth.currentUser?.uid != uid) return false
        return !goals(uid)
            .whereEqualTo("goalName", goalName)
            .limit(1)
            .get().await()
            .isEmpty
    }

    suspend fun findGoal(goalId: String): FindReturnInfo<SavingsGoal> {
        if (goalId.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Goal id is empty")
        val uid = auth.uid ?: return FindReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            val goal = goals(uid).document(goalId).get().await().toObject<SavingsGoal>()
            if (goal != null) FindReturnInfo(wasSuccessful = true, value = goal)
            else FindReturnInfo(wasSuccessful = false, errMsg = "Could not find goal '$goalId' for current auth user")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not find goal '$goalId' for current auth user")
        }
    }

    suspend fun findGoal(user: User, goalName: String): FindReturnInfo<SavingsGoal> {
        if (goalName.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Goal name is empty")
        if (auth.currentUser?.uid != user.uid) return FindReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val foundGoal = goals(user.uid)
                .whereEqualTo("goalName", goalName)
                .limit(1)
                .get().await()
                .documents.firstOrNull()
                ?.toObject<SavingsGoal>()
            if (foundGoal != null) FindReturnInfo(wasSuccessful = true, value = foundGoal)
            else FindReturnInfo(wasSuccessful = false, errMsg = "No goal with name = \"$goalName\" found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not find foal '$goalName'")
        }
    }

    suspend fun getAllGoalsForUser(user: User): FindAllReturnInfo<SavingsGoal> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val allGoals = goals(user.uid).get().await().toObjects<SavingsGoal>()
            FindAllReturnInfo(wasSuccessful = true, values = allGoals)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load goals")
        }
    }

    suspend fun updateGoalName(goal: SavingsGoal, newGoalName: String): UpdateReturnInfo<SavingsGoal> {
        val result = tryUpdateGoalName(goal, newGoalName)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "name for category '${goal.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateGoalName(goal: SavingsGoal, newGoalName: String): UpdateReturnInfo<SavingsGoal> {
        if (newGoalName.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New goal name is empty")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        if (isGoalNameTaken(uid, newGoalName)) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Goal name is already in use")
        return updateDocumentField(
            auth = auth,
            db = db,
            collectionName = "savingsGoals",
            entityTypeDisplayName = "Goal",
            id = goal.id,
            entity = goal,
            property = SavingsGoal::goalName,
            value = newGoalName,
            updatedEntity = goal.copy(goalName = newGoalName)
        )
    }

    suspend fun updateGoalIcon(goal: SavingsGoal, newIconKey: String?): UpdateGoalReturnInfo {
        val result = tryUpdateGoalIcon(goal, newIconKey)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "icon for goal '${goal.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateGoalIcon(goal: SavingsGoal, newIconKey: String?): UpdateReturnInfo<SavingsGoal> {
        if (newIconKey?.isBlank() ?: false) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New icon key path is empty")
        return updateDocumentField(
            auth = auth,
            db = db,
            collectionName = "savingsGoals",
            entityTypeDisplayName = "Goal",
            id = goal.id,
            entity = goal,
            property = SavingsGoal::iconKey,
            value = newIconKey,
            updatedEntity = goal.copy(iconKey = newIconKey)
        )
    }

    suspend fun updateGoalTarget(goal: SavingsGoal, newTargetAmount: Double?): UpdateReturnInfo<SavingsGoal> {
        val result = tryUpdateGoalTarget(goal, newTargetAmount)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "target amount for goal '${goal.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateGoalTarget(goal: SavingsGoal, newTargetAmount: Double?): UpdateReturnInfo<SavingsGoal> {
        if (newTargetAmount != null && newTargetAmount < 0.0) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Target amount cannot be negative")
        return updateDocumentField(
            auth = auth,
            db = db,
            collectionName = "savingsGoals",
            entityTypeDisplayName = "Goal",
            id = goal.id,
            entity = goal,
            property = SavingsGoal::targetAmount,
            value = newTargetAmount,
            updatedEntity = goal.copy(targetAmount = newTargetAmount)
        )
    }

    suspend fun deleteGoal(goal: SavingsGoal): DeleteReturnStatus {
        val result = deleteDocument(
            auth = auth,
            db = db,
            collectionName = "savingsGoals",
            id = goal.id,
            subCollections = emptyList(),
            relatedCollections = SAVINGS_GOAL_RELATED_COLLECTIONS,
            batchSize = BATCH_SIZE
        )
        val wasSuccessful = result == DeleteReturnStatus.Deleted
        val errMsg: String? =
            if (wasSuccessful) null
            else when (result) {
                DeleteReturnStatus.DoesNotExist -> "Goal does not exist"
                DeleteReturnStatus.ReauthenticationFailed -> "No user currently signed in"
                else -> "Unknown reason"
            }
        logOutcome(
            tag = TAG,
            wasSuccessful = wasSuccessful,
            verbOnSuccess = "deleted",
            verbOnFailure = "to delete",
            messageDetails = "goal '${goal.id}'",
            errMsg = errMsg
        )
        return result
    }
}
