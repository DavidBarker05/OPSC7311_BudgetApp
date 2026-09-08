package com.example.mybudgettree.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mybudgettree.database.type_converters.DateTimeConverter
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.daos.UserDao
import com.example.mybudgettree.database.daos.CategoryDao
import com.example.mybudgettree.database.daos.ExpenseDao

@Database(
    entities = [User::class, Category::class, Expense::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
}