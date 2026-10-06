package com.example.mybudgettree.database.managers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException


import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.managers.shared.*
import com.example.mybudgettree.database.managers.shared.firebase.*
import com.google.firebase.firestore.toObject

/**
 * This system manages category state, uniqueness validation, discovery, updates, and removals
 *
 */
class CategoryDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "CategoryDatabaseSystem"

        private const val BATCH_SIZE = 400L

        private val CATEGORY_RELATED_COLLECTIONS = listOf(
            RelatedCollection(collectionName = "expenses", referenceField = "categoryId"),
            RelatedCollection(collectionName = "incomes", referenceField = "categoryId")
        )
    }

    private fun categories(uid: String): CollectionReference = db.collection("users").document(uid).collection("categories")

    /**
     * Creates a new category for the user after validating the name and confirming it isn't already in use
     *
     * @param user The [User] the category belongs to
     * @param categoryName The desired name for the category, must be unique for the user
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createCategory(user: User, categoryName: String): CreateReturnInfo<Category> {
        val result = tryCreateCategory(user, categoryName)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "created",
            verbOnFailure = "to create",
            messageDetails = "category '$categoryName' for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateCategory(user: User, categoryName: String): CreateReturnInfo<Category> {
        if (categoryName.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
        if (auth.currentUser?.uid != user.uid) return CreateReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val categories = categories(user.uid)
            val nameInUse = !categories.whereEqualTo("categoryName", categoryName).limit(1).get().await().isEmpty
            if (nameInUse) return CreateReturnInfo(wasSuccessful = false, errMsg = "User already has a category with name \"$categoryName\"")
            val category = Category(categoryName = categoryName)
            val ref = categories.add(category).await()
            CreateReturnInfo(wasSuccessful = true, value = category.copy(id = ref.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create category")
        }
    }

    /**
     * Find the category in the database if it exists
     *
     * @param categoryId The id to search for
     * @return A [FindReturnInfo] indicating what happened with the search
     */
    suspend fun findCategory(categoryId: String): FindReturnInfo<Category> {
        if (categoryId.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Category id is empty")
        val uid = auth.uid ?: return FindReturnInfo(wasSuccessful = false, errMsg = "No user currently signed in")
        return try {
            val category = categories(uid).document(categoryId).get().await().toObject(Category::class.java)
            if (category != null) FindReturnInfo(wasSuccessful = true, value = category)
            else FindReturnInfo(wasSuccessful = false, errMsg = "Could not find category '$categoryId' for current auth user")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not find category '$categoryId' for current auth user")
        }
    }

    /**
     * Find the user's category by name if it exists
     *
     * @param user The [User] the category should belong to
     * @param categoryName The category name to search for
     * @return A [FindReturnInfo] indicating what happened with the search
     */
    suspend fun findCategory(user: User, categoryName: String): FindReturnInfo<Category> {
        if (categoryName.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
        if (auth.currentUser?.uid != user.uid) return FindReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val foundCategory = categories(user.uid)
                .whereEqualTo("categoryName", categoryName)
                .limit(1)
                .get().await()
                .documents.firstOrNull()
                ?.toObject<Category>()
            if (foundCategory != null) FindReturnInfo(wasSuccessful = true, value = foundCategory)
            else FindReturnInfo(wasSuccessful = false, errMsg = "No category with name = \"$categoryName\" found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not find category '$categoryName'")
        }
    }
    
    /**
     * Retrieves every category belonging to the user
     *
     * @param user The [User] to retrieve categories for
     * @return A [FindAllReturnInfo] indicating what happened with the retrieval
     */
    suspend fun getAllCategoriesForUser(user: User): FindAllReturnInfo<Category> {
        if (auth.currentUser?.uid != user.uid) return FindAllReturnInfo(wasSuccessful = false, errMsg = "User does not exist")
        return try {
            val allCategories = categories(user.uid).get().await().toObjects(Category::class.java)
            FindAllReturnInfo(wasSuccessful = true, values = allCategories)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FindAllReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not load categories")
        }
    }

    /**
     * Modifies the name for the category
     *
     * @param category The [Category] being updated
     * @param newCategoryName The new category name
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryName(category: Category, newCategoryName: String): UpdateReturnInfo<Category> {
        val result = tryUpdateCategoryName(category, newCategoryName)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "name for category '${category.categoryName}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateCategoryName(category: Category, newCategoryName: String): UpdateReturnInfo<Category> {

        if (newCategoryName.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New category name is empty")
        return updateDocumentField(
            auth = auth,
            db = db,
            collectionName = "categories",
            entityTypeDisplayName = "Category",
            id = category.id,
            entity = category,
            property = Category::categoryName,
            value = newCategoryName,
            updatedEntity = category.copy(categoryName = newCategoryName)
        )
    }

    /**
     * Modifies the budget for the category
     *
     * @param category The [Category] being updated
     * @param newBudgetAmount The new budgeted amount, or null to remove the budget, cannot be negative
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryBudget(category: Category, newBudgetAmount: Double?): UpdateReturnInfo<Category> {
        val result = tryUpdateCategoryBudget(category, newBudgetAmount)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "budget for category '${category.categoryName}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateCategoryBudget(category: Category, newBudgetAmount: Double?): UpdateReturnInfo<Category> {
        if (newBudgetAmount != null && newBudgetAmount < 0.0) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Budget amount cannot be negative")
        return updateDocumentField(
            auth = auth,
            db = db,
            collectionName = "categories",
            entityTypeDisplayName = "Category",
            id = category.id,
            entity = category,
            property = Category::budgetAmount,
            value = newBudgetAmount,
            updatedEntity = category.copy(budgetAmount = newBudgetAmount)
        )
    }

    /**
     * Modifies the icon for the category
     *
     * @param category The [Category] being updated
     * @param newIconKey The new [com.example.mybudgettree.IconCatalog] key, or null to fall back to the name-based default
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateCategoryIcon(category: Category, newIconKey: String?): UpdateReturnInfo<Category> {
        val result = tryUpdateCategoryIcon(category, newIconKey)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "icon for category '${category.categoryName}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateCategoryIcon(category: Category, newIconKey: String?): UpdateReturnInfo<Category> {
        if (newIconKey?.isBlank() ?: false) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New icon key path is empty")
        return updateDocumentField(
            auth = auth,
            db = db,
            collectionName = "categories",
            entityTypeDisplayName = "Category",
            id = category.id,
            entity = category,
            property = Category::iconKey,
            value = newIconKey,
            updatedEntity = category.copy(iconKey = newIconKey)
        )
    }

    /**
     * Deletes the category from the database
     *
     * @param category The [Category] to delete
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteCategory(category: Category): DeleteReturnStatus {
        val result = deleteDocument(
            auth = auth,
            db = db,
            collectionName = "categories",
            id = category.id,
            subCollections = emptyList(),
            relatedCollections = CATEGORY_RELATED_COLLECTIONS,
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
            messageDetails = "category '${category.categoryName}'",
            errMsg = errMsg
        )
        return result
    }
}
