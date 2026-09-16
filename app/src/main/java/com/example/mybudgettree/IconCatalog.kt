package com.example.mybudgettree

/**
 * The fixed set of vector icons a user can assign to a category or savings goal.
 * Storing the [key] on the entity (rather than a raw drawable id) keeps the choice
 * portable across resource id churn and app updates.
 */
object IconCatalog {
    data class Icon(val key: String, val res: Int)

    val icons = listOf(
        Icon("food", R.drawable.ic_utensils),
        Icon("transport", R.drawable.ic_cat_transport),
        Icon("medicine", R.drawable.ic_cat_medicine),
        Icon("groceries", R.drawable.ic_cat_groceries),
        Icon("rent", R.drawable.ic_cat_rent),
        Icon("gifts", R.drawable.ic_cat_gifts),
        Icon("savings", R.drawable.ic_cat_savings),
        Icon("entertainment", R.drawable.ic_cat_entertainment),
        Icon("generic", R.drawable.ic_cat_generic),
        Icon("travel", R.drawable.ic_plane),
        Icon("wedding", R.drawable.ic_wedding_rings),
        Icon("car", R.drawable.ic_car)
    )

    const val DEFAULT_KEY = "generic"

    fun resFor(key: String?): Int = icons.firstOrNull { it.key == key }?.res ?: icons.last { it.key == DEFAULT_KEY }.res
}
