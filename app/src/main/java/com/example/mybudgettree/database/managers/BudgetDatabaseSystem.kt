package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.BudgetDao
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Budget

/**
 * This system manages budget state, uniqueness validation, discovery, updates, and removals
 *
 * @property budgetDao The underlying Data Access Object managing RoomDB operations
 * @property categoryDatabaseSystem Used to validate that the owning category exists before budget operations proceed
 */
class BudgetDatabaseSystem(
    private val budgetDao: BudgetDao,
    private val categoryDatabaseSystem: CategoryDatabaseSystem
) {

    /**
     * Wraps the budget creation return in a detailed form
     *
     * @property wasSuccessful True if the budget was created without error, false otherwise
     * @property budget The newly created [Budget] record if successful, or null on execution failure
     * @property errMsg The explanatory message detailing why creation failed, or null if successful
     */
    data class CreateBudgetReturnInfo(
        val wasSuccessful: Boolean,
        val budget: Budget? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the search request return in a detailed form
     *
     * @property wasSuccessful True if the target record was found, false otherwise
     * @property budget The retrieved [Budget] entity if located, or null if the record doesn't exist
     * @property errMsg The diagnostic message stating the cause of failure, or null if found
     */
    data class FindBudgetReturnInfo(
        val wasSuccessful: Boolean,
        val budget: Budget? = null,
        val errMsg: String? = null
    )

    /**
     * Identifies exactly what happened when updating a budget
     */
    enum class UpdateBudgetReturnStatus {
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
     * @property status A [UpdateBudgetReturnStatus] specifying the operation outcome
     * @property budget The modified [Budget] record containing updated fields, or null if the task failed
     * @property errMsg The error message, only set if the [status] is [UpdateBudgetReturnStatus.Failed]
     */
    data class UpdateBudgetReturnInfo(
        val status: UpdateBudgetReturnStatus,
        val budget: Budget? = null,
        val errMsg: String? = null
    )

    /**
     * Indicates what happened when trying to delete a budget
     */
    enum class BudgetDeleteReturnStatus {
        /**
         * The budget couldn't be deleted because it doesn't exist in the database
         */
        DoesNotExist,
        /**
         * The budget was successfully deleted
         */
        Deleted
    }

    /**
     * Creates a new budget for the category after validating the fields and confirming the category doesn't already have one
     *
     * @param category The [Category] the budget belongs to, must not already have a budget
     * @param currency The currency the budget amount is denominated in
     * @param amount The budgeted amount, cannot be negative
     * @return A [CreateBudgetReturnInfo] indicating what happened with the creation
     */
    suspend fun createBudget(category: Category, currency: String, amount: Double): CreateBudgetReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (currency.isBlank()) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = "Currency type is blank")
        if (amount < 0.0) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        val budget = Budget(categoryId = category.id, currency = currency, amount = amount)
        val id = budgetDao.insertBudget(budget)
        if (id == -1L) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = "Category already has a budget")
        return CreateBudgetReturnInfo(wasSuccessful = true, budget = budget.copy(id = id))
    }

    /**
     * Find the budget in the database if it exists
     *
     * @param budgetId The id to search for
     * @return A [FindBudgetReturnInfo] indicating what happened with the search
     */
    suspend fun findBudget(budgetId: Long): FindBudgetReturnInfo {
        val budget = budgetDao.findBudget(budgetId) ?: return FindBudgetReturnInfo(wasSuccessful = false, errMsg = "Budget does not exist")
        return FindBudgetReturnInfo(wasSuccessful = true, budget = budget)
    }

    /**
     * Find the category's budget if it exists
     *
     * @param category The [Category] to search for a budget on
     * @return A [FindBudgetReturnInfo] indicating what happened with the search
     */
    suspend fun findBudgetForCategory(category: Category): FindBudgetReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return FindBudgetReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        val budget = budgetDao.findBudgetForCategory(category.id) ?: return FindBudgetReturnInfo(wasSuccessful = false, errMsg = "Category does not have a budget")
        return FindBudgetReturnInfo(wasSuccessful = true, budget = budget)
    }

    /**
     * Check if the budget still exists in the database
     *
     * @param budget The [Budget] to validate
     * @return True if the budget still exists, false otherwise
     */
    suspend fun isBudgetStillValid(budget: Budget): Boolean = findBudget(budget.id).wasSuccessful

    /**
     * Modifies the amount for the budget
     *
     * @param budget The [Budget] being updated
     * @param newAmount The new budgeted amount, cannot be negative
     * @return An [UpdateBudgetReturnInfo] indicating what happened with the update
     */
    suspend fun updateBudgetAmount(budget: Budget, newAmount: Double): UpdateBudgetReturnInfo {
        if (budget.amount == newAmount) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.NoChange, budget = budget)
        if (newAmount < 0.0) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
        if (!isBudgetStillValid(budget)) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "Budget is not valid")
        budgetDao.updateBudgetAmount(budget.id, newAmount)
        return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Succeeded, budget = budget.copy(amount = newAmount))
    }

    /**
     * Modifies the currency for the budget
     *
     * @param budget The [Budget] being updated
     * @param newCurrency The new currency
     * @return An [UpdateBudgetReturnInfo] indicating what happened with the update
     */
    suspend fun updateBudgetCurrency(budget: Budget, newCurrency: String): UpdateBudgetReturnInfo {
        if (budget.currency == newCurrency) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.NoChange, budget = budget)
        if (newCurrency.isBlank()) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "Currency type is blank")
        if (!isBudgetStillValid(budget)) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "Budget is not valid")
        budgetDao.updateBudgetCurrency(budget.id, newCurrency)
        return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Succeeded, budget = budget.copy(currency = newCurrency))
    }

    /**
     * Deletes the budget from the database
     *
     * @param budget The [Budget] to delete
     * @return A status reflection from [BudgetDeleteReturnStatus]
     */
    suspend fun deleteBudget(budget: Budget): BudgetDeleteReturnStatus = if (budgetDao.deleteBudget(budget) == 1) BudgetDeleteReturnStatus.Deleted else BudgetDeleteReturnStatus.DoesNotExist
}
