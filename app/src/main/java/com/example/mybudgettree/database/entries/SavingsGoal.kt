package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId

/**
 * A savings goal belonging to a user (e.g. Travel, Wedding, Car), stored in Firestore at
 * `users/{uid}/savingsGoals/{id}`. The owning user is identified by the path rather than a field
 *
 * @property id The auto-generated document ID, filled in from the document rather than stored as a field
 * @property goalName The goal's name, must be unique per user (enforced in code, since Firestore has no unique constraints)
 * @property iconKey The [com.example.mybudgettree.IconCatalog] key for the goal's icon, or null to fall back to a default
 * @property targetAmount The amount the user is aiming to save, or null if no target is set
 */
data class SavingsGoal(
    @DocumentId val id: String = "",
    val goalName: String = "",
    val iconKey: String? = null,
    val targetAmount: Double? = null
)
