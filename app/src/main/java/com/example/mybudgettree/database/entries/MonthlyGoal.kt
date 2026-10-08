package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId
import java.time.YearMonth

/**
 * A user's overall spending goal for one month, stored in Firestore at `users/{uid}/monthlyGoals/{yyyy-MM}`. The
 * document ID is the ISO-8601 year-month the goal applies to (e.g. "2026-10"), so a month can only ever have one
 * goal, and the owning user is identified by the path rather than a field
 *
 * @property id The year-month the goal applies to, as the document ID rather than a stored field; use [idAsYearMonth] to read it as a [YearMonth]
 * @property minGoal The minimum amount the user intends to spend this month
 * @property maxGoal The maximum amount the user intends to spend this month
 */
data class MonthlyGoal(
    @DocumentId val id: String = "",
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0
) {
    /**
     * Parses [id] into a [YearMonth]
     *
     * @return The year and month this goal applies to
     */
    fun idAsYearMonth(): YearMonth = YearMonth.parse(id)
}
