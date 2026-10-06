package com.example.mybudgettree.database.entries

import java.time.LocalDate
import java.time.LocalTime
import com.google.firebase.firestore.DocumentId

/**
 * A single income record, stored in Firestore at `users/{uid}/incomes/{id}`. The owning user is identified by the
 * path rather than a field, while the category it belongs to is referenced by [categoryId]
 *
 * Dates and times are stored as ISO-8601 strings, since Firestore can't store `java.time` types; use the
 * `...As...` functions to read them back as `java.time` objects
 *
 * @property id The auto-generated document ID, filled in from the document rather than stored as a field
 * @property categoryId The id of the [Category] the income belongs to; Firestore doesn't enforce this link, so it must be validated in code
 * @property description The income's name
 * @property amount The income amount
 * @property date The date the income occurred on, as an ISO-8601 date (e.g. "2026-10-05")
 * @property startTime The time the income started, as an ISO-8601 time (e.g. "14:30")
 * @property endTime The time the income ended, as an ISO-8601 time (e.g. "15:45")
 * @property imagePath The path to the income's proof image, or null if none is set
 */
data class Income(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val imagePath: String? = null
) {
    /**
     * Parses [date] into a [LocalDate]
     *
     * @return The date the income occurred on
     */
    fun dateAsLocalDate(): LocalDate = LocalDate.parse(date)

    /**
     * Parses [startTime] into a [LocalTime]
     *
     * @return The time the income started
     */
    fun startTimeAsLocalTime(): LocalTime = LocalTime.parse(startTime)

    /**
     * Parses [endTime] into a [LocalTime]
     *
     * @return The time the income ended
     */
    fun endTimeAsLocalTime(): LocalTime = LocalTime.parse(endTime)
}