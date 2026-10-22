package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One category's spending over the dashboard's period, and how it compares with the minimum and maximum
 *
 * @property category The category
 * @property total Everything spent in the category over the period
 * @property level GOOD or NONE (green) at or below the minimum, WARNING (orange) above the minimum, DANGER (red) above
 * the maximum
 * @property overBy How far past the threshold the total is: past the maximum for DANGER, past the minimum for WARNING.
 * Null while the total is within the limits
 */
data class CategorySpending(
    val category: Category,
    val total: Double,
    val level: BudgetLevel,
    val overBy: Double?
)

/**
 * One day of a category's spending
 *
 * @property date The day
 * @property amount What was spent on that day, zero if nothing was
 * @property runningTotal What was spent from the first day of the period up to and including this day
 */
data class DailySpending(
    val date: LocalDate,
    val amount: Double,
    val runningTotal: Double
)

/**
 * The quick choices for the period
 */
enum class DashboardPreset {
    /** From the first day of the current month up to today */
    THIS_MONTH,

    /** Today and the six days before it */
    LAST_7_DAYS;

    /**
     * @param today The date to count from
     * @return The first and last day of the period, both included
     */
    fun range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        THIS_MONTH -> today.withDayOfMonth(1) to today
        LAST_7_DAYS -> today.minusDays(6) to today
    }
}

/**
 * What can be wrong with what was entered on the dashboard
 */
enum class InputProblem {
    MIN_NOT_A_NUMBER,
    MAX_NOT_A_NUMBER,
    MIN_NEGATIVE,
    MAX_NEGATIVE,
    MIN_ABOVE_MAX,
    FROM_AFTER_TO,
    PERIOD_TOO_LONG
}

/**
 * What was entered, read as numbers and checked
 *
 * @property min The minimum, or null if it was left blank
 * @property max The maximum, or null if it was left blank
 * @property problems Everything wrong with the input, empty when it can be used
 */
data class DashboardInput(
    val min: Double?,
    val max: Double?,
    val from: LocalDate,
    val to: LocalDate,
    val problems: Set<InputProblem>
) {
    val isValid: Boolean get() = problems.isEmpty()
}

/**
 * The calculations behind the spending dashboard. They are plain functions of their arguments, so they can be tested
 * without a screen
 */
object SpendingDashboardCalculator {
    /** The longest period the dashboard accepts, in days, so the graph stays readable and the query small */
    const val MAX_PERIOD_DAYS = 366L

    /**
     * Reads and checks what was entered. A minimum or maximum may be left blank, and the "R" and commas people type in
     * amounts are ignored, as on the other screens
     *
     * @param minText The text of the minimum field
     * @param maxText The text of the maximum field
     * @param from The first day of the period
     * @param to The last day of the period
     * @return The numbers read, with every problem found
     */
    fun validate(minText: String, maxText: String, from: LocalDate, to: LocalDate): DashboardInput {
        val problems = mutableSetOf<InputProblem>()
        val min = readAmount(minText, InputProblem.MIN_NOT_A_NUMBER, InputProblem.MIN_NEGATIVE, problems)
        val max = readAmount(maxText, InputProblem.MAX_NOT_A_NUMBER, InputProblem.MAX_NEGATIVE, problems)
        if (min != null && max != null && min > max) problems += InputProblem.MIN_ABOVE_MAX
        if (from.isAfter(to)) {
            problems += InputProblem.FROM_AFTER_TO
        } else if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_PERIOD_DAYS) {
            problems += InputProblem.PERIOD_TOO_LONG
        }
        return DashboardInput(min, max, from, to, problems)
    }

    private fun readAmount(
        text: String,
        notANumber: InputProblem,
        negative: InputProblem,
        problems: MutableSet<InputProblem>
    ): Double? {
        val cleaned = text.trim().replace("R", "", ignoreCase = true).replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val amount = cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
        if (amount == null) {
            problems += notANumber
            return null
        }
        if (amount < 0.0) {
            problems += negative
            return null
        }
        return amount
    }

    /**
     * Adds up each category's spending over the period and colours it against the minimum and maximum. A minimum or
     * maximum that is null or zero counts as not set, as it does for every budget bar in the app
     *
     * @param expenses The expenses to count. Any outside the period are ignored
     * @param categories Every category to list, including those with no spending
     * @param from The first day of the period
     * @param to The last day of the period
     * @param min The minimum, or null
     * @param max The maximum, or null
     * @return One entry per category, the one with the most spending first. Ties are in name order
     */
    fun categoryTotals(
        expenses: List<Expense>,
        categories: List<Category>,
        from: LocalDate,
        to: LocalDate,
        min: Double?,
        max: Double?
    ): List<CategorySpending> {
        val totals = expenses
            .filter { it.dateAsLocalDate() in from..to }
            .groupBy { it.categoryId }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
        return categories
            .map { category ->
                val total = totals[category.id] ?: 0.0
                val level = BudgetStatusHelper.level(total, min ?: 0.0, max ?: 0.0)
                val overBy = when (level) {
                    BudgetLevel.DANGER -> total - (max ?: 0.0)
                    BudgetLevel.WARNING -> total - (min ?: 0.0)
                    else -> null
                }
                CategorySpending(category, total, level, overBy)
            }
            .sortedWith(compareByDescending<CategorySpending> { it.total }.thenBy { it.category.categoryName.lowercase() })
    }

    /**
     * One category's spending for every day of the period, days with nothing spent included
     *
     * @param expenses The category's expenses. Any outside the period are ignored
     * @param from The first day of the period
     * @param to The last day of the period
     * @return One entry per day from [from] to [to], oldest first, with the running total
     */
    fun dailySpending(expenses: List<Expense>, from: LocalDate, to: LocalDate): List<DailySpending> {
        if (from.isAfter(to)) return emptyList()
        val perDay = expenses.groupBy { it.dateAsLocalDate() }.mapValues { (_, items) -> items.sumOf { it.amount } }
        var running = 0.0
        return generateSequence(from) { day -> day.plusDays(1).takeIf { !it.isAfter(to) } }
            .map { day ->
                val amount = perDay[day] ?: 0.0
                running += amount
                DailySpending(day, amount, running)
            }
            .toList()
    }
}
