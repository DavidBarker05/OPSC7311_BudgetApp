package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.UserTree
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface UserTreeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserTree(userTree: UserTree): Long

    @Query("""
        SELECT *
        FROM user_trees
        WHERE username = :username
    """)
    suspend fun findUserTree(username: String): UserTree?
}
