package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.DocumentId

/**
 * A spending/income category, stored in Firestore at `users/{uid}/categories/{id}`. The owning user is
 * identified by the path rather than a field
 *
 * @property id The auto-generated document ID, filled in from the document rather than stored as a field
 * @property categoryName The category's name, must be unique per user (enforced in code, since Firestore has no unique constraints)
 * @property budgetAmount The category's budgeted amount, or null if no budget is set
 * @property iconKey The [com.example.mybudgettree.IconCatalog] key for the category's icon, or null to fall back to a name-based default
 */
data class Category(
    @DocumentId val id: String = "",
    val categoryName: String = "",
    val budgetAmount: Double? = null,
    val iconKey: String? = null
)
