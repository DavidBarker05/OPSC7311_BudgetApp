package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.time.YearMonth

/**
 * A user's overall minimum/maximum spending goal for a given month, distinct from
 * per-category budgets
 *
 * @property id The auto-generated primary key for the goal
 * @property username The username of the [User] the goal belongs to
 * @property period The year and month this goal applies to
 * @property minGoal The minimum amount the user intends to spend this month
 * @property maxGoal The maximum amount the user intends to spend this month
 */
@Entity(
    tableName = "monthly_goals",
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
        Index(value = ["username", "period"], unique = true)
    ]
)
data class MonthlyGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String, // Foreign key
    val period: YearMonth,
    @ColumnInfo(name = "min_goal") val minGoal: Double,
    @ColumnInfo(name = "max_goal") val maxGoal: Double
)
