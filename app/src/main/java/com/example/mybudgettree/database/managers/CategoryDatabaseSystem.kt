package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.CategoryDao
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category

class CategoryDatabaseSystem(
    private val categoryDao: CategoryDao,
    private val userDatabaseSystem: UserDatabaseSystem
) {

    data class CreateCategoryReturnInfo(
        val wasSuccessful: Boolean,
        val category: Category? = null,
        val errMsg: String? = null
    )

    data class FindCategoryReturnInfo(
        val wasSuccessful: Boolean,
        val category: Category? = null,
        val errMsg: String? = null
    )

    data class FindAllCategoriesReturnInfo(
        val wasSuccessful: Boolean,
        val categories: List<Category>? = null,
        val errMsg: String? = null
    )

    enum class UpdateCategoryReturnStatus {
        Failed,
        NoChange,
        Succeeded
    }

    data class UpdateCategoryReturnInfo(
        val status: UpdateCategoryReturnStatus,
        val category: Category? = null,
        val errMsg: String? = null
    )

    enum class CategoryDeleteReturnStatus {
        DoesNotExist,
        Deleted
    }

    suspend fun createCategory(user: User, categoryName: String): CreateCategoryReturnInfo {
        if (categoryName.isBlank()) return CreateCategoryReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return CreateCategoryReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        val category = Category(username = user.username, categoryName = categoryName)
        val id = categoryDao.insertCategory(category) ?: return CreateCategoryReturnInfo(wasSuccessful = false, errMsg = "User already has a category with name \"$categoryName\"")
        val categoryWithId = category.copy(id = id)
        return CreateCategoryReturnInfo(wasSuccessful = true, category = categoryWithId)
    }

    suspend fun findCategory(user: User, categoryName: String): FindCategoryReturnInfo {
        if (categoryName.isBlank()) return FindCategoryReturnInfo(wasSuccessful = false, errMsg = "Category name is empty")
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return FindCategoryReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        val foundCategory = categoryDao.findCategory(user.username, categoryName)
        return if (foundCategory != null) FindCategoryReturnInfo(wasSuccessful = true, category = foundCategory)
        else FindCategoryReturnInfo(wasSuccessful = false, errMsg = "No category with name = \"$categoryName\" found")
    }

    suspend fun doesCategoryExist(user: User, categoryName: String): Boolean = findCategory(user, categoryName).wasSuccessful

    suspend fun getAllCategoriesForUser(user: User): FindAllCategoriesReturnInfo {
        val userStatus = userDatabaseSystem.findUser(user.username)
        if (!userStatus.wasSuccessful) return FindAllCategoriesReturnInfo(wasSuccessful = false, errMsg = userStatus.errMsg)
        return FindAllCategoriesReturnInfo(wasSuccessful = true, categories = categoryDao.retrieveAllCategories(user.username))
    }

    suspend fun updateCategoryName(category: Category, newCategoryName: String): UpdateCategoryReturnInfo {
        if (newCategoryName.isBlank()) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category name is empty")
        if (category.categoryName == newCategoryName) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.NoChange, category = category)
        val userStatus = userDatabaseSystem.findUser(category.username)
        if (!userStatus.wasSuccessful) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = userStatus.errMsg)
        categoryDao.findCategory(category.username, category.categoryName) ?: return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category does not exist")
        if (categoryDao.findCategory(category.username, newCategoryName) != null) return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Failed, errMsg = "Category name already in use")
        categoryDao.updateCategoryName(category.id, newCategoryName)
        val categoryNewName = category.copy(categoryName = newCategoryName)
        return UpdateCategoryReturnInfo(status = UpdateCategoryReturnStatus.Succeeded, category = categoryNewName)
    }

    suspend fun deleteCategory(category: Category): CategoryDeleteReturnStatus = if (categoryDao.deleteCategory(category) == 1) CategoryDeleteReturnStatus.Deleted else CategoryDeleteReturnStatus.DoesNotExist
}