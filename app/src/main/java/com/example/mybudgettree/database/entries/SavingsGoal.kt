package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId
import java.time.LocalDateTime

/**
 * A savings goal belonging to a user (e.g. Travel, Wedding, Car), stored in Firestore at
 * `users/{uid}/savingsGoals/{id}`. The owning user is identified by the path rather than a field.
 * Each goal is its own watering can. Saving toward the goal fills that can. Once the can reaches
 * the goal's target, pouring it grows the user's single money tree
 *
 * @property id The auto-generated document ID, filled in from the document rather than stored as a field
 * @property goalName The goal's name, must be unique per user (enforced in code, since Firestore has no unique constraints)
 * @property iconKey The [com.example.mybudgettree.IconCatalog] key for the goal's icon, or null to fall back to a default
 * @property targetAmount The amount the user is aiming to save, or null if no target is set
 * @property lastWateredAt When the user last poured this goal's watering can onto the shared tree, as an ISO-8601 date-time, or null if they never have
 */
data class SavingsGoal(
    @DocumentId val id: String = "",
    val goalName: String = "",
    val iconKey: String? = null,
    val targetAmount: Double? = null,
    val lastWateredAt: String? = null
) {
    /**
     * Parses [lastWateredAt] into a [LocalDateTime]
     *
     * This is a function rather than a property so Firestore's serializer doesn't write it to the database as an extra field
     *
     * @return The last time this goal's can was poured onto the shared tree, or null if it never has been or the stored value cannot be parsed
     */
    fun lastWateredAtAsLocalDateTime(): LocalDateTime? =
        lastWateredAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
}
