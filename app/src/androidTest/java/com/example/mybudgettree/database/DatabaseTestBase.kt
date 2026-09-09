package com.example.mybudgettree.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem
import com.example.mybudgettree.database.managers.ExpenseDatabaseSystem
import com.example.mybudgettree.database.managers.IncomeDatabaseSystem
import com.example.mybudgettree.database.managers.UserDatabaseSystem
import com.example.mybudgettree.imagestorage.ImageStorageSystem
import com.example.mybudgettree.imagestorage.LocalImageStorageSystem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.time.LocalDate

abstract class DatabaseTestBase {
    protected lateinit var db: AppDatabase
    protected lateinit var userDatabaseSystem: UserDatabaseSystem
    protected lateinit var categoryDatabaseSystem: CategoryDatabaseSystem
    protected lateinit var expenseDatabaseSystem: ExpenseDatabaseSystem
    protected lateinit var incomeDatabaseSystem: IncomeDatabaseSystem
    protected lateinit var imageStorageSystem: ImageStorageSystem

    @Before
    fun setUpDatabase() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        userDatabaseSystem = UserDatabaseSystem(db.userDao())
        categoryDatabaseSystem = CategoryDatabaseSystem(db.categoryDao(), userDatabaseSystem)
        expenseDatabaseSystem = ExpenseDatabaseSystem(db.expenseDao(), userDatabaseSystem, categoryDatabaseSystem)
        incomeDatabaseSystem = IncomeDatabaseSystem(db.incomeDao(), userDatabaseSystem, categoryDatabaseSystem)
        imageStorageSystem = LocalImageStorageSystem(ApplicationProvider.getApplicationContext())
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    protected fun createTestUser(username: String = "testuser"): User = runBlocking {
        userDatabaseSystem.createUser(
            username = username,
            password = "password123",
            email = "$username@example.com",
            phoneNumber = "0821234567".plus(username.hashCode().toString().takeLast(2)),
            displayName = "Test User",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        ).user!!
    }

    protected fun createTestCategory(user: User, categoryName: String = "Groceries"): Category = runBlocking {
        categoryDatabaseSystem.createCategory(user, categoryName).category!!
    }
}
