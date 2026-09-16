package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A single deposit against a savings goal
 *
 * @property id The auto-generated primary key for the contribution
 * @property goalId The id of the [SavingsGoal] this contribution applies to
 * @property amount The amount deposited
 * @property date The date the contribution was made
 * @property createdAt The moment the contribution was recorded, kept for future watering-can logic
 */
@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoal::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goal_id"])
    ]
)
data class SavingsContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "goal_id") val goalId: Long,
    val amount: Double,
    val date: LocalDate,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime
)
