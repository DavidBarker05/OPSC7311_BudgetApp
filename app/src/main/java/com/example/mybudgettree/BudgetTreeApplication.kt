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
    // internal (rather than private) so instrumented UI tests in the same module can swap
    // these for an isolated in-memory database instead of the real on-device one — see
    // UiTestBase in the androidTest source set.
    lateinit var database: AppDatabase internal set
    lateinit var userDatabaseSystem: UserDatabaseSystem internal set
    lateinit var categoryDatabaseSystem: CategoryDatabaseSystem internal set
    lateinit var expenseDatabaseSystem: ExpenseDatabaseSystem internal set
    lateinit var incomeDatabaseSystem: IncomeDatabaseSystem internal set
    lateinit var savingsGoalDatabaseSystem: SavingsGoalDatabaseSystem internal set
    lateinit var savingsContributionDatabaseSystem: SavingsContributionDatabaseSystem internal set
    lateinit var userTreeDatabaseSystem: UserTreeDatabaseSystem internal set
    lateinit var monthlyGoalDatabaseSystem: MonthlyGoalDatabaseSystem internal set
    lateinit var imageStorageSystem: ImageStorageSystem internal set

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
