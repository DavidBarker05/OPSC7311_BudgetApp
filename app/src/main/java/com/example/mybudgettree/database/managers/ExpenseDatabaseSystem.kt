package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.ExpenseDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import java.time.LocalDate
import java.time.LocalTime

class ExpenseDatabaseSystem(
    private val expenseDao: ExpenseDao,
    private val userDatabaseSystem: UserDatabaseSystem,
    private val categoryDatabaseSystem: CategoryDatabaseSystem
) {

    data class CreateExpenseReturnInfo(
        val wasSuccessful: Boolean,
        val expense: Expense? = null,
        val errMsg: String? = null
    )

    data class FindExpenseReturnInfo(
        val wasSuccessful: Boolean,
        val expense: Expense? = null,
        val errMsg: String? = null
    )

    data class RetrieveExpensesReturnInfo(
        val wasSuccessful: Boolean,
        val expenses: List<Expense>? = null,
        val errMsg: String? = null
    )

    enum class UpdateExpenseReturnStatus {
        Failed,
        NoChange,
        Succeeded
    }

    data class UpdateExpenseReturnInfo(
        val status: UpdateExpenseReturnStatus,
        val expense: Expense? = null,
        val errMsg: String? = null
    )

    enum class ExpenseDeleteReturnStatus {
        DoesNotExist,
        Deleted
    }

    suspend fun createExpense(
        category: Category,
        description: String,
        currencyAtTime: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null
    ): CreateExpenseReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return CreateExpenseReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (description.isBlank()) return CreateExpenseReturnInfo(wasSuccessful = false, errMsg = "Description is blank")
        if (currencyAtTime.isBlank()) return CreateExpenseReturnInfo(wasSuccessful = false, errMsg = "Currency type is blank")
        if (amount < 0.0) return CreateExpenseReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        if (endTime < startTime) return CreateExpenseReturnInfo(wasSuccessful = false, errMsg = "Start time is after end time")
        if (imagePath?.isBlank() ?: false) return CreateExpenseReturnInfo(wasSuccessful = false, errMsg = "Image path is empty")
        val expense = Expense(
            categoryId = category.id,
            description = description,
            currencyAtTime = currencyAtTime,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            imagePath = imagePath
        )
        val id = expenseDao.insertExpense(expense)
        return CreateExpenseReturnInfo(wasSuccessful = true, expense = expense.copy(id = id))
    }

    suspend fun findExpense(expenseId: Long): FindExpenseReturnInfo {
        val expense = expenseDao.findExpense(expenseId) ?: return FindExpenseReturnInfo(wasSuccessful = false, errMsg = "Expense does not exist")
        return FindExpenseReturnInfo(wasSuccessful = true, expense = expense)
    }

    suspend fun isExpenseStillValid(expense: Expense): Boolean = findExpense(expense.id).wasSuccessful

    suspend fun retrieveAllExpenses(user: User): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpenses(user.username)
        )
    }

    suspend fun retrieveAllExpensesByDescription(user: User, description: String): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescription(user.username, description)
        )
    }

    suspend fun retrieveAllExpensesOnDate(user: User, date: LocalDate): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesOnDate(user.username, date)
        )
    }

    suspend fun retrieveAllExpensesByDescriptionOnDate(user: User, date: LocalDate, description: String): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionOnDate(user.username, date, description)
        )
    }

    suspend fun retrieveAllExpensesBetweenDates(user: User, startDate: LocalDate, endDate: LocalDate): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        if (startDate > endDate) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesBetweenDates(user.username, startDate, endDate)
        )
    }

    suspend fun retrieveAllExpensesByDescriptionBetweenDates(
        user: User,
        startDate: LocalDate,
        endDate: LocalDate,
        description: String
    ): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        if (startDate > endDate) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionBetweenDates(
                user.username,
                startDate,
                endDate,
                description
            )
        )
    }

    suspend fun retrieveAllExpensesForCategory(category: Category): RetrieveExpensesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesForCategory(category.id)
        )
    }

    suspend fun retrieveAllExpensesByDescriptionForCategory(category: Category, description: String): RetrieveExpensesReturnInfo {
        if (description.isBlank()) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionForCategory(category.id, description)
        )
    }

    suspend fun retrieveAllExpensesOnDateForCategory(category: Category, date: LocalDate): RetrieveExpensesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesOnDateForCategory(category.id, date)
        )
    }

    suspend fun retrieveAllExpensesByDescriptionOnDateForCategory(category: Category, description: String, date: LocalDate): RetrieveExpensesReturnInfo {
        if (description.isBlank()) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionOnDateForCategory(category.id, description, date)
        )
    }

    suspend fun retrieveAllExpensesBetweenDatesForCategory(category: Category, startDate: LocalDate, endDate: LocalDate): RetrieveExpensesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (startDate > endDate) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesBetweenDatesForCategory(category.id, startDate, endDate)
        )
    }

    suspend fun retrieveAllExpensesByDescriptionBetweenDatesForCategory(
        category: Category,
        description: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): RetrieveExpensesReturnInfo {
        if (description.isBlank()) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (startDate > endDate) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionBetweenDatesForCategory(
                category.id,
                description,
                startDate,
                endDate
            )
        )
    }

    suspend fun updateExpenseDescription(expense: Expense, newDescription: String): UpdateExpenseReturnInfo {
        if (expense.description == newDescription) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newDescription.isBlank()) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Description is empty")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseDescription(expense.id, newDescription)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(description = newDescription))
    }

    suspend fun updateExpenseAmount(expense: Expense, newAmount: Double): UpdateExpenseReturnInfo {
        if (expense.amount == newAmount) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newAmount < 0.0) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseAmount(expense.id, newAmount)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(amount = newAmount))
    }

    suspend fun updateExpenseDate(expense: Expense, newDate: LocalDate): UpdateExpenseReturnInfo {
        if (expense.date == newDate) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseDate(expense.id, newDate)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(date = newDate))
    }

    suspend fun updateExpenseStartTime(expense: Expense, newStartTime: LocalTime): UpdateExpenseReturnInfo {
        if (expense.startTime == newStartTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (expense.endTime < newStartTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Start time is after end time")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseStartTime(expense.id, newStartTime)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(startTime = newStartTime))
    }

    suspend fun updateExpenseEndTime(expense: Expense, newEndTime: LocalTime): UpdateExpenseReturnInfo {
        if (expense.endTime == newEndTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newEndTime < expense.startTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "End time is before start time")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseEndTime(expense.id, newEndTime)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(endTime = newEndTime))
    }

    suspend fun updateExpenseImage(expense: Expense, newImagePath: String?): UpdateExpenseReturnInfo {
        if (expense.imagePath == newImagePath) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newImagePath?.isBlank() ?: false) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Image path cannot be blank")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseImage(expense.id, newImagePath)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(imagePath = newImagePath))
    }

    suspend fun deleteExpense(expense: Expense): ExpenseDeleteReturnStatus = if (expenseDao.deleteExpense(expense) == 1) ExpenseDeleteReturnStatus.Deleted else ExpenseDeleteReturnStatus.DoesNotExist
}