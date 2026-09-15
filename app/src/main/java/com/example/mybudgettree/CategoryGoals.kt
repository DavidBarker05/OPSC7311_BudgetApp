package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem

object CategoryGoals {
    val names = listOf("Travel", "Wedding", "Car")
    private val defaultTargets = mapOf(
        "Travel" to 25_000.0,
        "Wedding" to 34_700.0,
        "Car" to 20_000.0
    )

    fun isGoal(name: String): Boolean = names.any { it.equals(name.trim(), ignoreCase = true) }

    fun spendingOnly(categories: List<Category>): List<Category> =
        categories.filterNot { isGoal(it.categoryName) }

    fun goalsOnly(categories: List<Category>): List<Category> =
        names.mapNotNull { name ->
            categories.firstOrNull { it.categoryName.equals(name, ignoreCase = true) }
        }

    fun iconRes(name: String): Int = when (name.trim().lowercase()) {
        "travel" -> R.drawable.ic_plane
        "wedding" -> R.drawable.ic_wedding_rings
        "car" -> R.drawable.ic_car
        else -> R.drawable.ic_cat_generic
    }

    suspend fun ensureForUser(user: User, categoryDatabaseSystem: CategoryDatabaseSystem): List<Category> {
        names.forEach { name ->
            val existing = categoryDatabaseSystem.findCategory(user, name).category
            if (existing == null) {
                val created = categoryDatabaseSystem.createCategory(user, name).category
                val target = defaultTargets[name]
                if (created != null && target != null) {
                    categoryDatabaseSystem.updateCategoryBudget(created, target)
                }
            } else if (existing.budgetAmount == null) {
                defaultTargets[name]?.let { categoryDatabaseSystem.updateCategoryBudget(existing, it) }
            }
        }
        return goalsOnly(categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty())
    }
}
