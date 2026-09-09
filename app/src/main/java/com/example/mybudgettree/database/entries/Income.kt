package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.time.LocalDate
import java.time.LocalTime

/**
 * A single income record belonging to a category
 *
 * @property id The auto-generated primary key for the income
 * @property categoryId The id of the [Category] the income belongs to
 * @property description The income's name
 * @property currencyAtTime The currency the amount was denominated in at the time of the income
 * @property amount The income amount
 * @property date The date the income occurred on
 * @property startTime The time the income started
 * @property endTime The time the income ended
 * @property imagePath The path to the income's proof image, or null if none is set
 */
@Entity(
    tableName = "incomes",
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
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val description: String,
    val amount: Double,
    val date: LocalDate,
    @ColumnInfo(name = "start_time") val startTime: LocalTime,
    @ColumnInfo(name = "end_time") val endTime: LocalTime,
    @ColumnInfo(name = "image_path") val imagePath: String? = null
)
