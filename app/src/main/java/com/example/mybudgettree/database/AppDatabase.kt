package com.example.mybudgettree.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mybudgettree.database.type_converters.DateTimeConverter
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.UserTree
import com.example.mybudgettree.database.entries.MonthlyGoal
import com.example.mybudgettree.database.daos.UserDao
import com.example.mybudgettree.database.daos.CategoryDao
import com.example.mybudgettree.database.daos.ExpenseDao
import com.example.mybudgettree.database.daos.IncomeDao
import com.example.mybudgettree.database.daos.SavingsGoalDao
import com.example.mybudgettree.database.daos.SavingsContributionDao
import com.example.mybudgettree.database.daos.UserTreeDao
import com.example.mybudgettree.database.daos.MonthlyGoalDao

@Database(
    entities = [User::class, Category::class, Expense::class, Income::class, SavingsGoal::class, SavingsContribution::class, UserTree::class, MonthlyGoal::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun savingsContributionDao(): SavingsContributionDao
    abstract fun userTreeDao(): UserTreeDao
    abstract fun monthlyGoalDao(): MonthlyGoalDao
}