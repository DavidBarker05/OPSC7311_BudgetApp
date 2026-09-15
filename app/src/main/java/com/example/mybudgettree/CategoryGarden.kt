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

    fun iconRes(name: String): Int = when (name.trim().lowercase()) {
        "food" -> R.drawable.ic_utensils
        "transport" -> R.drawable.ic_cat_transport
        "medicine" -> R.drawable.ic_cat_medicine
        "groceries" -> R.drawable.ic_cat_groceries
        "rent" -> R.drawable.ic_cat_rent
        "gifts" -> R.drawable.ic_cat_gifts
        "savings" -> R.drawable.ic_cat_savings
        "entertainment" -> R.drawable.ic_cat_entertainment
        else -> R.drawable.ic_cat_generic
    }

    fun sort(categories: List<Category>): List<Category> {
        val order = defaultNames.mapIndexed { index, name -> name.lowercase() to index }.toMap()
        return categories.sortedWith(
            compareBy<Category> { order[it.categoryName.lowercase()] ?: Int.MAX_VALUE }
                .thenBy { it.categoryName.lowercase() }
        )
    }
}
