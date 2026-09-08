package com.example.mybudgettree

import android.app.Application
import androidx.room.Room
import com.example.mybudgettree.database.AppDatabase
import com.example.mybudgettree.database.managers.BudgetDatabaseSystem
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem
import com.example.mybudgettree.database.managers.ExpenseDatabaseSystem
import com.example.mybudgettree.database.managers.IncomeDatabaseSystem
import com.example.mybudgettree.database.managers.UserDatabaseSystem
import com.example.mybudgettree.imagestorage.ImageStorageSystem
import com.example.mybudgettree.imagestorage.LocalImageStorageSystem

class BudgetTreeApplication : Application() {
    lateinit var database: AppDatabase private set
    lateinit var userDatabaseSystem: UserDatabaseSystem private set
    lateinit var categoryDatabaseSystem: CategoryDatabaseSystem private set
    lateinit var budgetDatabaseSystem: BudgetDatabaseSystem private set
    lateinit var expenseDatabaseSystem: ExpenseDatabaseSystem private set
    lateinit var incomeDatabaseSystem: IncomeDatabaseSystem private set
    lateinit var imageStorageSystem: ImageStorageSystem private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mybudgettree.db"
        ).build()
        userDatabaseSystem = UserDatabaseSystem(database.userDao())
        categoryDatabaseSystem = CategoryDatabaseSystem(database.categoryDao(), userDatabaseSystem)
        budgetDatabaseSystem = BudgetDatabaseSystem(database.budgetDao(), categoryDatabaseSystem)
        expenseDatabaseSystem = ExpenseDatabaseSystem(database.expenseDao(), userDatabaseSystem, categoryDatabaseSystem)
        incomeDatabaseSystem = IncomeDatabaseSystem(database.incomeDao(), userDatabaseSystem, categoryDatabaseSystem)
        imageStorageSystem = LocalImageStorageSystem(applicationContext)
    }
}
