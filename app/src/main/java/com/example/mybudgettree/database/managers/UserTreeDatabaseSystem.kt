package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.coroutines.cancellation.CancellationException

/**
 * This system manages the money-tree gamification state for users: creating it, finding it, updating its level, period
 * and last watering time, and deleting it
 *
 * Each user has exactly one tree, stored as a single Firestore document at `users/{uid}/userTree/tree`. The ID is the
 * fixed [UserTree.DOCUMENT_ID], so a second tree can never exist. [UserDatabaseSystem.createUser] already creates the
 * tree when the account is made, so [createOrGetUserTree] mostly just returns it. Only the signed-in user's tree can be
 * reached
 *
 * @property auth The Firebase Authentication instance used to find the signed-in user
 * @property db The Firestore instance that holds the tree state
 */
class UserTreeDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "UserTreeDatabaseSystem"

        private const val BATCH_SIZE = 400L
    }

    /**
     * Gets the collection that holds a user's tree, which only ever contains the one [UserTree.DOCUMENT_ID] document
     *
     * @param uid The ID of the user the tree belongs to
     * @return The `users/{uid}/userTree` collection
     */
    private fun treeCollection(uid: String): CollectionReference =
        db.collection("users").document(uid).collection("userTree")

    /**
     * Gets the document that holds the user's tree state, whether or not it exists yet
     *
     * @param user The [User] the tree belongs to
     * @return The `users/{uid}/userTree/tree` document
     */
    private fun treeRef(user: User): DocumentReference =
        treeCollection(user.uid).document(UserTree.DOCUMENT_ID)

    /**
     * Gets the user's tree state, creating it first if they don't have one yet. An existing tree is returned as it is and
     * is never overwritten, so this is safe to call every time the tree is needed
     *
     * @param user The [User] the tree state belongs to, who must be the one signed in
     * @param yearMonth The year and month the starting tree level applies to, only used if the tree has to be created
     * @return A [CreateReturnInfo] holding the existing or newly created [UserTree]
     */
    suspend fun createOrGetUserTree(user: User, yearMonth: YearMonth): CreateReturnInfo<UserTree> {
        val result = tryCreateOrGetUserTree(user, yearMonth)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "created",
            verbOnFailure = "to create",
            messageDetails = "tree state for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateOrGetUserTree(user: User, yearMonth: YearMonth): CreateReturnInfo<UserTree> {
        if (auth.currentUser?.uid != user.uid) return CreateReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val treeRef = treeRef(user)
            val ref = treeRef.get().await()
            if (ref.exists()) {
                val nullableTree = ref.toObject<UserTree>() ?: return CreateReturnInfo(wasSuccessful = false, errMsg = "Document does not contain a valid tree state")
                CreateReturnInfo(
                    wasSuccessful = true,
                    value = nullableTree
                )
            }
            else {
                treeRef.set(UserTree(yearMonth = yearMonth.toString())).await()
                CreateReturnInfo(
                    wasSuccessful = true,
                    value = UserTree(yearMonth = yearMonth.toString())
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create tree state")
        }
    }

    /**
     * Find the user's tree state if it exists
     *
     * @param user The [User] to search for, who must be the one signed in
     * @return A [FindReturnInfo] indicating what happened with the search
     */
    suspend fun findUserTree(user: User): FindReturnInfo<UserTree> {
        if (auth.currentUser?.uid != user.uid) return FindReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val tree = treeRef(user).get().await().toObject<UserTree>() ?: return FindReturnInfo(wasSuccessful = false, errMsg = "Document does not contain a valid tree")
            FindReturnInfo(
                wasSuccessful = true,
                value = tree
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not find tree state for user '${user.uid}'")
        }
    }

    /**
     * Modifies the growth level of the signed-in user's tree
     *
     * @param userTree The [UserTree] being updated, as it currently is
     * @param newTreeLevel The new level, cannot be less than 1
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateTreeLevel(userTree: UserTree, newTreeLevel: Int): UpdateReturnInfo<UserTree> {
        val result = tryUpdateTreeLevel(userTree, newTreeLevel)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "tree level of the tree state for the current auth user",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateTreeLevel(userTree: UserTree, newTreeLevel: Int): UpdateReturnInfo<UserTree> {
        if (newTreeLevel < 1) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Tree level must be greater than 0")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = treeCollection(uid),
            entityTypeDisplayName = "Tree state",
            entity = userTree,
            property = UserTree::treeLevel,
            newValue = newTreeLevel,
            updatedEntity = userTree.copy(treeLevel = newTreeLevel)
        )
    }

    /**
     * Modifies the year and month the tree's level applies to, used when a new month starts and the tree is reset
     *
     * @param userTree The [UserTree] being updated, as it currently is
     * @param newYearMonth The new year and month
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateYearMonth(userTree: UserTree, newYearMonth: YearMonth): UpdateReturnInfo<UserTree> {
        val result = tryUpdateYearMonth(userTree, newYearMonth)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "period of the tree state for the current auth user",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateYearMonth(userTree: UserTree, newYearMonth: YearMonth): UpdateReturnInfo<UserTree> {
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = treeCollection(uid),
            entityTypeDisplayName = "Tree state",
            entity = userTree,
            property = UserTree::yearMonth,
            newValue = newYearMonth.toString(),
            updatedEntity = userTree.copy(yearMonth = newYearMonth.toString())
        )
    }

    /**
     * Modifies the last time the user poured the watering can on the signed-in user's tree
     *
     * @param userTree The [UserTree] being updated, as it currently is
     * @param newWateringTime The new last watering time
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateLastWateringTime(userTree: UserTree, newWateringTime: LocalDateTime): UpdateReturnInfo<UserTree> {
        val result = tryUpdateLastWateringTime(userTree, newWateringTime)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "watering time of the tree state for the current auth user",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateLastWateringTime(userTree: UserTree, newWateringTime: LocalDateTime): UpdateReturnInfo<UserTree> {
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = treeCollection(uid),
            entityTypeDisplayName = "Tree state",
            entity = userTree,
            property = UserTree::lastWateringTime,
            newValue = newWateringTime.toString(),
            updatedEntity = userTree.copy(lastWateringTime = newWateringTime.toString())
        )
    }

    /**
     * Deletes the signed-in user's tree state from the database
     *
     * This isn't recommended: the tree belongs to the user, so it should be removed by deleting the user, which
     * [UserDatabaseSystem.deleteUser] does along with the rest of their data. This function is only here in case the
     * tree has to be removed on its own. Without a tree, the user has none until [createOrGetUserTree] makes a new one
     * at level 1
     *
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteUserTree(): DeleteReturnStatus {
        val uid = auth.uid
        val result =
            if (uid == null) DeleteReturnStatus.ReauthenticationFailed
            else deleteDocument(
                db = db,
                collection = treeCollection(uid),
                id = UserTree.DOCUMENT_ID,
                subcollections = emptyList(),
                relatedCollections = emptyList(),
                batchSize = BATCH_SIZE
            )
        val wasSuccessful = result == DeleteReturnStatus.Deleted
        val errMsg: String? =
            if (wasSuccessful) null
            else when (result) {
                DeleteReturnStatus.DoesNotExist -> "Tree state does not exist"
                DeleteReturnStatus.ReauthenticationFailed -> "No user currently signed in"
                else -> "Unknown reason"
            }
        logOutcome(
            tag = TAG,
            wasSuccessful = wasSuccessful,
            verbOnSuccess = "deleted",
            verbOnFailure = "to delete",
            messageDetails = "tree state for current auth user",
            errMsg = errMsg
        )
        return result
    }
}
