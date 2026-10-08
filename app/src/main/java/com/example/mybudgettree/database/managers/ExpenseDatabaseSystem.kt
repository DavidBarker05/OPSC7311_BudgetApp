package com.example.mybudgettree.database.managers

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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * This system manages expense state, validation, discovery, updates, and removals
 *
 * Expenses are stored in Firestore at `users/{uid}/expenses/{id}` and point at their category by `categoryId`. Only the
 * signed-in user's data can be reached. Dates and times are stored as ISO-8601 strings, which sort the same way as the
 * dates they represent, so they can be filtered with range queries. Combining a category or description filter with a
 * date range needs a Firestore composite index
 *
 * @property auth The Firebase Authentication instance used to find the signed-in user
 * @property db The Firestore instance that holds the expenses
 */
class ExpenseDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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
     * @param imagePath The path to the expense's receipt image on this device, or null if none is set
     * @param deviceId The ID of the device the image is saved on, required if [imagePath] is set
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createExpense(
        category: Category,
        description: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null,
        deviceId: String? = null
    ): CreateReturnInfo<Expense> {
        val result = tryCreateExpense(
            category = category,
            description = description,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            imagePath = imagePath,
            deviceId = deviceId
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
        imagePath: String? = null,
        deviceId: String? = null
    ): CreateReturnInfo<Expense> {
        if (category.id.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        if (description.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Description is blank")
        if (amount < 0.0) return CreateReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        if (endTime < startTime) return CreateReturnInfo(wasSuccessful = false, errMsg = "Start time is after end time")
        if (imagePath?.isBlank() ?: false) return CreateReturnInfo(wasSuccessful = false, errMsg = "Image path is empty")
        if (imagePath != null && deviceId.isNullOrBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Device id is empty")
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
                imagePaths = if (imagePath != null && deviceId != null) mapOf(deviceId to imagePath) else emptyMap()
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
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseDescription(expense: Expense, newDescription: String): UpdateReturnInfo<Expense> {
        val result = tryUpdateExpenseDescription(expense, newDescription)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "description for expense '${expense.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateExpenseDescription(expense: Expense, newDescription: String): UpdateReturnInfo<Expense> {
        if (newDescription.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Description is empty")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = expenses(uid),
            entityTypeDisplayName = "Expense",
            entity = expense,
            property = Expense::description,
            newValue = newDescription,
            updatedEntity = expense.copy(description = newDescription)
        )
    }

    /**
     * Modifies the amount for the expense
     *
     * @param expense The [Expense] being updated
     * @param newAmount The new amount, cannot be negative
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseAmount(expense: Expense, newAmount: Double): UpdateReturnInfo<Expense> {
        val result = tryUpdateExpenseAmount(expense, newAmount)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "amount for expense '${expense.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateExpenseAmount(expense: Expense, newAmount: Double): UpdateReturnInfo<Expense> {
        if (newAmount < 0.0) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New amount cannot be negative")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = expenses(uid),
            entityTypeDisplayName = "Expense",
            entity = expense,
            property = Expense::amount,
            newValue = newAmount,
            updatedEntity = expense.copy(amount = newAmount)
        )
    }

    /**
     * Modifies the date for the expense
     *
     * @param expense The [Expense] being updated
     * @param newDate The new date
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseDate(expense: Expense, newDate: LocalDate): UpdateReturnInfo<Expense> {
        val result = tryUpdateExpenseDate(expense, newDate)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "date for expense '${expense.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateExpenseDate(expense: Expense, newDate: LocalDate): UpdateReturnInfo<Expense> {
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = expenses(uid),
            entityTypeDisplayName = "Expense",
            entity = expense,
            property = Expense::date,
            newValue = newDate.toString(),
            updatedEntity = expense.copy(date = newDate.toString())
        )
    }

    /**
     * Modifies the start time for the expense
     *
     * @param expense The [Expense] being updated
     * @param newStartTime The new start time, cannot be after the expense's current end time
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseStartTime(expense: Expense, newStartTime: LocalTime): UpdateReturnInfo<Expense> {
        val result = tryUpdateExpenseStartTime(expense, newStartTime)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "start time for expense '${expense.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateExpenseStartTime(expense: Expense, newStartTime: LocalTime): UpdateReturnInfo<Expense> {
        if (expense.endTimeAsLocalTime() < newStartTime) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Start time cannot be after end time")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = expenses(uid),
            entityTypeDisplayName = "Expense",
            entity = expense,
            property = Expense::startTime,
            newValue = newStartTime.toString(),
            updatedEntity = expense.copy(date = newStartTime.toString())
        )
    }

    /**
     * Modifies the end time for the expense
     *
     * @param expense The [Expense] being updated
     * @param newEndTime The new end time, cannot be before the expense's current start time
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseEndTime(expense: Expense, newEndTime: LocalTime): UpdateReturnInfo<Expense> {
        val result = tryUpdateExpenseEndTime(expense, newEndTime)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "end time for expense '${expense.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateExpenseEndTime(expense: Expense, newEndTime: LocalTime): UpdateReturnInfo<Expense> {
        if (newEndTime < expense.startTimeAsLocalTime()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "End time cannot be before start time")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = expenses(uid),
            entityTypeDisplayName = "Expense",
            entity = expense,
            property = Expense::endTime,
            newValue = newEndTime.toString(),
            updatedEntity = expense.copy(date = newEndTime.toString())
        )
    }

    /**
     * Modifies the receipt image path one device saved for the expense, leaving the other devices' paths untouched
     *
     * @param expense The [Expense] being updated
     * @param deviceId The ID of the device the image is saved on
     * @param newImagePath The new local image path, or null to remove this device's image
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateExpenseImage(expense: Expense, deviceId: String, newImagePath: String?): UpdateReturnInfo<Expense> {
        val result = tryUpdateExpenseImage(expense, deviceId, newImagePath)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "image for expense '${expense.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateExpenseImage(expense: Expense, deviceId: String, newImagePath: String?): UpdateReturnInfo<Expense> {
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDeviceImagePath(
            collection = expenses(uid),
            entityTypeDisplayName = "Expense",
            entity = expense,
            id = expense.id,
            currentPaths = expense.imagePaths,
            deviceId = deviceId,
            newPath = newImagePath,
            withPaths = { expense.copy(imagePaths = it) }
        )
    }

    /**
     * Deletes the expense from the database
     *
     * @param expense The [Expense] to delete
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteExpense(expense: Expense): DeleteReturnStatus {
        val uid = auth.uid
        val result =
            if (uid == null) DeleteReturnStatus.ReauthenticationFailed
            else deleteDocument(
                db = db,
                collection = expenses(uid),
                id = expense.id,
                subcollections = emptyList(),
                relatedCollections = emptyList(),
                batchSize = 0L
            )
        val wasSuccessful = result == DeleteReturnStatus.Deleted
        val errMsg: String? =
            if (wasSuccessful) null
            else when (result) {
                DeleteReturnStatus.DoesNotExist -> "Category does not exist"
                DeleteReturnStatus.ReauthenticationFailed -> "No user currently signed in"
                else -> "Unknown reason"
            }
        logOutcome(
            tag = TAG,
            wasSuccessful = wasSuccessful,
            verbOnSuccess = "deleted",
            verbOnFailure = "to delete",
            messageDetails = "expense '${expense.id}'",
            errMsg = errMsg
        )
        return result
    }
}
