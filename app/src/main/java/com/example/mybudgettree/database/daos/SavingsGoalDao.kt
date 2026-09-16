package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.SavingsGoal
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface SavingsGoalDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Query("""
        UPDATE savings_goals
        SET goal_name = :newName
        WHERE id = :id
    """)
    suspend fun updateGoalName(id: Long, newName: String)

    @Query("""
        UPDATE savings_goals
        SET icon_key = :newIconKey
        WHERE id = :id
    """)
    suspend fun updateGoalIcon(id: Long, newIconKey: String?)

    @Query("""
        UPDATE savings_goals
        SET target_amount = :newTargetAmount
        WHERE id = :id
    """)
    suspend fun updateGoalTarget(id: Long, newTargetAmount: Double?)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal): Int

    @Query("""
        SELECT *
        FROM savings_goals
        WHERE id = :id
    """)
    suspend fun findGoal(id: Long): SavingsGoal?

    @Query("""
        SELECT *
        FROM savings_goals
        WHERE username = :username AND goal_name = :goalName
    """)
    suspend fun findGoal(username: String, goalName: String): SavingsGoal?

    @Query("""
        SELECT *
        FROM savings_goals
        WHERE username = :username
    """)
    suspend fun retrieveAllGoals(username: String): List<SavingsGoal>
}
