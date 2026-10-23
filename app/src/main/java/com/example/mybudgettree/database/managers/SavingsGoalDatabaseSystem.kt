package com.example.mybudgettree.database.managers

import com.example.mybudgettree.GoalTreeGame
import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * This system manages savings goal state, uniqueness validation, discovery, updates, and removals
 *
 * Goals are stored in Firestore at `users/{uid}/savingsGoals/{id}`. Only the signed-in user's data can be reached, so
 * each function checks that the user it's given is the one signed in. Goal names are unique per user, which Firestore
 * can't enforce, so it's checked in code. Deleting a goal also deletes its contributions
 *
 * @property auth The Firebase Authentication instance used to find the signed-in user
 * @property db The Firestore instance that holds the goals
 */
class SavingsGoalDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun goals(uid: String): CollectionReference = db.collection("users").document(uid).collection("savingsGoals")
    private fun relatedCollections(uid: String): List<RelatedCollection> = listOf(
        RelatedCollection(
            collection = db.collection("users").document(uid).collection("savingsContributions"),
            referenceField = "goalId"
        )
    )

    companion object {
        private const val TAG = "SavingsGoalDatabaseSystem"

        private const val BATCH_SIZE = 400L
    }



    /**
     * Checks whether a user already has a savings goal with the given name. Names are compared exactly, so "Car" and "car" are different
     *
     * @param uid The ID of the user to check, who must be the one signed in
     * @param goalName The name to look for
     * @return True if the user has a goal with that name. False if the name is free, or if the name is blank or [uid] isn't the signed-in user
     */
    suspend fun isGoalNameTaken(uid: String, goalName: String): Boolean {
        if (uid.isBlank() || goalName.isBlank() || auth.currentUser?.uid != uid) return false
        return !goals(uid)
            .whereEqualTo("goalName", goalName)
            .limit(1)
            .get().await()
            .isEmpty
    }

    /**
     * Checks whether the user already has a savings goal with the given name
     *
     * @param user The [User] to check
     * @param goalName The name to look for
     * @return True if the user has a goal with that name, see the overload that takes a user ID
     */
    suspend fun isGoalNameTaken(user: User, goalName: String): Boolean = isGoalNameTaken(user.uid, goalName)

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
        if (targetAmount != null && targetAmount < 0.0) return CreateReturnInfo(wasSuccessful = false, errMsg = "Target amount cannot be negative")
        if (auth.currentUser?.uid != user.uid) return CreateReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            if (isGoalNameTaken(user, goalName)) return CreateReturnInfo(wasSuccessful = false, errMsg = "User already has a goal with name \"$goalName\"")
            val goals = goals(user.uid)
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
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create goal")
        }
    }

    /**
     * Find the signed-in user's savings goal by its ID if it exists
     *
     * @param goalId The id to search for
     * @return A [FindReturnInfo] indicating what happened with the search
     */
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

    /**
     * Find the user's savings goal by name if it exists
     *
     * @param user The [User] the goal should belong to
     * @param goalName The goal name to search for
     * @return A [FindReturnInfo] indicating what happened with the search
     */
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

    /**
     * Retrieves every savings goal belonging to the user
     *
     * @param user The [User] to retrieve goals for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
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

    /**
     * Modifies the name for the goal
     *
     * @param goal The [SavingsGoal] being updated
     * @param newGoalName The new goal name, must not already be used by another of the user's goals
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateGoalName(goal: SavingsGoal, newGoalName: String): UpdateReturnInfo<SavingsGoal> {
        val result = tryUpdateGoalName(goal, newGoalName)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "name for goal '${goal.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateGoalName(goal: SavingsGoal, newGoalName: String): UpdateReturnInfo<SavingsGoal> {
        if (newGoalName.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New goal name is empty")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        if (isGoalNameTaken(uid, newGoalName)) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Goal name is already in use")
        return updateDocumentField(
            collection = goals(uid),
            entityTypeDisplayName = "Goal",
            entity = goal,
            property = SavingsGoal::goalName,
            newValue = newGoalName,
            updatedEntity = goal.copy(goalName = newGoalName)
        )
    }

    /**
     * Modifies the icon for the goal
     *
     * @param goal The [SavingsGoal] being updated
     * @param newIconKey The new [com.example.mybudgettree.IconCatalog] key, or null to fall back to the default
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateGoalIcon(goal: SavingsGoal, newIconKey: String?): UpdateReturnInfo<SavingsGoal> {
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
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = goals(uid),
            entityTypeDisplayName = "Goal",
            entity = goal,
            property = SavingsGoal::iconKey,
            newValue = newIconKey,
            updatedEntity = goal.copy(iconKey = newIconKey)
        )
    }

    /**
     * Modifies the target amount for the goal
     *
     * @param goal The [SavingsGoal] being updated
     * @param newTargetAmount The new amount to aim to save, or null to remove the target, cannot be negative
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
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
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = goals(uid),
            entityTypeDisplayName = "Goal",
            entity = goal,
            property = SavingsGoal::targetAmount,
            newValue = newTargetAmount,
            updatedEntity = goal.copy(targetAmount = newTargetAmount)
        )
    }

    /**
     * Pours this goal's watering can onto the user's one money tree
     *
     * The can must be full: savings added since it was last poured have to reach the goal's target.
     * The shared tree then grows by one level, and every deposit already in this can counts as poured,
     * so the can stays empty until the user saves toward this goal again. Other goals' cans are left
     * as they are. The latest deposit date is left as it is, so the can can still show when it was last
     * filled. The can and the tree are updated in one transaction, so a second tap sees that the water
     * was already poured and does not grow the tree again
     *
     * @param goal The [SavingsGoal] whose can is being poured
     * @param wateredAt The moment the user poured the can
     * @return An [UpdateReturnInfo] with the shared tree after it grows, or a failure when the can is not full or the goal is missing
     */
    suspend fun waterTree(goal: SavingsGoal, wateredAt: LocalDateTime): UpdateReturnInfo<UserTree> {
        val result = tryWaterTree(goal, wateredAt)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "shared tree using the can for goal '${goal.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryWaterTree(goal: SavingsGoal, wateredAt: LocalDateTime): UpdateReturnInfo<UserTree> {
        if (goal.id.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Goal id is empty")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return try {
            val userRef = db.collection("users").document(uid)
            val goalRef = goals(uid).document(goal.id)
            val treeRef = userRef.collection("userTree").document(UserTree.DOCUMENT_ID)
            val contributions = userRef.collection("savingsContributions")
                .whereEqualTo("goalId", goal.id)
                .get().await()
                .toObjects<SavingsContribution>()
            val updated = db.runTransaction { tx ->
                val currentGoal = tx.get(goalRef).toObject<SavingsGoal>()
                    ?: throw IllegalStateException("Goal does not exist")
                val treeSnap = tx.get(treeRef)
                val currentTree = if (treeSnap.exists()) {
                    treeSnap.toObject<UserTree>()
                        ?: throw IllegalStateException("Document does not contain a valid tree")
                } else {
                    UserTree(yearMonth = YearMonth.now().toString())
                }
                val can = GoalTreeGame.canState(currentGoal, contributions)
                if (can.unpouredAmount <= 0.0) throw IllegalStateException(GoalTreeGame.EMPTY_CAN_MESSAGE)
                if (!can.canWater) throw IllegalStateException(GoalTreeGame.NOT_FULL_MESSAGE)
                val newLevel = GoalTreeGame.nextLevel(currentTree.treeLevel)
                val watered = wateredAt.toString()
                val grown = currentTree.copy(treeLevel = newLevel, lastWateringTime = watered)
                if (treeSnap.exists()) {
                    tx.update(treeRef, "treeLevel", newLevel)
                    tx.update(treeRef, "lastWateringTime", watered)
                } else {
                    tx.set(treeRef, grown)
                }
                tx.update(goalRef, "lastWateredAt", watered)
                grown
            }.await()
            UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not water the tree")
        }
    }

    /**
     * Deletes the goal from the database, along with every contribution made to it
     *
     * @param goal The [SavingsGoal] to delete
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteGoal(goal: SavingsGoal): DeleteReturnStatus {
        val uid = auth.uid
        val result =
            if (uid == null) DeleteReturnStatus.ReauthenticationFailed
            else deleteDocument(
                db = db,
                collection = goals(uid),
                id = goal.id,
                subcollections = emptyList(),
                relatedCollections = relatedCollections(uid),
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
