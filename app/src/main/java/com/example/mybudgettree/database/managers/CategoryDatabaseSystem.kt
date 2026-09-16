package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.daos.CategoryDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category

/**
 * This system manages category state, uniqueness validation, discovery, updates, and removals
 *
 * @property categoryDao The underlying Data Access Object managing RoomDB operations
 * @property userDatabaseSystem Used to validate that the owning user exists before category operations proceed
 */
class CategoryDatabaseSystem(
    private val categoryDao: CategoryDao,
    private val userDatabaseSystem: UserDatabaseSystem
) {

    companion object {
        private const val TAG = "CategoryDatabaseSystem"

        private fun logUpdateOutcome(action: String, status: UpdateCategoryReturnStatus, errMsg: String?) {
            when (status) {
                UpdateCategoryReturnStatus.Succeeded -> Log.i(TAG, "Successfully $action")
                UpdateCategoryReturnStatus.Failed -> Log.w(TAG, "Failed to $action: $errMsg")
                UpdateCategoryReturnStatus.NoChange -> Log.d(TAG, "No change $action")
            }
        }

        private fun logCreateOutcome(action: String, wasSuccessful: Boolean, errMsg: String?) {
            if (wasSuccessful) Log.i(TAG, "Successfully $action") else Log.w(TAG, "Failed to $action: $errMsg")
        }
    }

    /**
     * Wraps the category creation return in a detailed form
     *
     * @property wasSuccessful True if the category was created without error, false otherwise
     * @property category The newly created [Category] record if successful, or null on execution failure
     * @property errMsg The explanatory message detailing why creation failed, or null if successful
     */
    data class CreateCategoryReturnInfo(
        val wasSuccessful: Boolean,
        val category: Category? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the search request return in a detailed form
     *
     * @property wasSuccessful True if the target record was found, false otherwise
     * @property category The retrieved [Category] entity if located, or null if the record doesn't exist
     * @property errMsg The diagnostic message stating the cause of failure, or null if found
     */
    data class FindCategoryReturnInfo(
        val wasSuccessful: Boolean,
        val category: Category? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the bulk retrieval return in a detailed form
     *
     * @property wasSuccessful True if the categories were retrieved without error, false otherwise
     * @property categories The retrieved list of [Category] entities if successful, or null on execution failure
     * @property errMsg The diagnostic message stating the cause of failure, or null if successful
     */
    data class FindAllCategoriesReturnInfo(
        val wasSuccessful: Boolean,
        val categories: List<Category>? = null,
        val errMsg: String? = null
    )

    /**
     * Identifies exactly what happened when updating a category
     */
    enum class UpdateCategoryReturnStatus {
        /**
         * The update failed
         */
        Failed,
        /**
         * The update did not change any data, but did not fail
         */
        NoChange,
        /**
         * The update successfully changed data
         */
        Succeeded
    }

    /**
     * Wraps the update request return in a detailed form
     *
     * @property status A [UpdateCategoryReturnStatus] specifying the operation outcome
     * @property category The modified [Category] record containing updated fields, or null if the task failed
     * @property errMsg The error message, only set if the [status] is [UpdateCategoryReturnStatus.Failed]
     */
    data class UpdateCategoryReturnInfo(
        val status: UpdateCategoryReturnStatus,
        val category: Category? = null,
        val errMsg: String? = null
    )

    /**
     * Indicates what happened when trying to delete a category
     */
    enum class CategoryDeleteReturnStatus {
        /**
         * The category couldn't be deleted because it doesn't exist in the database
         */
        DoesNotExist,
        /**
         * The category was successfully deleted
         */
        Deleted
    }

    /**
     * Creates a new category for the user after validating the name and confirming it isn't already in use
     *
     * @param user The [User] the category belongs to
     * @param categoryName The desired name for the category, must be unique for the user
     * @return A [CreateCategoryReturnInfo] indicating what happened with the creation
     */
    suspend fun createCategory(user: User, categoryName: String): CreateCategoryReturnInfo {
        val result = run {
            if (categoryName.isBlank()) return@run CreateCategoryReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
            val userStatus = userDatabaseSystem.findUser(user.username)
            if (!userStatus.wasSuccessful) return@run CreateCategoryReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
            val category = Category(username = user.username, categoryName = categoryName)
            val id = categoryDao.insertCategory(category)
            if (id == -1L) return@run CreateCategoryReturnInfo(wasSuccessful = false, errMsg = "User already has a category with name \"$categoryName\"")
            CreateCategoryReturnInfo(wasSuccessful = true, category = category.copy(id = id))
        }
        logCreateOutcome("create category '$categoryName' for user '${user.username}'", result.wasSuccessful, result.errMsg)
        return result
    }

    /**
     * Find the category in the database if it exists
     *
     * @param categoryId The id to search for
     * @return A [FindCategoryReturnInfo] indicating what happened with the search
     */
    suspend fun findCategory(categoryId: Long): FindCategoryReturnInfo {
        val category = categoryDao.findCategory(categoryId) ?: return FindCategoryReturnInfo(wasSuccessful = false, errMsg = "Category does not exist")
        return FindCategoryReturnInfo(wasSuccessful = true, category = category)
    }

    /**
     * Find the user's category by name if it exists
     *
     * @param user The [User] the category should belong to
     * @param categoryName The category name to search for
     * @return A [FindCategoryReturnInfo] indicating what happened with the search
     */
    suspend fun findCategory(user: User, categoryName: String): FindCategoryReturnInfo {
        if (categoryName.isBlank()) return FindCategoryReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return FindCategoryReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        val foundCategory = categoryDao.findCategory(user.username, categoryName)
        return if (foundCategory != null) FindCategoryReturnInfo(wasSuccessful = true, category = foundCategory)
        else FindCategoryReturnInfo(wasSuccessful = false, errMsg = "No category with name = \"$categoryName\" found")
    }

    /**
     * Check if the user already has a category with the given name
     *
     * @param user The [User] the category should belong to
     * @param categoryName The category name to search for
     * @return True if the category exists, false otherwise
     */
    suspend fun doesCategoryExist(user: User, categoryName: String): Boolean = findCategory(user, categoryName).wasSuccessful

    /**
     * Check if the category still exists in the database
     *
     * @param category The [Category] to validate
     * @return True if the category still exists, false otherwise
     */
    suspend fun isCategoryStillValid(category: Category): Boolean = findCategory(category.id).wasSuccessful

    /**
     * Retrieves every category belonging to the user
     *
     * @param user The [User] to retrieve categories for
     * @return A [FindAllCategoriesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun getAllCategoriesForUser(user: User): FindAllCategoriesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return FindAllCategoriesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return FindAllCategoriesReturnInfo(wasSuccessful = true, categories = categoryDao.retrieveAllCategories(user.username))
    }

    /**
     * Modifies the name for the category
     *
     * @param category The [Category] being updated
     * @param newCategoryName The new category name
     * @return An [UpdateCategoryReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryName(category: Category, newCategoryName: String): UpdateCategoryReturnInfo {
        val result = run {
            if (newCategoryName.isBlank()) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category name is empty")
            if (category.categoryName == newCategoryName) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.NoChange, category = category)
            val userStatus = userDatabaseSystem.findUser(category.username)
            if (!userStatus.wasSuccessful) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = userStatus.errMsg)
            categoryDao.findCategory(category.username, category.categoryName) ?: return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category does not exist")
            if (categoryDao.findCategory(category.username, newCategoryName) != null) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category name already in use")
            categoryDao.updateCategoryName(category.id, newCategoryName)
            UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Succeeded, category = category.copy(categoryName = newCategoryName))
        }
        logUpdateOutcome("update name for category '${category.categoryName}'", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the budget for the category
     *
     * @param category The [Category] being updated
     * @param newBudgetAmount The new budgeted amount, or null to remove the budget, cannot be negative
     * @return An [UpdateCategoryReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryBudget(category: Category, newBudgetAmount: Double?): UpdateCategoryReturnInfo {
        val result = run {
            if (category.budgetAmount == newBudgetAmount) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.NoChange, category = category)
            if (newBudgetAmount != null && newBudgetAmount < 0.0) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Budget amount cannot be negative")
            if (!isCategoryStillValid(category)) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category does not exist")
            categoryDao.updateCategoryBudget(category.id, newBudgetAmount)
            UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Succeeded, category = category.copy(budgetAmount = newBudgetAmount))
        }
        logUpdateOutcome("update budget for category '${category.categoryName}'", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the icon for the category
     *
     * @param category The [Category] being updated
     * @param newIconKey The new [com.example.mybudgettree.IconCatalog] key, or null to fall back to the name-based default
     * @return An [UpdateCategoryReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryIcon(category: Category, newIconKey: String?): UpdateCategoryReturnInfo {
        val result = run {
            if (category.iconKey == newIconKey) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.NoChange, category = category)
            if (!isCategoryStillValid(category)) return@run UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category does not exist")
            categoryDao.updateCategoryIcon(category.id, newIconKey)
            UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Succeeded, category = category.copy(iconKey = newIconKey))
        }
        logUpdateOutcome("update icon for category '${category.categoryName}'", result.status, result.errMsg)
        return result
    }

    /**
     * Deletes the category from the database
     *
     * @param category The [Category] to delete
     * @return A status reflection from [CategoryDeleteReturnStatus]
     */
    suspend fun deleteCategory(category: Category): CategoryDeleteReturnStatus {
        val status = if (categoryDao.deleteCategory(category) == 1) CategoryDeleteReturnStatus.Deleted else CategoryDeleteReturnStatus.DoesNotExist
        if (status == CategoryDeleteReturnStatus.Deleted) Log.i(TAG, "Successfully deleted category '${category.categoryName}'")
        else Log.w(TAG, "Failed to delete category '${category.categoryName}': category does not exist")
        return status
    }
}
