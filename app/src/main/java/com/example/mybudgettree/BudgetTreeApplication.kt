package com.example.mybudgettree

import android.app.Application
import androidx.room.Room
import com.example.mybudgettree.database.AppDatabase
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem
import com.example.mybudgettree.database.managers.ExpenseDatabaseSystem
import com.example.mybudgettree.database.managers.IncomeDatabaseSystem
import com.example.mybudgettree.database.managers.SavingsContributionDatabaseSystem
import com.example.mybudgettree.database.managers.SavingsGoalDatabaseSystem
import com.example.mybudgettree.database.managers.UserDatabaseSystem
import com.example.mybudgettree.database.managers.UserTreeDatabaseSystem
import com.example.mybudgettree.database.managers.MonthlyGoalDatabaseSystem
import com.example.mybudgettree.imagestorage.ImageStorageSystem
import com.example.mybudgettree.imagestorage.LocalImageStorageSystem

class BudgetTreeApplication : Application() {
    lateinit var database: AppDatabase private set
    lateinit var userDatabaseSystem: UserDatabaseSystem private set
    lateinit var categoryDatabaseSystem: CategoryDatabaseSystem private set
    lateinit var expenseDatabaseSystem: ExpenseDatabaseSystem private set
    lateinit var incomeDatabaseSystem: IncomeDatabaseSystem private set
    lateinit var savingsGoalDatabaseSystem: SavingsGoalDatabaseSystem private set
    lateinit var savingsContributionDatabaseSystem: SavingsContributionDatabaseSystem private set
    lateinit var userTreeDatabaseSystem: UserTreeDatabaseSystem private set
    lateinit var monthlyGoalDatabaseSystem: MonthlyGoalDatabaseSystem private set
    lateinit var imageStorageSystem: ImageStorageSystem private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mybudgettree.db"
        ).fallbackToDestructiveMigration(true).build()
        userDatabaseSystem = UserDatabaseSystem(database.userDao())
        categoryDatabaseSystem = CategoryDatabaseSystem(database.categoryDao(), userDatabaseSystem)
        expenseDatabaseSystem = ExpenseDatabaseSystem(database.expenseDao(), userDatabaseSystem, categoryDatabaseSystem)
        incomeDatabaseSystem = IncomeDatabaseSystem(database.incomeDao(), userDatabaseSystem, categoryDatabaseSystem)
        savingsGoalDatabaseSystem = SavingsGoalDatabaseSystem(database.savingsGoalDao(), userDatabaseSystem)
        savingsContributionDatabaseSystem = SavingsContributionDatabaseSystem(database.savingsContributionDao(), savingsGoalDatabaseSystem)
        userTreeDatabaseSystem = UserTreeDatabaseSystem(database.userTreeDao())
        monthlyGoalDatabaseSystem = MonthlyGoalDatabaseSystem(database.monthlyGoalDao())
        imageStorageSystem = LocalImageStorageSystem(applicationContext)
    }
}
