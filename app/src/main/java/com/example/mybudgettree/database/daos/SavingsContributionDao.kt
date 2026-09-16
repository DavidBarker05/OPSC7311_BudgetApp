package com.example.mybudgettree.database.daos

import com.example.mybudgettree.database.entries.SavingsContribution
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query

@Dao
interface SavingsContributionDao {
    @Insert
    suspend fun insertContribution(contribution: SavingsContribution): Long

    @Delete
    suspend fun deleteContribution(contribution: SavingsContribution): Int

    @Query("""
        SELECT *
        FROM savings_contributions
        WHERE goal_id = :goalId
    """)
    suspend fun retrieveAllContributionsForGoal(goalId: Long): List<SavingsContribution>

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM savings_contributions
        WHERE goal_id = :goalId
    """)
    suspend fun getTotalForGoal(goalId: Long): Double
}
