package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Category

object CategoryGarden {
    val defaultNames = listOf(
        "Food",
        "Transport",
        "Medicine",
        "Groceries",
        "Rent",
        "Gifts",
        "Savings",
        "Entertainment"
    )

    private fun defaultIconKeyFor(name: String): String? = when (name.trim().lowercase()) {
        "food" -> "food"
        "transport" -> "transport"
        "medicine" -> "medicine"
        "groceries" -> "groceries"
        "rent" -> "rent"
        "gifts" -> "gifts"
        "savings" -> "savings"
        "entertainment" -> "entertainment"
        else -> null
    }

    fun iconKeyFor(category: Category): String =
        category.iconKey ?: defaultIconKeyFor(category.categoryName) ?: IconCatalog.DEFAULT_KEY

    fun iconRes(category: Category): Int = IconCatalog.resFor(iconKeyFor(category))

    fun sort(categories: List<Category>): List<Category> {
        val order = defaultNames.mapIndexed { index, name -> name.lowercase() to index }.toMap()
        return categories.sortedWith(
            compareBy<Category> { order[it.categoryName.lowercase()] ?: Int.MAX_VALUE }
                .thenBy { it.categoryName.lowercase() }
        )
    }
}
