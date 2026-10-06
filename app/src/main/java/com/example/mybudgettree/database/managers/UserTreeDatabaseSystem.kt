package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.YearMonth

/**
 * This system manages the money-tree gamification state for users. It is currently
 * insert-only: nothing reads or updates the tree state yet, this only exists so the
 * schema is in place ahead of the final PoE
 *
 * Each user has exactly one tree, stored as a single Firestore document at `users/{uid}/userTree/tree`
 *
 * @property db The Firestore instance that holds the tree state
 */
class UserTreeDatabaseSystem(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        private const val TAG = "UserTreeDatabaseSystem"
    }

    /**
     * Gets the document that holds the user's tree state. Its ID is the fixed [UserTree.DOCUMENT_ID], so a user
     * can only ever have one
     *
     * @param user The [User] the tree state belongs to
     * @return The [DocumentReference] pointing at the user's tree, whether or not it exists yet
     */
    private fun treeRef(user: User): DocumentReference =
        db.collection("users").document(user.uid).collection("userTree").document(UserTree.DOCUMENT_ID)

    /**
     * Creates the initial tree state for a newly created user, unless they already have one
     *
     * @param user The [User] the tree state belongs to
     * @param yearMonth The year and month the starting tree level applies to
     */
    suspend fun createUserTree(user: User, yearMonth: YearMonth) {
        val ref = treeRef(user)
        // Checking and writing in one transaction means it can't be created twice, even if called twice at once
        val wasCreated = db.runTransaction { tx ->
            if (tx.get(ref).exists()) {
                false
            } else {
                tx.set(ref, UserTree(yearMonth = yearMonth.toString()))
                true
            }
        }.await()
        if (wasCreated) Log.i(TAG, "Created tree state for user '${user.uid}'")
        else Log.w(TAG, "User '${user.uid}' already has a tree state")
    }

    /**
     * Find the user's tree state if it exists
     *
     * @param user The [User] to search for
     * @return The [UserTree] record, or null if not found
     */
    suspend fun findUserTree(user: User): UserTree? = treeRef(user).get().await().toObject(UserTree::class.java)
}
