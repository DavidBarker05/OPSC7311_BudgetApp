package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.IncomeDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Income
import java.time.LocalDate
import java.time.LocalTime

class IncomeDatabaseSystem(
    private val incomeDao: IncomeDao,
    private val userDatabaseSystem: UserDatabaseSystem,
    private val categoryDatabaseSystem: CategoryDatabaseSystem
) {

    data class CreateIncomeReturnInfo(
        val wasSuccessful: Boolean,
        val income: Income? = null,
        val errMsg: String? = null
    )

    data class FindIncomeReturnInfo(
        val wasSuccessful: Boolean,
        val income: Income? = null,
        val errMsg: String? = null
    )

    data class RetrieveIncomesReturnInfo(
        val wasSuccessful: Boolean,
        val incomes: List<Income>? = null,
        val errMsg: String? = null
    )

    enum class UpdateIncomeReturnStatus {
        Failed,
        NoChange,
        Succeeded
    }

    data class UpdateIncomeReturnInfo(
        val status: UpdateIncomeReturnStatus,
        val income: Income? = null,
        val errMsg: String? = null
    )

    enum class IncomeDeleteReturnStatus {
        DoesNotExist,
        Deleted
    }

    suspend fun createIncome(
        category: Category,
        description: String,
        currencyAtTime: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null
    ): CreateIncomeReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (description.isBlank()) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Description is blank")
        if (currencyAtTime.isBlank()) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Currency type is blank")
        if (amount < 0.0) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        if (endTime < startTime) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Start time is after end time")
        if (imagePath?.isBlank() ?: false) return CreateIncomeReturnInfo(wasSuccessful = false, errMsg = "Image path is empty")
        val income = Income(
            categoryId = category.id,
            description = description,
            currencyAtTime = currencyAtTime,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            imagePath = imagePath
        )
        val id = incomeDao.insertIncome(income)
        return CreateIncomeReturnInfo(wasSuccessful = true, income = income.copy(id = id))
    }

    suspend fun findIncome(incomeId: Long): FindIncomeReturnInfo {
        val income = incomeDao.findIncome(incomeId) ?: return FindIncomeReturnInfo(wasSuccessful = false, errMsg = "Income does not exist")
        return FindIncomeReturnInfo(wasSuccessful = true, income = income)
    }

    suspend fun isIncomeStillValid(income: Income): Boolean = findIncome(income.id).wasSuccessful

    suspend fun retrieveAllIncomes(user: User): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomes(user.username)
        )
    }

    suspend fun retrieveAllIncomesByDescription(user: User, description: String): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescription(user.username, description)
        )
    }

    suspend fun retrieveAllIncomesOnDate(user: User, date: LocalDate): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesOnDate(user.username, date)
        )
    }

    suspend fun retrieveAllIncomesByDescriptionOnDate(user: User, date: LocalDate, description: String): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionOnDate(user.username, date, description)
        )
    }

    suspend fun retrieveAllIncomesBetweenDates(user: User, startDate: LocalDate, endDate: LocalDate): RetrieveIncomesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        if (startDate > endDate) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesBetweenDates(user.username, startDate, endDate)
        )
    }

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

    suspend fun retrieveAllIncomesForCategory(category: Category): RetrieveIncomesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesForCategory(category.id)
        )
    }

    suspend fun retrieveAllIncomesByDescriptionForCategory(category: Category, description: String): RetrieveIncomesReturnInfo {
        if (description.isBlank()) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionForCategory(category.id, description)
        )
    }

    suspend fun retrieveAllIncomesOnDateForCategory(category: Category, date: LocalDate): RetrieveIncomesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesOnDateForCategory(category.id, date)
        )
    }

    suspend fun retrieveAllIncomesByDescriptionOnDateForCategory(category: Category, description: String, date: LocalDate): RetrieveIncomesReturnInfo {
        if (description.isBlank()) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Description is empty")
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesByDescriptionOnDateForCategory(category.id, description, date)
        )
    }

    suspend fun retrieveAllIncomesBetweenDatesForCategory(category: Category, startDate: LocalDate, endDate: LocalDate): RetrieveIncomesReturnInfo {
        val categoryStatus = categoryDatabaseSystem.findCategory(category.id)
        if (!categoryStatus.wasSuccessful) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = categoryStatus.errMsg)
        if (startDate > endDate) return RetrieveIncomesReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return RetrieveIncomesReturnInfo(
            wasSuccessful = true,
            incomes = incomeDao.retrieveAllIncomesBetweenDatesForCategory(category.id, startDate, endDate)
        )
    }

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

    suspend fun updateIncomeDescription(income: Income, newDescription: String): UpdateIncomeReturnInfo {
        if (income.description == newDescription) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newDescription.isBlank()) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Description is empty")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeDescription(income.id, newDescription)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(description = newDescription))
    }

    suspend fun updateIncomeAmount(income: Income, newAmount: Double): UpdateIncomeReturnInfo {
        if (income.amount == newAmount) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newAmount < 0.0) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeAmount(income.id, newAmount)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(amount = newAmount))
    }

    suspend fun updateIncomeDate(income: Income, newDate: LocalDate): UpdateIncomeReturnInfo {
        if (income.date == newDate) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeDate(income.id, newDate)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(date = newDate))
    }

    suspend fun updateIncomeStartTime(income: Income, newStartTime: LocalTime): UpdateIncomeReturnInfo {
        if (income.startTime == newStartTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (income.endTime < newStartTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Start time is after end time")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeStartTime(income.id, newStartTime)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(startTime = newStartTime))
    }

    suspend fun updateIncomeEndTime(income: Income, newEndTime: LocalTime): UpdateIncomeReturnInfo {
        if (income.endTime == newEndTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newEndTime < income.startTime) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "End time is before start time")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeEndTime(income.id, newEndTime)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(endTime = newEndTime))
    }

    suspend fun updateIncomeImage(income: Income, newImagePath: String?): UpdateIncomeReturnInfo {
        if (income.imagePath == newImagePath) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.NoChange, income = income)
        if (newImagePath?.isBlank() ?: false) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Image path cannot be blank")
        if (!isIncomeStillValid(income)) return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Failed, errMsg = "Income is not valid")
        incomeDao.updateIncomeImage(income.id, newImagePath)
        return UpdateIncomeReturnInfo(status = UpdateIncomeReturnStatus.Succeeded, income = income.copy(imagePath = newImagePath))
    }

    suspend fun deleteIncome(income: Income): IncomeDeleteReturnStatus = if (incomeDao.deleteIncome(income) == 1) IncomeDeleteReturnStatus.Deleted else IncomeDeleteReturnStatus.DoesNotExist
}
