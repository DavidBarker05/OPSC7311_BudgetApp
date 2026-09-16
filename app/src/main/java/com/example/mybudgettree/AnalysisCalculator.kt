package com.example.mybudgettree

import android.content.Context
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import com.example.mybudgettree.database.entries.MonthlyGoal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class AnalysisPeriod { DAILY, WEEKLY, MONTHLY, YEARLY }

data class AnalysisBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

data class AnalysisSnapshot(
    val totalBalance: Double,
    val totalExpense: Double,
    val periodIncome: Double,
    val periodExpense: Double,
    val budgetGoal: Double,
    val minGoal: Double,
    val monthExpense: Double,
    val expensePercent: Int,
    val buckets: List<AnalysisBucket>
)

object AnalysisCalculator {
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    fun snapshot(
        context: Context,
        incomes: List<Income>,
        expenses: List<Expense>,
        monthlyGoal: MonthlyGoal?,
        period: AnalysisPeriod,
        anchorDate: LocalDate
    ): AnalysisSnapshot {
        val totalIncome = incomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val budgetGoal = monthlyGoal?.maxGoal ?: 0.0
        val minGoal = monthlyGoal?.minGoal ?: 0.0
        val anchorMonth = YearMonth.from(anchorDate)
        val monthExpense = expenses.filter { YearMonth.from(it.date) == anchorMonth }.sumOf { it.amount }
        val ranges = ranges(context, period, anchorDate)
        val buckets = ranges.map { range ->
            AnalysisBucket(
                label = range.label,
                income = incomes.filter { it.date in range.start..range.end }.sumOf { it.amount },
                expense = expenses.filter { it.date in range.start..range.end }.sumOf { it.amount }
            )
        }
        val periodIncome = buckets.sumOf { it.income }
        val periodExpense = buckets.sumOf { it.expense }

        return AnalysisSnapshot(
            totalBalance = totalIncome - totalExpense,
            totalExpense = totalExpense,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            budgetGoal = budgetGoal,
            minGoal = minGoal,
            monthExpense = monthExpense,
            expensePercent = percent(monthExpense, budgetGoal, capAtHundred = false),
            buckets = buckets
        )
    }

    private fun percent(part: Double, whole: Double, capAtHundred: Boolean = true): Int {
        if (whole <= 0.0) return 0
        val value = ((part / whole) * 100.0).toInt().coerceAtLeast(0)
        return if (capAtHundred) value.coerceAtMost(100) else value
    }

    private data class DateRange(val label: String, val start: LocalDate, val end: LocalDate)

    private fun ranges(context: Context, period: AnalysisPeriod, anchor: LocalDate): List<DateRange> {
        return when (period) {
            AnalysisPeriod.DAILY -> {
                val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                (0..6).map { offset ->
                    val date = monday.plusDays(offset.toLong())
                    DateRange(
                        label = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                        start = date,
                        end = date
                    )
                }
            }
            AnalysisPeriod.WEEKLY -> {
                val month = YearMonth.from(anchor)
                val start = month.atDay(1)
                val end = month.atEndOfMonth()
                val labels = listOf(
                    context.getString(R.string.week_1),
                    context.getString(R.string.week_2),
                    context.getString(R.string.week_3),
                    context.getString(R.string.week_4)
                )
                labels.mapIndexed { index, label ->
                    val rangeStart = start.plusDays(index * 7L).coerceAtMost(end)
                    val rangeEnd = if (index == 3) end else rangeStart.plusDays(6).coerceAtMost(end)
                    DateRange(label, rangeStart, rangeEnd)
                }
            }
            AnalysisPeriod.MONTHLY -> {
                val current = YearMonth.from(anchor)
                (5 downTo 0).map { offset ->
                    val month = current.minusMonths(offset.toLong())
                    DateRange(
                        label = monthFormatter.format(month.atDay(1)),
                        start = month.atDay(1),
                        end = month.atEndOfMonth()
                    )
                }
            }
            AnalysisPeriod.YEARLY -> {
                val currentYear = anchor.year
                (4 downTo 0).map { offset ->
                    val year = currentYear - offset
                    DateRange(
                        label = year.toString(),
                        start = LocalDate.of(year, 1, 1),
                        end = LocalDate.of(year, 12, 31)
                    )
                }
            }
        }
    }
}
