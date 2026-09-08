package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.ExpenseDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import java.time.LocalDate
import java.time.LocalTime

/**
 * This system manages expense state, validation, discovery, updates, and removals
 *
 * @property expenseDao The underlying Data Access Object managing RoomDB operations
 * @property userDatabaseSystem Used to validate that the owning user exists before user-scoped expense operations proceed
 * @property categoryDatabaseSystem Used to validate that the owning category exists before expense operations proceed
 */
class ExpenseDatabaseSystem(
    private val expenseDao: ExpenseDao,
    private val userDatabaseSystem: UserDatabaseSystem,
    private val categoryDatabaseSystem: CategoryDatabaseSystem
) {

    /**
     * Wraps the expense creation return in a detailed form
     *
     * @property wasSuccessful True if the expense was created without error, false otherwise
     * @property expense The newly created [Expense] record if successful, or null on execution failure
     * @property errMsg The explanatory message detailing why creation failed, or null if successful
     */
    data class CreateExpenseReturnInfo(
        val wasSuccessful: Boolean,
        val expense: Expense? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the search request return in a detailed form
     *
     * @property wasSuccessful True if the target record was found, false otherwise
     * @property expense The retrieved [Expense] entity if located, or null if the record doesn't exist
     * @property errMsg The diagnostic message stating the cause of failure, or null if found
     */
    data class FindExpenseReturnInfo(
        val wasSuccessful: Boolean,
        val expense: Expense? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the bulk retrieval return in a detailed form
     *
     * @property wasSuccessful True if the expenses were retrieved without error, false otherwise
     * @property expenses The retrieved list of [Expense] entities if successful, or null on execution failure
     * @property errMsg The diagnostic message stating the cause of failure, or null if successful
     */
    data class RetrieveExpensesReturnInfo(
        val wasSuccessful: Boolean,
        val expenses: List<Expense>? = null,
        val errMsg: String? = null
    )

    /**
     * Identifies exactly what happened when updating an expense
     */
    enum class UpdateExpenseReturnStatus {
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
     * @property status A [UpdateExpenseReturnStatus] specifying the operation outcome
     * @property expense The modified [Expense] record containing updated fields, or null if the task failed
     * @property errMsg The error message, only set if the [status] is [UpdateExpenseReturnStatus.Failed]
     */
    data class UpdateExpenseReturnInfo(
        val status: UpdateExpenseReturnStatus,
        val expense: Expense? = null,
        val errMsg: String? = null
    )

    /**
     * Indicates what happened when trying to delete an expense
     */
    enum class ExpenseDeleteReturnStatus {
        /**
         * The expense couldn't be deleted because it doesn't exist in the database
         */
        DoesNotExist,
        /**
         * The expense was successfully deleted
         */
        Deleted
    }

    /**
     * Creates a new expense for the category after validating the fields
     *
     * @param category The [Category] the expense belongs to
     * @param description The expense's name
     * @param currencyAtTime The currency the amount was denominated in at the time of the expense
     * @param amount The expense amount, cannot be negative
     * @param date The date the expense occurred on
     * @param startTime The time the expense started
     * @param endTime The time the expense ended, cannot be before [startTime]
     * @param imagePath The path to the expense's receipt image, or null if none is set
     * @return A [CreateExpenseReturnInfo] indicating what happened with the creation
     */
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

    /**
     * Find the expense in the database if it exists
     *
     * @param expenseId The id to search for
     * @return A [FindExpenseReturnInfo] indicating what happened with the search
     */
    suspend fun findExpense(expenseId: Long): FindExpenseReturnInfo {
        val expense = expenseDao.findExpense(expenseId) ?: return FindExpenseReturnInfo(wasSuccessful = false, errMsg = "Expense does not exist")
        return FindExpenseReturnInfo(wasSuccessful = true, expense = expense)
    }

    /**
     * Check if the expense still exists in the database
     *
     * @param expense The [Expense] to validate
     * @return True if the expense still exists, false otherwise
     */
    suspend fun isExpenseStillValid(expense: Expense): Boolean = findExpense(expense.id).wasSuccessful

    /**
     * Retrieves every expense belonging to the user
     *
     * @param user The [User] to retrieve expenses for
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpenses(user: User): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpenses(user.username)
        )
    }

    /**
     * Retrieves every expense belonging to the user with a matching description
     *
     * @param user The [User] to retrieve expenses for
     * @param description The description to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesByDescription(user: User, description: String): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescription(user.username, description)
        )
    }

    /**
     * Retrieves every expense belonging to the user that occurred on the given date
     *
     * @param user The [User] to retrieve expenses for
     * @param date The date to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesOnDate(user: User, date: LocalDate): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesOnDate(user.username, date)
        )
    }

    /**
     * Retrieves every expense belonging to the user that occurred on the given date with a matching description
     *
     * @param user The [User] to retrieve expenses for
     * @param date The date to filter by
     * @param description The description to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesByDescriptionOnDate(user: User, date: LocalDate, description: String): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionOnDate(user.username, date, description)
        )
    }

    /**
     * Retrieves every expense belonging to the user that occurred between the given dates, inclusive
     *
     * @param user The [User] to retrieve expenses for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesBetweenDates(user: User, startDate: LocalDate, endDate: LocalDate): RetrieveExpensesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        if (startDate > endDate) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesBetweenDates(user.username, startDate, endDate)
        )
    }

    /**
     * Retrieves every expense belonging to the user that occurred between the given dates, inclusive, with a matching description
     *
     * @param user The [User] to retrieve expenses for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @param description The description to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
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

    /**
     * Retrieves every expense belonging to the category
     *
     * @param category The [Category] to retrieve expenses for
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesForCategory(category: Category): RetrieveExpensesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesForCategory(category.id)
        )
    }

    /**
     * Retrieves every expense belonging to the category with a matching description
     *
     * @param category The [Category] to retrieve expenses for
     * @param description The description to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesByDescriptionForCategory(category: Category, description: String): RetrieveExpensesReturnInfo {
        if (description.isBlank()) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionForCategory(category.id, description)
        )
    }

    /**
     * Retrieves every expense belonging to the category that occurred on the given date
     *
     * @param category The [Category] to retrieve expenses for
     * @param date The date to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesOnDateForCategory(category: Category, date: LocalDate): RetrieveExpensesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesOnDateForCategory(category.id, date)
        )
    }

    /**
     * Retrieves every expense belonging to the category that occurred on the given date with a matching description
     *
     * @param category The [Category] to retrieve expenses for
     * @param description The description to filter by
     * @param date The date to filter by
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesByDescriptionOnDateForCategory(category: Category, description: String, date: LocalDate): RetrieveExpensesReturnInfo {
        if (description.isBlank()) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesByDescriptionOnDateForCategory(category.id, description, date)
        )
    }

    /**
     * Retrieves every expense belonging to the category that occurred between the given dates, inclusive
     *
     * @param category The [Category] to retrieve expenses for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesBetweenDatesForCategory(category: Category, startDate: LocalDate, endDate: LocalDate): RetrieveExpensesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (startDate > endDate) return RetrieveExpensesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveExpensesReturnInfo(
            wasSuccessful = true,
            expenses = expenseDao.retrieveAllExpensesBetweenDatesForCategory(category.id, startDate, endDate)
        )
    }

    /**
     * Retrieves every expense belonging to the category that occurred between the given dates, inclusive, with a matching description
     *
     * @param category The [Category] to retrieve expenses for
     * @param description The description to filter by
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [RetrieveExpensesReturnInfo] indicating what happened with the retrieval
     */
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

    /**
     * Modifies the description for the expense
     *
     * @param expense The [Expense] being updated
     * @param newDescription The new description
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseDescription(expense: Expense, newDescription: String): UpdateExpenseReturnInfo {
        if (expense.description == newDescription) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newDescription.isBlank()) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Description is empty")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseDescription(expense.id, newDescription)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(description = newDescription))
    }

    /**
     * Modifies the amount for the expense
     *
     * @param expense The [Expense] being updated
     * @param newAmount The new amount, cannot be negative
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseAmount(expense: Expense, newAmount: Double): UpdateExpenseReturnInfo {
        if (expense.amount == newAmount) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newAmount < 0.0) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseAmount(expense.id, newAmount)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(amount = newAmount))
    }

    /**
     * Modifies the date for the expense
     *
     * @param expense The [Expense] being updated
     * @param newDate The new date
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseDate(expense: Expense, newDate: LocalDate): UpdateExpenseReturnInfo {
        if (expense.date == newDate) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseDate(expense.id, newDate)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(date = newDate))
    }

    /**
     * Modifies the start time for the expense
     *
     * @param expense The [Expense] being updated
     * @param newStartTime The new start time, cannot be after the expense's current end time
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseStartTime(expense: Expense, newStartTime: LocalTime): UpdateExpenseReturnInfo {
        if (expense.startTime == newStartTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (expense.endTime < newStartTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Start time is after end time")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseStartTime(expense.id, newStartTime)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(startTime = newStartTime))
    }

    /**
     * Modifies the end time for the expense
     *
     * @param expense The [Expense] being updated
     * @param newEndTime The new end time, cannot be before the expense's current start time
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseEndTime(expense: Expense, newEndTime: LocalTime): UpdateExpenseReturnInfo {
        if (expense.endTime == newEndTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newEndTime < expense.startTime) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "End time is before start time")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseEndTime(expense.id, newEndTime)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(endTime = newEndTime))
    }

    /**
     * Modifies the receipt image path for the expense
     *
     * @param expense The [Expense] being updated
     * @param newImagePath The new image path, or null to remove it
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseImage(expense: Expense, newImagePath: String?): UpdateExpenseReturnInfo {
        if (expense.imagePath == newImagePath) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
        if (newImagePath?.isBlank() ?: false) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Image path cannot be blank")
        if (!isExpenseStillValid(expense)) return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
        expenseDao.updateExpenseImage(expense.id, newImagePath)
        return UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(imagePath = newImagePath))
    }

    /**
     * Deletes the expense from the database
     *
     * @param expense The [Expense] to delete
     * @return A status reflection from [ExpenseDeleteReturnStatus]
     */
    suspend fun deleteExpense(expense: Expense): ExpenseDeleteReturnStatus = if (expenseDao.deleteExpense(expense) == 1) ExpenseDeleteReturnStatus.Deleted else ExpenseDeleteReturnStatus.DoesNotExist
}
