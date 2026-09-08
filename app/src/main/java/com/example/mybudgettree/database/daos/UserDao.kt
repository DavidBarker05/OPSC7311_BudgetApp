package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.User
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User): Long?

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

    @Delete
    suspend fun deleteUser(user: User): Int

    @Query("""
        SELECT *
        FROM users
        WHERE username = :username
    """)
    suspend fun findUser(username: String): User?
}