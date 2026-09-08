package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index

/**
 * A budget for a category, at most one per category
 *
 * @property id The auto-generated primary key for the budget
 * @property categoryId The id of the [Category] the budget belongs to, must be unique
 * @property currency The currency the budgeted amount is denominated in
 * @property amount The budgeted amount
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"], unique = true)
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val currency: String,
    val amount: Double
)
