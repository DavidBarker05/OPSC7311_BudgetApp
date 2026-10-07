package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId
import java.time.YearMonth

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
