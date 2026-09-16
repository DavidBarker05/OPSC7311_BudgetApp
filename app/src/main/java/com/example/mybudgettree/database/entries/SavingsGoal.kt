package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index

/**
 * A savings goal belonging to a user (e.g. Travel, Wedding, Car)
 *
 * @property id The auto-generated primary key for the goal
 * @property username The username of the [User] the goal belongs to
 * @property goalName The goal's name, must be unique per user
 * @property iconKey The [com.example.mybudgettree.IconCatalog] key for the goal's icon, or null to fall back to a default
 * @property targetAmount The amount the user is aiming to save, or null if no target is set
 */
@Entity(
    tableName = "savings_goals",
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
        Index(value = ["username", "goal_name"], unique = true)
    ]
)
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String, // Foreign key
    @ColumnInfo(name = "goal_name") val goalName: String,
    @ColumnInfo(name = "icon_key") val iconKey: String? = null,
    @ColumnInfo(name = "target_amount") val targetAmount: Double? = null
)
