package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.MonthlyGoal
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import java.time.YearMonth

@Dao
interface MonthlyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: MonthlyGoal): Long

    @Query("""
        SELECT *
        FROM monthly_goals
        WHERE username = :username AND period = :period
    """)
    suspend fun findGoal(username: String, period: YearMonth): MonthlyGoal?
}
