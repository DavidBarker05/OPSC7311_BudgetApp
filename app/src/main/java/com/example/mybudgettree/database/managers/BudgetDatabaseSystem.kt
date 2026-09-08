package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.BudgetDao
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Budget

class BudgetDatabaseSystem(
    private val budgetDao: BudgetDao,
    private val categoryDatabaseSystem: CategoryDatabaseSystem
) {

    data class CreateBudgetReturnInfo(
        val wasSuccessful: Boolean,
        val budget: Budget? = null,
        val errMsg: String? = null
    )

    data class FindBudgetReturnInfo(
        val wasSuccessful: Boolean,
        val budget: Budget? = null,
        val errMsg: String? = null
    )

    enum class UpdateBudgetReturnStatus {
        Failed,
        NoChange,
        Succeeded
    }

    data class UpdateBudgetReturnInfo(
        val status: UpdateBudgetReturnStatus,
        val budget: Budget? = null,
        val errMsg: String? = null
    )

    enum class BudgetDeleteReturnStatus {
        DoesNotExist,
        Deleted
    }

    suspend fun createBudget(category: Category, currency: String, amount: Double): CreateBudgetReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (currency.isBlank()) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = "Currency type is blank")
        if (amount < 0.0) return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        val budget = Budget(categoryId = category.id, currency = currency, amount = amount)
        val id = budgetDao.insertBudget(budget) ?: return CreateBudgetReturnInfo(wasSuccessful = false, errMsg = "Category already has a budget")
        return CreateBudgetReturnInfo(wasSuccessful = true, budget = budget.copy(id = id))
    }

    suspend fun findBudget(budgetId: Long): FindBudgetReturnInfo {
        val budget = budgetDao.findBudget(budgetId) ?: return FindBudgetReturnInfo(wasSuccessful = false, errMsg = "Budget does not exist")
        return FindBudgetReturnInfo(wasSuccessful = true, budget = budget)
    }

    suspend fun findBudgetForCategory(category: Category): FindBudgetReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return FindBudgetReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        val budget = budgetDao.findBudgetForCategory(category.id) ?: return FindBudgetReturnInfo(wasSuccessful = false, errMsg = "Category does not have a budget")
        return FindBudgetReturnInfo(wasSuccessful = true, budget = budget)
    }

    suspend fun isBudgetStillValid(budget: Budget): Boolean = findBudget(budget.id).wasSuccessful

    suspend fun updateBudgetAmount(budget: Budget, newAmount: Double): UpdateBudgetReturnInfo {
        if (budget.amount == newAmount) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.NoChange, budget = budget)
        if (newAmount < 0.0) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
        if (!isBudgetStillValid(budget)) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "Budget is not valid")
        budgetDao.updateBudgetAmount(budget.id, newAmount)
        return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Succeeded, budget = budget.copy(amount = newAmount))
    }

    suspend fun updateBudgetCurrency(budget: Budget, newCurrency: String): UpdateBudgetReturnInfo {
        if (budget.currency == newCurrency) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.NoChange, budget = budget)
        if (newCurrency.isBlank()) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "Currency type is blank")
        if (!isBudgetStillValid(budget)) return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Failed, errMsg = "Budget is not valid")
        budgetDao.updateBudgetCurrency(budget.id, newCurrency)
        return UpdateBudgetReturnInfo(status = UpdateBudgetReturnStatus.Succeeded, budget = budget.copy(currency = newCurrency))
    }

    suspend fun deleteBudget(budget: Budget): BudgetDeleteReturnStatus = if (budgetDao.deleteBudget(budget) == 1) BudgetDeleteReturnStatus.Deleted else BudgetDeleteReturnStatus.DoesNotExist
}
