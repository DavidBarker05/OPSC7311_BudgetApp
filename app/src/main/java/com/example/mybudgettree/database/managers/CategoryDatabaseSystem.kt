package com.example.mybudgettree.database.managers

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
        if (categoryName.isBlank()) return CreateCategoryReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return CreateCategoryReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        val category = Category(username = user.username, categoryName = categoryName)
        val id = categoryDao.insertCategory(category)
        if (id == -1L) return CreateCategoryReturnInfo(wasSuccessful = false, errMsg = "User already has a category with name \"$categoryName\"")
        return CreateCategoryReturnInfo(wasSuccessful = true, category = category.copy(id = id))
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
        if (newCategoryName.isBlank()) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category name is empty")
        if (category.categoryName == newCategoryName) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.NoChange, category = category)
        val userStatus = userDatabaseSystem.findUser(category.username)
        if (!userStatus.wasSuccessful) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = userStatus.errMsg)
        categoryDao.findCategory(category.username, category.categoryName) ?: return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category does not exist")
        if (categoryDao.findCategory(category.username, newCategoryName) != null) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category name already in use")
        categoryDao.updateCategoryName(category.id, newCategoryName)
        return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Succeeded, category = category.copy(categoryName = newCategoryName))
    }

    /**
     * Modifies the budget for the category
     *
     * @param category The [Category] being updated
     * @param newBudgetAmount The new budgeted amount, or null to remove the budget, cannot be negative
     * @return An [UpdateCategoryReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryBudget(category: Category, newBudgetAmount: Double?): UpdateCategoryReturnInfo {
        if (category.budgetAmount == newBudgetAmount) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.NoChange, category = category)
        if (newBudgetAmount != null && newBudgetAmount < 0.0) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Budget amount cannot be negative")
        if (!isCategoryStillValid(category)) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category does not exist")
        categoryDao.updateCategoryBudget(category.id, newBudgetAmount)
        return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Succeeded, category = category.copy(budgetAmount = newBudgetAmount))
    }

    /**
     * Deletes the category from the database
     *
     * @param category The [Category] to delete
     * @return A status reflection from [CategoryDeleteReturnStatus]
     */
    suspend fun deleteCategory(category: Category): CategoryDeleteReturnStatus = if (categoryDao.deleteCategory(category) == 1) CategoryDeleteReturnStatus.Deleted else CategoryDeleteReturnStatus.DoesNotExist
}
