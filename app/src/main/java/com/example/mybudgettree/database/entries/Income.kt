package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId
import java.time.LocalDate
import java.time.LocalTime

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
 * @property imagePaths The local path to the income's proof image on each device that has one, keyed by device ID (a random ID the app generates once per install). The image itself never leaves the device that saved it, so each device keeps its own path and one device can't overwrite another's. Empty if no device has an image
 */
data class Income(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val imagePaths: Map<String, String> = emptyMap()
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

    /**
     * Whether any device has saved an image for this income
     *
     * @return True if at least one device has an image
     */
    fun hasImage(): Boolean = imagePaths.isNotEmpty()

    /**
     * Gets the image path saved by one device
     *
     * @param deviceId The ID of the device to look up
     * @return The local path on that device, or null if it has no image for this income
     */
    fun imagePathFor(deviceId: String): String? = imagePaths[deviceId]
}
