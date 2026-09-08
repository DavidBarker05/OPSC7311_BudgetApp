package com.example.mybudgettree.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mybudgettree.database.type_converters.DateTimeConverter
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import com.example.mybudgettree.database.entries.Budget
import com.example.mybudgettree.database.daos.UserDao
import com.example.mybudgettree.database.daos.CategoryDao
import com.example.mybudgettree.database.daos.ExpenseDao
import com.example.mybudgettree.database.daos.IncomeDao
import com.example.mybudgettree.database.daos.BudgetDao

@Database(
    entities = [User::class, Category::class, Expense::class, Income::class, Budget::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun budgetDao(): BudgetDao
}