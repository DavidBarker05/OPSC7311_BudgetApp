package com.example.mybudgettree.database.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.time.LocalDate

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
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "profile_photo_path") val profilePhotoPath: String? = null
)