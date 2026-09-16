package com.example.mybudgettree

import com.example.mybudgettree.database.entries.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/**
 * Snapshot of actual savings-goal progress, used by the Home and Quickly Analysis
 * "Savings On Goals" cards
 *
 * @property percentOfTarget This month's savings as a percentage of the sum of all goal targets
 * @property savedThisMonth Total contributed to any savings goal this calendar month
 * @property savedThisWeek Total contributed to any savings goal since the most recent Monday
 * @property filledDrops How many of the 5 sapling drops should appear filled
 */
data class SavingsSnapshot(
    val percentOfTarget: Int,
    val savedThisMonth: Double,
    val savedThisWeek: Double,
    val filledDrops: Int
) {
    companion object {
        suspend fun compute(app: BudgetTreeApplication, user: User, today: LocalDate = LocalDate.now()): SavingsSnapshot {
            val goals = app.savingsGoalDatabaseSystem.getAllGoalsForUser(user).goals.orEmpty()
            val contributions = goals.flatMap { goal ->
                app.savingsContributionDatabaseSystem.retrieveAllContributionsForGoal(goal).contributions.orEmpty()
            }
            val targetSum = goals.mapNotNull { it.targetAmount }.sum()
            val currentMonth = YearMonth.from(today)
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val savedThisMonth = contributions.filter { YearMonth.from(it.date) == currentMonth }.sumOf { it.amount }
            val savedThisWeek = contributions.filter { it.date in weekStart..today }.sumOf { it.amount }
            val percent = if (targetSum <= 0.0) 0 else ((savedThisMonth / targetSum) * 100.0).toInt().coerceIn(0, 100)
            val filledDrops = if (targetSum <= 0.0) 0 else (percent / 20.0).roundToInt().coerceIn(0, 5)
            return SavingsSnapshot(percent, savedThisMonth, savedThisWeek, filledDrops)
        }
    }
}
