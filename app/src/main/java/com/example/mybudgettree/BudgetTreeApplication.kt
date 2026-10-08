package com.example.mybudgettree

import android.app.Application
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
    // internal (rather than private) so instrumented tests in the same module can swap these, e.g. for systems
    // pointed at the Firebase emulators
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
        // The systems default to the shared Firebase instances, so they need no wiring
        userDatabaseSystem = UserDatabaseSystem()
        categoryDatabaseSystem = CategoryDatabaseSystem()
        expenseDatabaseSystem = ExpenseDatabaseSystem()
        incomeDatabaseSystem = IncomeDatabaseSystem()
        savingsGoalDatabaseSystem = SavingsGoalDatabaseSystem()
        savingsContributionDatabaseSystem = SavingsContributionDatabaseSystem()
        userTreeDatabaseSystem = UserTreeDatabaseSystem()
        monthlyGoalDatabaseSystem = MonthlyGoalDatabaseSystem()
        imageStorageSystem = LocalImageStorageSystem(applicationContext)
    }
}
