package com.example.mybudgettree.database.managers

import android.util.Log
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import java.time.LocalDate
import java.time.LocalTime
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * This system manages expense state, validation, discovery, updates, and removals
 *
 */
class ExpenseDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    companion object {
        private const val TAG = "ExpenseDatabaseSystem"
    }

    private fun expenses(uid: String): CollectionReference = db.collection("users").document(uid).collection("expenses")

    /**
     * Creates a new expense for the category after validating the fields
     *
     * @param category The [Category] the expense belongs to
     * @param description The expense's name
     * @param amount The expense amount, cannot be negative
     * @param date The date the expense occurred on
     * @param startTime The time the expense started
     * @param endTime The time the expense ended, cannot be before [startTime]
     * @param imagePath The path to the expense's receipt image, or null if none is set
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createExpense(
        category: Category,
        description: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null
    ): CreateReturnInfo<Expense> {
        val result = tryCreateExpense(
            category = category,
            description = description,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            imagePath = imagePath
        )
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "created",
            verbOnFailure = "to create",
            messageDetails = "expense for category '${category.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateExpense(
        category: Category,
        description: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null
    ): CreateReturnInfo<Expense> {
        if (category.id.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        if (description.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Description is blank")
        if (amount < 0.0) return CreateReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        if (endTime < startTime) return CreateReturnInfo(wasSuccessful = false, errMsg = "Start time is after end time")
        if (imagePath?.isBlank() ?: false) return CreateReturnInfo(wasSuccessful = false, errMsg = "Image path is empty")
        val uid = auth.uid ?: return CreateReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (!db .collection("users")
                .document(uid)
                .collection("categories")
                .document(category.id)
                .get()
                .await()
                .exists())
            return CreateReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
        return try {
            val expenses = expenses(uid)
            val expense = Expense(
                categoryId = category.id,
                description = description,
                amount = amount,
                date = date.toString(),
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                imagePath = imagePath
            )
            val ref = expenses.add(expense).await()
            CreateReturnInfo(wasSuccessful = true, value = expense.copy(id = ref.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create expense")
        }
    }

    /**
     * Retrieves every expense belonging to the user
     *
     * @param user The [User] to retrieve expenses for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpenses(user: User): FindAllReturnInfo<Expense> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val allExpenses = expenses(user.uid).get().await().toObjects<Expense>()
            FindAllReturnInfo(wasSuccessful = true, values = allExpenses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load expenses")
        }
    }

    /**
     * Retrieves every expense belonging to the user that occurred on the given date
     *
     * @param user The [User] to retrieve expenses for
     * @param date The date to filter by
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesOnDate(user: User, date: LocalDate): FindAllReturnInfo<Expense> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val allExpenses = expenses(user.uid)
                .whereEqualTo("date", date.toString())
                .get()
                .await()
                .toObjects<Expense>()
            FindAllReturnInfo(wasSuccessful = true, values = allExpenses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load expenses")
        }
    }

    /**
     * Retrieves every expense belonging to the user that occurred between the given dates, inclusive
     *
     * @param user The [User] to retrieve expenses for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesBetweenDates(user: User, startDate: LocalDate, endDate: LocalDate): FindAllReturnInfo<Expense> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        if (startDate > endDate) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return try {
            val allExpenses = expenses(user.uid)
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .toObjects<Expense>()
            FindAllReturnInfo(wasSuccessful = true, values = allExpenses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load expenses")
        }
    }

    /**
     * Retrieves every expense belonging to the category
     *
     * @param category The [Category] to retrieve expenses for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesForCategory(category: Category): FindAllReturnInfo<Expense> {
        if (category.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (!db .collection("users")
                .document(uid)
                .collection("categories")
                .document(category.id)
                .get()
                .await()
                .exists())
            return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
        return try {
            val allExpenses = expenses(uid)
                .whereEqualTo("categoryID", category.id)
                .get()
                .await()
                .toObjects<Expense>()
            FindAllReturnInfo(wasSuccessful = true, values = allExpenses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load expenses")
        }
    }

    /**
     * Retrieves every expense belonging to the category that occurred on the given date
     *
     * @param category The [Category] to retrieve expenses for
     * @param date The date to filter by
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesOnDateForCategory(category: Category, date: LocalDate): FindAllReturnInfo<Expense> {
        if (category.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (!db .collection("users")
                .document(uid)
                .collection("categories")
                .document(category.id)
                .get()
                .await()
                .exists())
            return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
        return try {
            val allExpenses = expenses(uid)
                .whereEqualTo("categoryID", category.id)
                .whereEqualTo("date", date.toString())
                .get()
                .await()
                .toObjects<Expense>()
            FindAllReturnInfo(wasSuccessful = true, values = allExpenses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load expenses")
        }
    }

    /**
     * Retrieves every expense belonging to the category that occurred between the given dates, inclusive
     *
     * @param category The [Category] to retrieve expenses for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllExpensesBetweenDatesForCategory(category: Category, startDate: LocalDate, endDate: LocalDate): FindAllReturnInfo<Expense> {
        if (category.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        if (startDate > endDate) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        if (!db .collection("users")
                .document(uid)
                .collection("categories")
                .document(category.id)
                .get()
                .await()
                .exists())
            return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
        return try {
            val allExpenses = expenses(uid)
                .whereEqualTo("categoryID", category.id)
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .toObjects<Expense>()
            FindAllReturnInfo(wasSuccessful = true, values = allExpenses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load expenses")
        }
    }

    /**
     * Modifies the description for the expense
     *
     * @param expense The [Expense] being updated
     * @param newDescription The new description
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseDescription(expense: Expense, newDescription: String): UpdateExpenseReturnInfo {
        val result = run {
            if (expense.description == newDescription) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
            if (newDescription.isBlank()) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Description is empty")
            if (!isExpenseStillValid(expense)) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
            expenseDao.updateExpenseDescription(expense.id, newDescription)
            UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(description = newDescription))
        }
        logUpdateOutcome("update description for expense id ${expense.id}", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the amount for the expense
     *
     * @param expense The [Expense] being updated
     * @param newAmount The new amount, cannot be negative
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseAmount(expense: Expense, newAmount: Double): UpdateExpenseReturnInfo {
        val result = run {
            if (expense.amount == newAmount) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
            if (newAmount < 0.0) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "New amount cannot be less than 0")
            if (!isExpenseStillValid(expense)) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
            expenseDao.updateExpenseAmount(expense.id, newAmount)
            UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(amount = newAmount))
        }
        logUpdateOutcome("update amount for expense id ${expense.id}", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the date for the expense
     *
     * @param expense The [Expense] being updated
     * @param newDate The new date
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseDate(expense: Expense, newDate: LocalDate): UpdateExpenseReturnInfo {
        val result = run {
            if (expense.date == newDate) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
            if (!isExpenseStillValid(expense)) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
            expenseDao.updateExpenseDate(expense.id, newDate)
            UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(date = newDate))
        }
        logUpdateOutcome("update date for expense id ${expense.id}", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the start time for the expense
     *
     * @param expense The [Expense] being updated
     * @param newStartTime The new start time, cannot be after the expense's current end time
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseStartTime(expense: Expense, newStartTime: LocalTime): UpdateExpenseReturnInfo {
        val result = run {
            if (expense.startTime == newStartTime) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
            if (expense.endTime < newStartTime) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Start time is after end time")
            if (!isExpenseStillValid(expense)) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
            expenseDao.updateExpenseStartTime(expense.id, newStartTime)
            UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(startTime = newStartTime))
        }
        logUpdateOutcome("update start time for expense id ${expense.id}", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the end time for the expense
     *
     * @param expense The [Expense] being updated
     * @param newEndTime The new end time, cannot be before the expense's current start time
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseEndTime(expense: Expense, newEndTime: LocalTime): UpdateExpenseReturnInfo {
        val result = run {
            if (expense.endTime == newEndTime) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
            if (newEndTime < expense.startTime) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "End time is before start time")
            if (!isExpenseStillValid(expense)) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
            expenseDao.updateExpenseEndTime(expense.id, newEndTime)
            UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(endTime = newEndTime))
        }
        logUpdateOutcome("update end time for expense id ${expense.id}", result.status, result.errMsg)
        return result
    }

    /**
     * Modifies the receipt image path for the expense
     *
     * @param expense The [Expense] being updated
     * @param newImagePath The new image path, or null to remove it
     * @return An [UpdateExpenseReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseImage(expense: Expense, newImagePath: String?): UpdateExpenseReturnInfo {
        val result = run {
            if (expense.imagePath == newImagePath) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.NoChange, expense = expense)
            if (newImagePath?.isBlank() ?: false) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Image path cannot be blank")
            if (!isExpenseStillValid(expense)) return@run UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Failed, errMsg = "Expense is not valid")
            expenseDao.updateExpenseImage(expense.id, newImagePath)
            UpdateExpenseReturnInfo(status = UpdateExpenseReturnStatus.Succeeded, expense = expense.copy(imagePath = newImagePath))
        }
        logUpdateOutcome("update image for expense id ${expense.id}", result.status, result.errMsg)
        return result
    }

    /**
     * Deletes the expense from the database
     *
     * @param expense The [Expense] to delete
     * @return A status reflection from [ExpenseDeleteReturnStatus]
     */
    suspend fun deleteExpense(expense: Expense): ExpenseDeleteReturnStatus {
        val status = if (expenseDao.deleteExpense(expense) == 1) ExpenseDeleteReturnStatus.Deleted else ExpenseDeleteReturnStatus.DoesNotExist
        if (status == ExpenseDeleteReturnStatus.Deleted) Log.i(TAG, "Successfully deleted expense '${expense.description}'")
        else Log.w(TAG, "Failed to delete expense '${expense.description}': expense does not exist")
        return status
    }
}
