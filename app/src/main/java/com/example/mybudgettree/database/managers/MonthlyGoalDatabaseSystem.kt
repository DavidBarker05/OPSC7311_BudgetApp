package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.entries.MonthlyGoal
import com.example.mybudgettree.database.entries.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.YearMonth
import com.example.mybudgettree.database.managers.shared.*
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * This system manages the user's overall monthly minimum/maximum spending goal,
 * distinct from per-category budgets
 *
 */
class MonthlyGoalDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "MonthlyGoalDatabaseSystem"
    }

    /**
     * Finds the user's monthly goal for the given period, if one has been set
     *
     * @param user The [User] to look up
     * @param period The year and month to look up
     * @return The [MonthlyGoal] record, or null if none is set
     */
    suspend fun getGoal(user: User, period: YearMonth): MonthlyGoal? {
        if (auth.currentUser?.uid != user.uid) return null
        return try {
            db.collection("users").document(user.uid)
                .collection("monthlyGoals")
                .document(period.toString())
                .get().await()
                .toObject<MonthlyGoal>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates or replaces the user's monthly goal for the given period
     *
     * @param user The [User] the goal belongs to
     * @param period The year and month this goal applies to
     * @param minGoal The minimum amount the user intends to spend this month, cannot be negative
     * @param maxGoal The maximum amount the user intends to spend this month, cannot be less than [minGoal]
     * @return A [SaveReturnInfo] indicating what happened with the save
     */
    suspend fun saveGoal(user: User, period: YearMonth, minGoal: Double, maxGoal: Double): SaveReturnInfo<MonthlyGoal> {
        val result = trySaveGoal(user, period, minGoal, maxGoal)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "saved",
            verbOnFailure = "to save",
            messageDetails = "monthly goal '$period' for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun trySaveGoal(user: User, period: YearMonth, minGoal: Double, maxGoal: Double): SaveReturnInfo<MonthlyGoal> {
        if (minGoal < 0.0) return SaveReturnInfo(wasSuccessful = false, errMsg = "Minimum goal cannot be negative")
        if (maxGoal < minGoal) return SaveReturnInfo(wasSuccessful = false, errMsg = "Maximum goal cannot be less than the minimum goal")
        val uid = auth.uid ?: return SaveReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (uid != user.uid) return SaveReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val goal = MonthlyGoal(id = period.toString(), minGoal = minGoal, maxGoal = maxGoal)
            db.collection("users").document(uid).collection("monthlyGoals")
                .document(period.toString())
                .set(goal)
                .await()
            SaveReturnInfo(wasSuccessful = true, value = goal.copy(id = period.toString()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not save monthly goal")
        }
    }
}
