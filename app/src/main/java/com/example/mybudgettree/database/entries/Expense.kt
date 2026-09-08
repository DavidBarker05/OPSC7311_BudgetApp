package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.time.LocalDate
import java.time.LocalTime

/**
 * A single expense record belonging to a category
 *
 * @property id The auto-generated primary key for the expense
 * @property categoryId The id of the [Category] the expense belongs to
 * @property description The expense's name
 * @property currencyAtTime The currency the amount was denominated in at the time of the expense
 * @property amount The expense amount
 * @property date The date the expense occurred on
 * @property startTime The time the expense started
 * @property endTime The time the expense ended
 * @property imagePath The path to the expense's receipt image, or null if none is set
 */
@Entity(
    tableName = "expenses",
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
        Index(value = ["category_id"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val description: String,
    @ColumnInfo(name = "currency_at_time") val currencyAtTime: String,
    val amount: Double,
    val date: LocalDate,
    @ColumnInfo(name = "start_time") val startTime: LocalTime,
    @ColumnInfo(name = "end_time") val endTime: LocalTime,
    @ColumnInfo(name = "image_path") val imagePath: String? = null
)
