package com.example.mybudgettree.database.entries

import java.time.YearMonth
import com.google.firebase.firestore.DocumentId


/**
 * A user's overall minimum/maximum spending goal for a given month, distinct from per-category budgets,
 * stored in Firestore at `users/{uid}/monthlyGoals/{id}`. The owning user is identified by the path
 * rather than a field
 *
 * @property id The auto-generated document ID, filled in from the document rather than stored as a field
 * @property period The year and month this goal applies to, as an ISO-8601 year-month (e.g. "2026-10"), must be unique per user (enforced in code, since Firestore has no unique constraints)
 * @property minGoal The minimum amount the user intends to spend this month
 * @property maxGoal The maximum amount the user intends to spend this month
 */
data class MonthlyGoal(
    @DocumentId val id: String = "",
    val period: String = "",
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0
) {
    /**
     * Parses [period] into a [YearMonth]
     *
     * @return The year and month this goal applies to
     */
    fun periodAsYearMonth(): YearMonth = YearMonth.parse(period)
}
