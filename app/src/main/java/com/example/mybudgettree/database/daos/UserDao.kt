package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.User
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy
import java.time.LocalDate

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User): Long

    @Query("""
        UPDATE users
        SET username = :newUsername
        WHERE username = :oldUsername
    """)
    suspend fun updateUsername(oldUsername: String, newUsername: String)

    @Query("""
        UPDATE users
        SET password = :newPassword
        WHERE username = :username
    """)
    suspend fun updatePassword(username: String, newPassword: String)

    @Query("""
        UPDATE users
        SET email = :newEmail
        WHERE username = :username
    """)
    suspend fun updateEmail(username: String, newEmail: String)

    @Query("""
        UPDATE users
        SET phone_number = :newPhoneNumber
        WHERE username = :username
    """)
    suspend fun updatePhoneNumber(username: String, newPhoneNumber: String)

    @Query("""
        UPDATE users
        SET display_name = :newDisplayName
        WHERE username = :username
    """)
    suspend fun updateDisplayName(username: String, newDisplayName: String)

    @Query("""
        UPDATE users
        SET date_of_birth = :newDateOfBirth
        WHERE username = :username
    """)
    suspend fun updateDateOfBirth(username: String, newDateOfBirth: LocalDate)

    @Query("""
        UPDATE users
        SET currency = :newCurrency
        WHERE username = :username
    """)
    suspend fun updateCurrency(username: String, newCurrency: String)

    @Query("""
        UPDATE users
        SET profile_photo_path = :newProfilePhotoPath
        WHERE username = :username
    """)
    suspend fun updateProfilePhoto(username: String, newProfilePhotoPath: String?)

    @Query("""
        UPDATE users
        SET tree_level = :newTreeLevel
        WHERE username = :username
    """)
    suspend fun updateTreeLevel(username: String, newTreeLevel: Int)

    @Delete
    suspend fun deleteUser(user: User): Int

    @Query("""
        SELECT *
        FROM users
        WHERE username = :username
    """)
    suspend fun findUser(username: String): User?

    @Query("""
        SELECT *
        FROM users
        WHERE email = :email
    """)
    suspend fun findUserByEmail(email: String): User?

    @Query("""
        SELECT *
        FROM users
        WHERE phone_number = :phoneNumber
    """)
    suspend fun findUserByPhoneNumber(phoneNumber: String): User?
}