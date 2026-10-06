package com.example.mybudgettree.database.entries

import java.time.LocalDateTime
import java.time.YearMonth

/**
 * A user's money-tree gamification state, split out of [User] since it is not required until the final PoE
 * and should stay inert until then. Stored in Firestore at `users/{uid}/userTree/tree`. There is exactly one per
 * user, so the document ID is the fixed [DOCUMENT_ID] rather than an auto-generated one, and the owning user is
 * identified by the path rather than a field
 *
 * The year-month and date-time are stored as ISO-8601 strings, since Firestore can't store `java.time` types;
 * use the `...As...` functions to read them back as `java.time` objects
 *
 * @property treeLevel The current growth level of the user's money tree
 * @property yearMonth The year and month the current [treeLevel] applies to, as an ISO-8601 year-month (e.g. "2026-10"), used to detect when a new month has started so the tree can reset
 * @property lastWateringTime The last time the user "poured" the watering can, as an ISO-8601 date-time, or null if never
 */
data class UserTree(
    val treeLevel: Int = 1,
    val yearMonth: String = "",
    val lastWateringTime: String? = null
) {
    /**
     * Parses [yearMonth] into a [YearMonth]
     *
     * @return The year and month the current tree level applies to
     */
    fun yearMonthAsYearMonth(): YearMonth = YearMonth.parse(yearMonth)

    /**
     * Parses [lastWateringTime] into a [LocalDateTime]
     *
     * @return The last time the watering can was poured, or null if it never has been
     */
    fun lastWateringTimeAsNullableLocalDateTime(): LocalDateTime? =
        if (lastWateringTime != null) LocalDateTime.parse(lastWateringTime)
        else null

    companion object {
        /**
         * The fixed ID of the one tree document every user has, so a second one can never be created
         */
        const val DOCUMENT_ID = "tree"
    }
}
