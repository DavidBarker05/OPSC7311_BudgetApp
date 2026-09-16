package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.ColumnInfo
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * A user's money-tree gamification state, split out of [User] since it is not
 * required until the final PoE and should stay inert until then
 *
 * @property username The username of the [User] this tree state belongs to, also its primary key
 * @property treeLevel The current growth level of the user's money tree
 * @property yearMonth The year and month the current [treeLevel] applies to, used to detect when a new month has started so the tree can reset
 * @property lastWateringTime The last time the user "poured" the watering can, or null if never
 */
@Entity(
    tableName = "user_trees",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["username"],
            childColumns = ["username"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserTree(
    @PrimaryKey val username: String,
    @ColumnInfo(name = "tree_level") val treeLevel: Int = 1,
    @ColumnInfo(name = "year_month") val yearMonth: YearMonth,
    @ColumnInfo(name = "last_watering_time") val lastWateringTime: LocalDateTime? = null
)
