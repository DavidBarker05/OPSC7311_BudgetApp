package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A single deposit against a savings goal, stored in Firestore at `users/{uid}/savingsContributions/{id}`.
 * The owning user is identified by the path rather than a field, while the goal it applies to is
 * referenced by [goalId]
 *
 * The date and time are stored as ISO-8601 strings, since Firestore can't store `java.time` types; use the
 * `...As...` functions to read them back as `java.time` objects
 *
 * @property id The auto-generated document ID, filled in from the document rather than stored as a field
 * @property goalId The id of the [SavingsGoal] this contribution applies to; Firestore doesn't enforce this link, so it must be validated in code
 * @property amount The amount deposited
 * @property date The date the contribution was made, as an ISO-8601 date (e.g. "2026-10-05")
 * @property createdAt The moment the contribution was recorded, as an ISO-8601 date-time (e.g. "2026-10-05T14:30:00"), kept for future watering-can logic
 */
data class SavingsContribution(
    @DocumentId val id: String = "",
    val goalId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val createdAt: String = ""
) {
    /**
     * Parses [date] into a [LocalDate]
     *
     * @return The date the contribution was made
     */
    fun dateAsLocalDate(): LocalDate = LocalDate.parse(date)

    /**
     * Parses [createdAt] into a [LocalDateTime]
     *
     * @return The moment the contribution was recorded
     */
    fun createdAtAsLocalDateTime(): LocalDateTime = LocalDateTime.parse(createdAt)
}
