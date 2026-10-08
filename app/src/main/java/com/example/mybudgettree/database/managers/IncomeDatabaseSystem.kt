package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import java.time.LocalDate
import java.time.LocalTime
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * This system manages income state, validation, discovery, updates, and removals
 *
 * Incomes are stored in Firestore at `users/{uid}/incomes/{id}` and point at their category by `categoryId`. Only the
 * signed-in user's data can be reached. Dates and times are stored as ISO-8601 strings, which sort the same way as the
 * dates they represent, so they can be filtered with range queries. Combining a category or description filter with a
 * date range needs a Firestore composite index
 *
 * @property auth The Firebase Authentication instance used to find the signed-in user
 * @property db The Firestore instance that holds the incomes
 */
class IncomeDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun incomes(uid: String): CollectionReference = db.collection("users").document(uid).collection("incomes")

    companion object {
        private const val TAG = "IncomeDatabaseSystem"

        private const val BATCH_SIZE = 400L
    }

    /**
     * Creates a new income for the category after validating the fields
     *
     * @param category The [Category] the income belongs to
     * @param description The income's name
     * @param amount The income amount, cannot be negative
     * @param date The date the income occurred on
     * @param startTime The time the income started
     * @param endTime The time the income ended, cannot be before [startTime]
     * @param imagePath The path to the income's proof image on this device, or null if none is set
     * @param deviceId The ID of the device the image is saved on, required if [imagePath] is set
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createIncome(
        category: Category,
        description: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null,
        deviceId: String? = null
    ): CreateReturnInfo<Income> {
        val result = tryCreateIncome(
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
            messageDetails = "income for category '${category.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateIncome(
        category: Category,
        description: String,
        amount: Double,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        imagePath: String? = null,
        deviceId: String? = null
    ): CreateReturnInfo<Income> {
        if (category.id.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        if (description.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Description is blank")
        if (amount < 0.0) return CreateReturnInfo(wasSuccessful = false, errMsg = "Amount cannot be negative")
        if (endTime < startTime) return CreateReturnInfo(wasSuccessful = false, errMsg = "Start time is after end time")
        if (imagePath?.isBlank() ?: false) return CreateReturnInfo(wasSuccessful = false, errMsg = "Image path is empty")
        if (imagePath != null && deviceId.isNullOrBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Device id is empty")
        val uid = auth.uid ?: return CreateReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            if (!db .collection("users")
                    .document(uid)
                    .collection("categories")
                    .document(category.id)
                    .get()
                    .await()
                    .exists())
                return CreateReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
            val expenses = incomes(uid)
            val expense = Income(
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
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create income")
        }
    }

    /**
     * Find the signed-in user's income by its ID if it exists
     *
     * @param incomeId The id to search for
     * @return A [FindReturnInfo] indicating what happened with the search
     */
    suspend fun findIncome(incomeId: String): FindReturnInfo<Income> {
        if (incomeId.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Income id is empty")
        val uid = auth.uid ?: return FindReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            val income = incomes(uid).document(incomeId).get().await().toObject<Income>()
            if (income != null) FindReturnInfo(wasSuccessful = true, value = income)
            else FindReturnInfo(wasSuccessful = false, errMsg = "Could not find income '${incomeId}' for current auth user")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not find income '${incomeId}' for current auth user")
        }
    }

    /**
     * Retrieves every income belonging to the user
     *
     * @param user The [User] to retrieve incomes for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomes(user: User): FindAllReturnInfo<Income> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val allIncomes = incomes(user.uid).get().await().toObjects<Income>()
            FindAllReturnInfo(wasSuccessful = true, values = allIncomes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load incomes")
        }
    }

    /**
     * Retrieves every income belonging to the user that occurred on the given date
     *
     * @param user The [User] to retrieve incomes for
     * @param date The date to filter by
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesOnDate(user: User, date: LocalDate): FindAllReturnInfo<Income> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val allIncomes = incomes(user.uid)
                .whereEqualTo("date", date.toString())
                .get()
                .await()
                .toObjects<Income>()
            FindAllReturnInfo(wasSuccessful = true, values = allIncomes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load incomes")
        }
    }

    /**
     * Retrieves every income belonging to the user that occurred between the given dates, inclusive
     *
     * @param user The [User] to retrieve incomes for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesBetweenDates(user: User, startDate: LocalDate, endDate: LocalDate): FindAllReturnInfo<Income> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        if (startDate > endDate) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        return try {
            val allIncomes = incomes(user.uid)
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .toObjects<Income>()
            FindAllReturnInfo(wasSuccessful = true, values = allIncomes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load incomes")
        }
    }

    /**
     * Retrieves every income belonging to the category
     *
     * @param category The [Category] to retrieve incomes for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesForCategory(category: Category): FindAllReturnInfo<Income> {
        if (category.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            if (!db .collection("users")
                    .document(uid)
                    .collection("categories")
                    .document(category.id)
                    .get()
                    .await()
                    .exists())
                return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
            val allIncomes = incomes(uid)
                .whereEqualTo("categoryId", category.id)
                .get()
                .await()
                .toObjects<Income>()
            FindAllReturnInfo(wasSuccessful = true, values = allIncomes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load incomes")
        }
    }

    /**
     * Retrieves every income belonging to the category that occurred on the given date
     *
     * @param category The [Category] to retrieve incomes for
     * @param date The date to filter by
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesOnDateForCategory(category: Category, date: LocalDate): FindAllReturnInfo<Income> {
        if (category.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            if (!db .collection("users")
                    .document(uid)
                    .collection("categories")
                    .document(category.id)
                    .get()
                    .await()
                    .exists())
                return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
            val allIncomes = incomes(uid)
                .whereEqualTo("categoryId", category.id)
                .whereEqualTo("date", date.toString())
                .get()
                .await()
                .toObjects<Income>()
            FindAllReturnInfo(wasSuccessful = true, values = allIncomes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load incomes")
        }
    }

    /**
     * Retrieves every income belonging to the category that occurred between the given dates, inclusive
     *
     * @param category The [Category] to retrieve incomes for
     * @param startDate The earliest date to include
     * @param endDate The latest date to include, cannot be before [startDate]
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun retrieveAllIncomesBetweenDatesForCategory(category: Category, startDate: LocalDate, endDate: LocalDate): FindAllReturnInfo<Income> {
        if (category.id.isBlank()) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        if (startDate > endDate) return FindAllReturnInfo(wasSuccessful = false, errMsg = "Start date is after end date")
        val uid = auth.uid ?: return FindAllReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            if (!db .collection("users")
                    .document(uid)
                    .collection("categories")
                    .document(category.id)
                    .get()
                    .await()
                    .exists())
                return FindAllReturnInfo(wasSuccessful = false, errMsg = "Category does not exist in the database")
            val allIncomes = incomes(uid)
                .whereEqualTo("categoryId", category.id)
                .whereGreaterThanOrEqualTo("date", startDate.toString())
                .whereLessThanOrEqualTo("date", endDate.toString())
                .get()
                .await()
                .toObjects<Income>()
            FindAllReturnInfo(wasSuccessful = true, values = allIncomes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load incomes")
        }
    }

    /**
     * Modifies the description for the income
     *
     * @param income The [Income] being updated
     * @param newDescription The new description
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeDescription(income: Income, newDescription: String): UpdateReturnInfo<Income> {
        val result = tryUpdateIncomeDescription(income, newDescription)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "description for income '${income.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateIncomeDescription(income: Income, newDescription: String): UpdateReturnInfo<Income> {
        if (newDescription.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Description is empty")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = incomes(uid),
            entityTypeDisplayName = "Income",
            entity = income,
            property = Income::description,
            newValue = newDescription,
            updatedEntity = income.copy(description = newDescription)
        )
    }

    /**
     * Modifies the amount for the income
     *
     * @param income The [Income] being updated
     * @param newAmount The new amount, cannot be negative
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeAmount(income: Income, newAmount: Double): UpdateReturnInfo<Income> {
        val result = tryUpdateIncomeAmount(income, newAmount)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "amount for income '${income.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateIncomeAmount(income: Income, newAmount: Double): UpdateReturnInfo<Income> {
        if (newAmount < 0.0) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New amount cannot be negative")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = incomes(uid),
            entityTypeDisplayName = "Income",
            entity = income,
            property = Income::amount,
            newValue = newAmount,
            updatedEntity = income.copy(amount = newAmount)
        )
    }

    /**
     * Modifies the date for the income
     *
     * @param income The [Income] being updated
     * @param newDate The new date
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeDate(income: Income, newDate: LocalDate): UpdateReturnInfo<Income> {
        val result = tryUpdateIncomeDate(income, newDate)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "date for income '${income.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateIncomeDate(income: Income, newDate: LocalDate): UpdateReturnInfo<Income> {
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = incomes(uid),
            entityTypeDisplayName = "Income",
            entity = income,
            property = Income::date,
            newValue = newDate.toString(),
            updatedEntity = income.copy(date = newDate.toString())
        )
    }

    /**
     * Modifies the start time for the income
     *
     * @param income The [Income] being updated
     * @param newStartTime The new start time, cannot be after the income's current end time
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeStartTime(income: Income, newStartTime: LocalTime): UpdateReturnInfo<Income> {
        val result = tryUpdateIncomeStartTime(income, newStartTime)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "start time for income '${income.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateIncomeStartTime(income: Income, newStartTime: LocalTime): UpdateReturnInfo<Income> {
        if (income.endTimeAsLocalTime() < newStartTime) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Start time cannot be after end time")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = incomes(uid),
            entityTypeDisplayName = "Income",
            entity = income,
            property = Income::startTime,
            newValue = newStartTime.toString(),
            updatedEntity = income.copy(date = newStartTime.toString())
        )
    }

    /**
     * Modifies the end time for the income
     *
     * @param income The [Income] being updated
     * @param newEndTime The new end time, cannot be before the income's current start time
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeEndTime(income: Income, newEndTime: LocalTime): UpdateReturnInfo<Income> {
        val result = tryUpdateIncomeEndTime(income, newEndTime)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "end time for income '${income.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateIncomeEndTime(income: Income, newEndTime: LocalTime): UpdateReturnInfo<Income> {
        if (newEndTime < income.startTimeAsLocalTime()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "End time cannot be before start time")
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDocumentField(
            collection = incomes(uid),
            entityTypeDisplayName = "Income",
            entity = income,
            property = Income::endTime,
            newValue = newEndTime.toString(),
            updatedEntity = income.copy(date = newEndTime.toString())
        )
    }

    /**
     * Modifies the proof image path one device saved for the income, leaving the other devices' paths untouched
     *
     * @param income The [Income] being updated
     * @param deviceId The ID of the device the image is saved on
     * @param newImagePath The new local image path, or null to remove this device's image
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateIncomeImage(income: Income, deviceId: String, newImagePath: String?): UpdateReturnInfo<Income> {
        val result = tryUpdateIncomeImage(income, deviceId, newImagePath)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "image for income '${income.id}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateIncomeImage(income: Income, deviceId: String, newImagePath: String?): UpdateReturnInfo<Income> {
        val uid = auth.uid ?: return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "No user currently signed in")
        return updateDeviceImagePath(
            collection = incomes(uid),
            entityTypeDisplayName = "Income",
            entity = income,
            id = income.id,
            currentPaths = income.imagePaths,
            deviceId = deviceId,
            newPath = newImagePath,
            withPaths = { income.copy(imagePaths = it) }
        )
    }

    /**
     * Deletes the income from the database
     *
     * @param income The [Income] to delete
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteIncome(income: Income): DeleteReturnStatus {
        val uid = auth.uid
        val result =
            if (uid == null) DeleteReturnStatus.ReauthenticationFailed
            else deleteDocument(
                db = db,
                collection = incomes(uid),
                id = income.id,
                subcollections = emptyList(),
                relatedCollections = emptyList(),
                batchSize = BATCH_SIZE
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
            messageDetails = "income '${income.id}'",
            errMsg = errMsg
        )
        return result
    }
}
