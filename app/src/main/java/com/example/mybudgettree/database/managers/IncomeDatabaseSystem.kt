package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.IncomeDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Income
import java.time.LocalDate
import java.time.LocalTime

/**
 * This system manages income state, validation, discovery, updates, and removals
 *
 * @property incomeDao The underlying Data Access Object managing RoomDB operations
 * @property userDatabaseSystem Used to validate that the owning user exists before user-scoped income operations proceed
 * @property categoryDatabaseSystem Used to validate that the owning category exists before income operations proceed
 */
class IncomeDatabaseSystem(
    private val incomeDao: IncomeDao,
    private val userDatabaseSystem: UserDatabaseSystem,
    private val categoryDatabaseSystem: CategoryDatabaseSystem
) {

    /**
     * Wraps the income creation return in a detailed form
     *
     * @property wasSuccessful True if the income was created without error, false otherwise
     * @property income The newly created [Income] record if successful, or null on execution failure
     * @property errMsg The explanatory message detailing why creation failed, or null if successful
     */
    data class CreateIncomeReturnInfo(
        val wasSuccessful: Boolean,
        val income: Income? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the search request return in a detailed form
     *
     * @property wasSuccessful True if the target record was found, false otherwise
     * @property income The retrieved [Income] entity if located, or null if the record doesn't exist
     * @property errMsg The diagnostic message stating the cause of failure, or null if found
     */
    data class FindIncomeReturnInfo(
        val wasSuccessful: Boolean,
        val income: Income? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the bulk retrieval return in a detailed form
     *
     * @property wasSuccessful True if the incomes were retrieved without error, false otherwise
     * @property incomes The retrieved list of [Income] entities if successful, or null on execution failure
     * @property errMsg The diagnostic message stating the cause of failure, or null if successful
     */
    data class RetrieveIncomesReturnInfo(
        val wasSuccessful: Boolean,
        val incomes: List<Income>? = null,
        val errMsg: String? = null
    )

    /**
     * Identifies exactly what happened when updating an income
     */
    enum class UpdateIncomeReturnStatus {
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
     * @property status A [UpdateIncomeReturnStatus] specifying the operation outcome
     * @property income The modified [Income] record containing updated fields, or null if the task failed
     * @property errMsg The error message, only set if the [status] is [UpdateIncomeReturnStatus.Failed]
     */
    data class UpdateIncomeReturnInfo(
        val status: UpdateIncomeReturnStatus,
        val income: Income? = null,
        val errMsg: String? = null
    )

    /**
     * Indicates what happened when trying to delete an income
     */
    enum class IncomeDeleteReturnStatus {
        /**
         * The income couldn't be deleted because it doesn't exist in the database
         */
        DoesNotExist,
        /**
         * The income was successfully deleted
         */
        Deleted
    }

    /**
     * Creates a new income for the category after validating the fields
     *
     * @param category The [Category] the income belongs to
     * @param description The income's name
     * @param currencyAtTime The currency the amount was denominated in at the time of the income
     * @param amount The income amount, cannot be negative
     * @param date The date the income occurred on
     * @param startTime The time the income started
     * @param endTime The time the income ended, cannot be before [startTime]
     * @param imagePath The path to the income's proof image, or null if none is set
     * @return A [CreateIncomeReturnInfo] indicating what happened with the creation
     */
    suspend fun createIncome(
        category: Category,
        description: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null
    ): CreateIncomeReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (description.isBlank()) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Description is blank")
        if (amount < 0.0) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        if (endTime < startTime) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Start time is after end time")
        if (imagePath?.isBlank() ?: false) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Image path is empty")
        val income = Income(
            categoryId = category.id,
            description = description,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            imagePath = imagePath
        )
        val id = incomeDao.insertIncome(income)
        return CreateIncomeReturnInfo(wasSuccessful = true, income = income.copy(id = id))
    }

    /**
     * Find the income in the database if it exists
     *
     * @param incomeId The id to search for
     * @return A [FindIncomeReturnInfo] indicating what happened with the search
     */
    suspend fun findIncome(incomeId: Long): FindIncomeReturnInfo {
        val income = incomeDao.findIncome(incomeId) ?: return FindIncomeReturnInfo(wasSuccessful = false, errMsg = "Income does not exist")
        return FindIncomeReturnInfo(wasSuccessful = true, income = income)
    }

    /**
     * Check if the income still exists in the database
     *
     * @param income The [Income] to validate
     * @return True if the income still exists, false otherwise
     */
    suspend fun isIncomeStillValid(income: Income): Boolean = findIncome(income.id).wasSuccessful

    /**
     * Retrieves every income belonging to the user
     *
     * @param user The [User] to retrieve incomes for
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomes(user: User): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomes(user.username)
        )
    }

    /**
     * Retrieves every income belonging to the user with a matching description
     *
     * @param user The [User] to retrieve incomes for
     * @param description The description to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesByDescription(user: User, description: String): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescription(user.username, description)
        )
    }

    /**
     * Retrieves every income belonging to the user that occurred on the given date
     *
     * @param user The [User] to retrieve incomes for
     * @param date The date to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesOnDate(user: User, date: LocalDate): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesOnDate(user.username, date)
        )
    }

    /**
     * Retrieves every income belonging to the user that occurred on the given date with a matching description
     *
     * @param user The [User] to retrieve incomes for
     * @param date The date to filter by
     * @param description The description to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesByDescriptionOnDate(user: User, date: LocalDate, description: String): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionOnDate(user.username, date, description)
        )
    }

    /**
     * Retrieves every income belonging to the user that occurred between the given dates, inclusive
     *
     * @param user The [User] to retrieve incomes for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesBetweenDates(user: User, startDate: LocalDate, endDate: LocalDate): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        if (startDate > endDate) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesBetweenDates(user.username, startDate, endDate)
        )
    }

    /**
     * Retrieves every income belonging to the user that occurred between the given dates, inclusive, with a matching description
     *
     * @param user The [User] to retrieve incomes for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @param description The description to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesByDescriptionBetweenDates(
        user: User,
        startDate: LocalDate,
        endDate: LocalDate,
        description: String
    ): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        if (startDate > endDate) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionBetweenDates(
                user.username,
                startDate,
                endDate,
                description
            )
        )
    }

    /**
     * Retrieves every income belonging to the category
     *
     * @param category The [Category] to retrieve incomes for
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesForCategory(category: Category): RetrieveIncomesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesForCategory(category.id)
        )
    }

    /**
     * Retrieves every income belonging to the category with a matching description
     *
     * @param category The [Category] to retrieve incomes for
     * @param description The description to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesByDescriptionForCategory(category: Category, description: String): RetrieveIncomesReturnInfo {
        if (description.isBlank()) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionForCategory(category.id, description)
        )
    }

    /**
     * Retrieves every income belonging to the category that occurred on the given date
     *
     * @param category The [Category] to retrieve incomes for
     * @param date The date to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesOnDateForCategory(category: Category, date: LocalDate): RetrieveIncomesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesOnDateForCategory(category.id, date)
        )
    }

    /**
     * Retrieves every income belonging to the category that occurred on the given date with a matching description
     *
     * @param category The [Category] to retrieve incomes for
     * @param description The description to filter by
     * @param date The date to filter by
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesByDescriptionOnDateForCategory(category: Category, description: String, date: LocalDate): RetrieveIncomesReturnInfo {
        if (description.isBlank()) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionOnDateForCategory(category.id, description, date)
        )
    }

    /**
     * Retrieves every income belonging to the category that occurred between the given dates, inclusive
     *
     * @param category The [Category] to retrieve incomes for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesBetweenDatesForCategory(category: Category, startDate: LocalDate, endDate: LocalDate): RetrieveIncomesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (startDate > endDate) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesBetweenDatesForCategory(category.id, startDate, endDate)
        )
    }

    /**
     * Retrieves every income belonging to the category that occurred between the given dates, inclusive, with a matching description
     *
     * @param category The [Category] to retrieve incomes for
     * @param description The description to filter by
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [RetrieveIncomesReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesByDescriptionBetweenDatesForCategory(
        category: Category,
        description: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): RetrieveIncomesReturnInfo {
        if (description.isBlank()) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (startDate > endDate) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionBetweenDatesForCategory(
                category.id,
                description,
                startDate,
                endDate
            )
        )
    }

    /**
     * Modifies the description for the income
     *
     * @param income The [Income] being updated
     * @param newDescription The new description
     * @return An [UpdateIncomeReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeDescription(income: Income, newDescription: String): UpdateIncomeReturnInfo {
        if (income.description == newDescription) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newDescription.isBlank()) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Description is empty")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeDescription(income.id, newDescription)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(description = newDescription))
    }

    /**
     * Modifies the amount for the income
     *
     * @param income The [Income] being updated
     * @param newAmount The new amount, cannot be negative
     * @return An [UpdateIncomeReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeAmount(income: Income, newAmount: Double): UpdateIncomeReturnInfo {
        if (income.amount == newAmount) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newAmount < 0.0) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeAmount(income.id, newAmount)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(amount = newAmount))
    }

    /**
     * Modifies the date for the income
     *
     * @param income The [Income] being updated
     * @param newDate The new date
     * @return An [UpdateIncomeReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeDate(income: Income, newDate: LocalDate): UpdateIncomeReturnInfo {
        if (income.date == newDate) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeDate(income.id, newDate)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(date = newDate))
    }

    /**
     * Modifies the start time for the income
     *
     * @param income The [Income] being updated
     * @param newStartTime The new start time, cannot be after the income's current end time
     * @return An [UpdateIncomeReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeStartTime(income: Income, newStartTime: LocalTime): UpdateIncomeReturnInfo {
        if (income.startTime == newStartTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (income.endTime < newStartTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Start time is after end time")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeStartTime(income.id, newStartTime)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(startTime = newStartTime))
    }

    /**
     * Modifies the end time for the income
     *
     * @param income The [Income] being updated
     * @param newEndTime The new end time, cannot be before the income's current start time
     * @return An [UpdateIncomeReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeEndTime(income: Income, newEndTime: LocalTime): UpdateIncomeReturnInfo {
        if (income.endTime == newEndTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newEndTime < income.startTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "End time is before start time")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeEndTime(income.id, newEndTime)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(endTime = newEndTime))
    }

    /**
     * Modifies the proof image path for the income
     *
     * @param income The [Income] being updated
     * @param newImagePath The new image path, or null to remove it
     * @return An [UpdateIncomeReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeImage(income: Income, newImagePath: String?): UpdateIncomeReturnInfo {
        if (income.imagePath == newImagePath) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newImagePath?.isBlank() ?: false) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Image path cannot be blank")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeImage(income.id, newImagePath)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(imagePath = newImagePath))
    }

    /**
     * Deletes the income from the database
     *
     * @param income The [Income] to delete
     * @return A status reflection from [IncomeDeleteReturnStatus]
     */
    suspend fun deleteIncome(income: Income): IncomeDeleteReturnStatus = if (incomeDao.deleteIncome(income) == 1) IncomeDeleteReturnStatus.Deleted else IncomeDeleteReturnStatus.DoesNotExist
}
