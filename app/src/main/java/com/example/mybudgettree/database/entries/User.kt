package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.time.LocalDate
import java.time.YearMonth

/**
 * A user account
 *
 * @property username The user's unique username, used as the primary key
 * @property password The account password
 * @property email The account email address, must be unique
 * @property phoneNumber The account phone number, must be unique
 * @property displayName The name shown for the user
 * @property dateOfBirth The user's date of birth
 * @property currency The user's preferred currency
 * @property profilePhotoPath The path to the user's profile photo, or null if none is set
 * @property treeLevel The current growth level of the user's money tree
 * @property treeLevelPeriod The year and month the current [treeLevel] applies to, used to detect when a new month has started so the tree can reset
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["phone_number"], unique = true)
    ]
)
data class User(
    @PrimaryKey val username: String,
    val password: String,
    val email: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: LocalDate,
    val currency: String,
    @ColumnInfo(name = "profile_photo_path") val profilePhotoPath: String? = null,
    @ColumnInfo(name = "tree_level") val treeLevel: Int = 1,
    @ColumnInfo(name = "tree_level_period") val treeLevelPeriod: YearMonth
)
