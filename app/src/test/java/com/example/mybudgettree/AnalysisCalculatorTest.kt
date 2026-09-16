package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import com.example.mybudgettree.database.entries.MonthlyGoal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class AnalysisCalculatorTest {

    // 2026-06-15 is a Monday, which keeps the DAILY week boundary predictable
    private val anchor = LocalDate.of(2026, 6, 15)

    private fun expense(amount: Double, date: LocalDate) = Expense(
        categoryId = 1,
        description = "test",
        amount = amount,
        date = date,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT
    )

    private fun income(amount: Double, date: LocalDate) = Income(
        categoryId = 1,
        description = "test",
        amount = amount,
        date = date,
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT
    )

    private fun goal(min: Double, max: Double) = MonthlyGoal(
        username = "user",
        period = YearMonth.from(anchor),
        minGoal = min,
        maxGoal = max
    )

    @Test
    fun totalBalance_isTotalIncomeMinusTotalExpense() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = listOf(income(1000.0, anchor)),
            expenses = listOf(expense(300.0, anchor)),
            monthlyGoal = null,
            period = AnalysisPeriod.MONTHLY,
            anchorDate = anchor
        )
        assertEquals(700.0, snapshot.totalBalance, 0.0)
    }

    @Test
    fun monthExpense_onlyCountsExpensesInAnchorMonth() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = emptyList(),
            expenses = listOf(
                expense(300.0, anchor),
                expense(9999.0, anchor.minusMonths(1))
            ),
            monthlyGoal = null,
            period = AnalysisPeriod.MONTHLY,
            anchorDate = anchor
        )
        assertEquals(300.0, snapshot.monthExpense, 0.0)
    }

    @Test
    fun expensePercent_isNotCappedAt100_unlikeGoalSnapshotPercent() {
        // AnalysisSnapshot.expensePercent intentionally does NOT cap at 100, since the
        // Analysis screen needs to show genuine overspend (e.g. "312%"), not a flattened bar
        val snapshot = AnalysisCalculator.snapshot(
            incomes = emptyList(),
            expenses = listOf(expense(3000.0, anchor)),
            monthlyGoal = goal(min = 0.0, max = 1000.0),
            period = AnalysisPeriod.MONTHLY,
            anchorDate = anchor
        )
        assertEquals(300, snapshot.expensePercent)
    }

    @Test
    fun expensePercent_zeroBudget_isZero() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = emptyList(),
            expenses = listOf(expense(300.0, anchor)),
            monthlyGoal = null,
            period = AnalysisPeriod.MONTHLY,
            anchorDate = anchor
        )
        assertEquals(0, snapshot.expensePercent)
    }

    @Test
    fun daily_returnsSevenBucketsMondayToSunday() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = emptyList(),
            expenses = listOf(expense(150.0, LocalDate.of(2026, 6, 17))), // Wednesday that week
            monthlyGoal = null,
            period = AnalysisPeriod.DAILY,
            anchorDate = anchor
        )
        assertEquals(7, snapshot.buckets.size)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), snapshot.buckets.map { it.label })
        assertEquals(150.0, snapshot.buckets[2].expense, 0.0) // Wed bucket
        assertEquals(0.0, snapshot.buckets[0].expense, 0.0) // Mon bucket unaffected
    }

    @Test
    fun weekly_splitsMonthIntoFourBucketsUsingProvidedLabels() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = listOf(income(400.0, LocalDate.of(2026, 6, 10))), // falls in week 2 (8th-14th)
            expenses = emptyList(),
            monthlyGoal = null,
            period = AnalysisPeriod.WEEKLY,
            anchorDate = anchor,
            weekLabels = listOf("W1", "W2", "W3", "W4")
        )
        assertEquals(listOf("W1", "W2", "W3", "W4"), snapshot.buckets.map { it.label })
        assertEquals(400.0, snapshot.buckets[1].income, 0.0)
    }

    @Test
    fun weekly_lastBucket_extendsToTheEndOfTheMonth() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = listOf(income(50.0, LocalDate.of(2026, 6, 30))), // last day of June
            expenses = emptyList(),
            monthlyGoal = null,
            period = AnalysisPeriod.WEEKLY,
            anchorDate = anchor
        )
        assertEquals(50.0, snapshot.buckets.last().income, 0.0)
    }

    @Test
    fun monthly_returnsSixMonthsEndingAtAnchorMonth() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = emptyList(),
            expenses = listOf(expense(200.0, anchor)),
            monthlyGoal = null,
            period = AnalysisPeriod.MONTHLY,
            anchorDate = anchor
        )
        assertEquals(6, snapshot.buckets.size)
        assertEquals("Jan", snapshot.buckets.first().label)
        assertEquals("Jun", snapshot.buckets.last().label)
        assertEquals(200.0, snapshot.buckets.last().expense, 0.0)
    }

    @Test
    fun yearly_returnsFiveYearsEndingAtAnchorYear() {
        val snapshot = AnalysisCalculator.snapshot(
            incomes = emptyList(),
            expenses = listOf(expense(75.0, anchor)),
            monthlyGoal = null,
            period = AnalysisPeriod.YEARLY,
            anchorDate = anchor
        )
        assertEquals(5, snapshot.buckets.size)
        assertEquals(listOf("2022", "2023", "2024", "2025", "2026"), snapshot.buckets.map { it.label })
        assertEquals(75.0, snapshot.buckets.last().expense, 0.0)
    }
}
