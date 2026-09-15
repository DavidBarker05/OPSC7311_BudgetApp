package com.example.mybudgettree

import android.util.Log
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem

object CategoryGoals {
    private const val TAG = "CategoryGoals"
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
                val createResult = categoryDatabaseSystem.createCategory(user, name)
                val created = createResult.category
                if (created == null) {
                    Log.w(TAG, "Failed to seed goal category '$name' for user '${user.username}': ${createResult.errMsg}")
                    return@forEach
                }
                val target = defaultTargets[name]
                if (target != null) {
                    val budgetResult = categoryDatabaseSystem.updateCategoryBudget(created, target)
                    if (budgetResult.status == CategoryDatabaseSystem.UpdateCategoryReturnStatus.Failed) {
                        Log.w(TAG, "Failed to set default target for goal '$name': ${budgetResult.errMsg}")
                    }
                }
            } else if (existing.budgetAmount == null) {
                defaultTargets[name]?.let {
                    val budgetResult = categoryDatabaseSystem.updateCategoryBudget(existing, it)
                    if (budgetResult.status == CategoryDatabaseSystem.UpdateCategoryReturnStatus.Failed) {
                        Log.w(TAG, "Failed to set default target for goal '$name': ${budgetResult.errMsg}")
                    }
                }
            }
        }
        return goalsOnly(categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty())
    }
}
