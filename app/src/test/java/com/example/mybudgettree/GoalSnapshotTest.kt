package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.MonthlyGoal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class GoalSnapshotTest {

    private val today = LocalDate.of(2026, 6, 15)

    private fun expense(amount: Double, date: LocalDate) = Expense(
        categoryId = 1,
        description = "test",
        amount = amount,
        date = date,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT
    )

    private fun goal(min: Double, max: Double) = MonthlyGoal(
        username = "user",
        period = YearMonth.from(today),
        minGoal = min,
        maxGoal = max
    )

    @Test
    fun from_noMonthlyGoal_zeroesOutBudgetFields() {
        val snapshot = GoalSnapshot.from(emptyList(), monthlyGoal = null, today = today)
        assertEquals(0, snapshot.goalPercent)
        assertEquals(0.0, snapshot.budgetGoal, 0.0)
        assertEquals(0.0, snapshot.minGoal, 0.0)
    }

    @Test
    fun from_onlyCountsExpensesInTheGivenMonth() {
        val expenses = listOf(
            expense(500.0, today), // this month
            expense(9999.0, today.minusMonths(1)) // last month, should be excluded
        )
        val snapshot = GoalSnapshot.from(expenses, goal(min = 0.0, max = 2000.0), today)
        assertEquals(500.0, snapshot.spentThisMonth, 0.0)
    }

    @Test
    fun from_computesPercentOfMaxGoal() {
        val expenses = listOf(expense(600.0, today))
        val snapshot = GoalSnapshot.from(expenses, goal(min = 0.0, max = 2000.0), today)
        assertEquals(30, snapshot.goalPercent)
    }

    @Test
    fun from_percentIsCappedAt100EvenWhenOverspent() {
        val expenses = listOf(expense(5000.0, today))
        val snapshot = GoalSnapshot.from(expenses, goal(min = 0.0, max = 2000.0), today)
        assertEquals(100, snapshot.goalPercent)
    }

    @Test
    fun from_overspendIsStillReflectedInRawSpentAmount_notJustTheCappedPercent() {
        // regression guard: goalPercent alone can't tell overspend from exactly-at-budget once
        // it's capped at 100, so spentThisMonth must carry the real, uncapped figure
        val expenses = listOf(expense(5000.0, today))
        val snapshot = GoalSnapshot.from(expenses, goal(min = 0.0, max = 2000.0), today)
        assertEquals(5000.0, snapshot.spentThisMonth, 0.0)
    }

    @Test
    fun from_zeroMaxGoal_percentIsZeroRegardlessOfSpend() {
        val expenses = listOf(expense(500.0, today))
        val snapshot = GoalSnapshot.from(expenses, goal(min = 100.0, max = 0.0), today)
        assertEquals(0, snapshot.goalPercent)
    }
}
