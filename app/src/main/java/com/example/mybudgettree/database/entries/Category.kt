package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index

/**
 * A spending/income category belonging to a user
 *
 * @property id The auto-generated primary key for the category
 * @property username The username of the [User] the category belongs to
 * @property categoryName The category's name, must be unique per user
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["username"],
            childColumns = ["username"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["username", "category_name"], unique = true)
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String, // Foreign key
    @ColumnInfo(name = "category_name") val categoryName: String
)
