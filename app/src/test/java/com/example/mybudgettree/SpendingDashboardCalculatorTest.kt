package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SpendingDashboardCalculatorTest {

    private val from = LocalDate.of(2026, 10, 1)
    private val to = LocalDate.of(2026, 10, 31)

    private fun category(id: String, name: String = id) = Category(id = id, categoryName = name)

    private fun expense(categoryId: String, amount: Double, date: LocalDate) = Expense(
        id = "$categoryId-$amount-$date",
        categoryId = categoryId,
        description = "test",
        amount = amount,
        date = date.toString(),
        startTime = "09:00",
        endTime = "09:00"
    )

    private fun totals(
        expenses: List<Expense>,
        categories: List<Category>,
        min: Double? = null,
        max: Double? = null
    ) = SpendingDashboardCalculator.categoryTotals(expenses, categories, from, to, min, max)

    // ---- categoryTotals

    @Test
    fun categoryTotals_addsUpEachCategorysSpending() {
        val result = totals(
            expenses = listOf(
                expense("food", 100.0, from),
                expense("food", 50.0, from.plusDays(3)),
                expense("fuel", 200.0, from)
            ),
            categories = listOf(category("food"), category("fuel"))
        )
        assertEquals(mapOf("food" to 150.0, "fuel" to 200.0), result.associate { it.category.id to it.total })
    }

    @Test
    fun categoryTotals_listsCategoriesWithNoSpendingAsZero() {
        val result = totals(emptyList(), listOf(category("food"), category("fuel")))
        assertEquals(listOf(0.0, 0.0), result.map { it.total })
    }

    @Test
    fun categoryTotals_ignoresExpensesOutsideThePeriod() {
        val result = totals(
            expenses = listOf(
                expense("food", 10.0, from.minusDays(1)),
                expense("food", 20.0, from),
                expense("food", 30.0, to),
                expense("food", 40.0, to.plusDays(1))
            ),
            categories = listOf(category("food"))
        )
        assertEquals(50.0, result.single().total, 0.0)
    }

    @Test
    fun categoryTotals_ignoresExpensesOfCategoriesThatAreNotListed() {
        val result = totals(listOf(expense("gone", 999.0, from)), listOf(category("food")))
        assertEquals(0.0, result.single().total, 0.0)
    }

    @Test
    fun categoryTotals_mostSpendingFirst_tiesByName() {
        val result = totals(
            expenses = listOf(expense("b", 50.0, from), expense("a", 50.0, from), expense("c", 80.0, from)),
            categories = listOf(category("a", "Apples"), category("b", "bananas"), category("c", "Cherries"))
        )
        assertEquals(listOf("c", "a", "b"), result.map { it.category.id })
    }

    @Test
    fun categoryTotals_belowTheMinimum_isGreen() {
        val item = totals(listOf(expense("food", 99.0, from)), listOf(category("food")), min = 100.0, max = 200.0).single()
        assertEquals(BudgetLevel.GOOD, item.level)
        assertNull(item.overBy)
    }

    @Test
    fun categoryTotals_exactlyTheMinimum_staysGreen() {
        val item = totals(listOf(expense("food", 100.0, from)), listOf(category("food")), min = 100.0, max = 200.0).single()
        assertEquals(BudgetLevel.GOOD, item.level)
    }

    @Test
    fun categoryTotals_aboveTheMinimum_isOrange_andSaysByHowMuch() {
        val item = totals(listOf(expense("food", 130.0, from)), listOf(category("food")), min = 100.0, max = 200.0).single()
        assertEquals(BudgetLevel.WARNING, item.level)
        assertEquals(30.0, item.overBy!!, 0.0)
    }

    @Test
    fun categoryTotals_exactlyTheMaximum_staysOrange() {
        val item = totals(listOf(expense("food", 200.0, from)), listOf(category("food")), min = 100.0, max = 200.0).single()
        assertEquals(BudgetLevel.WARNING, item.level)
    }

    @Test
    fun categoryTotals_aboveTheMaximum_isRed_andSaysByHowMuch() {
        val item = totals(listOf(expense("food", 250.0, from)), listOf(category("food")), min = 100.0, max = 200.0).single()
        assertEquals(BudgetLevel.DANGER, item.level)
        assertEquals(50.0, item.overBy!!, 0.0)
    }

    @Test
    fun categoryTotals_onlyAMaximum_isGreenThenRed() {
        val expenses = listOf(expense("low", 150.0, from), expense("high", 250.0, from))
        val result = totals(expenses, listOf(category("low"), category("high")), min = null, max = 200.0)
        assertEquals(BudgetLevel.GOOD, result.first { it.category.id == "low" }.level)
        assertEquals(BudgetLevel.DANGER, result.first { it.category.id == "high" }.level)
    }

    @Test
    fun categoryTotals_onlyAMinimum_isGreenThenOrange_neverRed() {
        val expenses = listOf(expense("low", 50.0, from), expense("high", 5000.0, from))
        val result = totals(expenses, listOf(category("low"), category("high")), min = 100.0, max = null)
        assertEquals(BudgetLevel.GOOD, result.first { it.category.id == "low" }.level)
        assertEquals(BudgetLevel.WARNING, result.first { it.category.id == "high" }.level)
    }

    @Test
    fun categoryTotals_noMinimumOrMaximum_everythingStaysGreen() {
        val item = totals(listOf(expense("food", 9999.0, from)), listOf(category("food"))).single()
        assertEquals(BudgetLevel.NONE, item.level)
        assertNull(item.overBy)
    }

    // ---- dailySpending

    @Test
    fun dailySpending_hasOneEntryForEveryDayIncludingBothEnds() {
        val days = SpendingDashboardCalculator.dailySpending(emptyList(), from, from.plusDays(6))
        assertEquals(7, days.size)
        assertEquals(from, days.first().date)
        assertEquals(from.plusDays(6), days.last().date)
    }

    @Test
    fun dailySpending_aSingleDayPeriodHasOneEntry() {
        assertEquals(1, SpendingDashboardCalculator.dailySpending(emptyList(), from, from).size)
    }

    @Test
    fun dailySpending_daysWithNothingSpentAreZero() {
        val days = SpendingDashboardCalculator.dailySpending(listOf(expense("food", 40.0, from.plusDays(2))), from, from.plusDays(4))
        assertEquals(listOf(0.0, 0.0, 40.0, 0.0, 0.0), days.map { it.amount })
    }

    @Test
    fun dailySpending_addsUpSeveralExpensesOnTheSameDay() {
        val days = SpendingDashboardCalculator.dailySpending(
            listOf(expense("food", 10.0, from), expense("food", 15.0, from)),
            from,
            from.plusDays(1)
        )
        assertEquals(25.0, days.first().amount, 0.0)
    }

    @Test
    fun dailySpending_runningTotalGrowsDayByDay() {
        val days = SpendingDashboardCalculator.dailySpending(
            listOf(expense("food", 10.0, from), expense("food", 30.0, from.plusDays(2))),
            from,
            from.plusDays(3)
        )
        assertEquals(listOf(10.0, 10.0, 40.0, 40.0), days.map { it.runningTotal })
    }

    @Test
    fun dailySpending_lastRunningTotalIsTheWholePeriodsTotal() {
        val expenses = listOf(expense("food", 10.0, from), expense("food", 30.0, from.plusDays(5)))
        val days = SpendingDashboardCalculator.dailySpending(expenses, from, to)
        assertEquals(40.0, days.last().runningTotal, 0.0)
    }

    @Test
    fun dailySpending_ignoresExpensesOutsideThePeriod() {
        val days = SpendingDashboardCalculator.dailySpending(
            listOf(expense("food", 99.0, from.minusDays(1)), expense("food", 99.0, to.plusDays(1))),
            from,
            to
        )
        assertEquals(0.0, days.last().runningTotal, 0.0)
    }

    @Test
    fun dailySpending_startAfterEnd_isEmpty() {
        assertTrue(SpendingDashboardCalculator.dailySpending(emptyList(), to, from).isEmpty())
    }

    @Test
    fun dailySpending_crossesMonthAndYearEnds() {
        val days = SpendingDashboardCalculator.dailySpending(emptyList(), LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2))
        assertEquals(
            listOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2)),
            days.map { it.date }
        )
    }

    // ---- validate

    private fun validate(min: String = "", max: String = "", f: LocalDate = from, t: LocalDate = to) =
        SpendingDashboardCalculator.validate(min, max, f, t)

    @Test
    fun validate_blankMinimumAndMaximum_isFine() {
        val input = validate()
        assertTrue(input.isValid)
        assertNull(input.min)
        assertNull(input.max)
    }

    @Test
    fun validate_readsAmountsWithTheRAndCommasPeopleType() {
        val input = validate(min = "R1,000", max = " r2 500.50 ")
        assertTrue(input.isValid)
        assertEquals(1000.0, input.min!!, 0.0)
        assertEquals(2500.5, input.max!!, 0.0)
    }

    @Test
    fun validate_zeroIsAllowed() {
        assertTrue(validate(min = "0", max = "0").isValid)
    }

    @Test
    fun validate_textThatIsNotANumber_isRejected() {
        assertEquals(setOf(InputProblem.MIN_NOT_A_NUMBER), validate(min = "abc").problems)
        assertEquals(setOf(InputProblem.MAX_NOT_A_NUMBER), validate(max = "12x").problems)
        assertEquals(setOf(InputProblem.MIN_NOT_A_NUMBER), validate(min = "NaN").problems)
        assertEquals(setOf(InputProblem.MAX_NOT_A_NUMBER), validate(max = "Infinity").problems)
    }

    @Test
    fun validate_negativeAmounts_areRejected() {
        assertEquals(setOf(InputProblem.MIN_NEGATIVE), validate(min = "-5").problems)
        assertEquals(setOf(InputProblem.MAX_NEGATIVE), validate(max = "-5").problems)
    }

    @Test
    fun validate_minimumAboveMaximum_isRejected() {
        assertEquals(setOf(InputProblem.MIN_ABOVE_MAX), validate(min = "200", max = "100").problems)
    }

    @Test
    fun validate_minimumEqualToMaximum_isAllowed() {
        assertTrue(validate(min = "100", max = "100").isValid)
    }

    @Test
    fun validate_onlyOneOfMinimumAndMaximum_isNeverCompared() {
        assertTrue(validate(min = "500").isValid)
        assertTrue(validate(max = "10").isValid)
    }

    @Test
    fun validate_startAfterEnd_isRejected() {
        assertEquals(setOf(InputProblem.FROM_AFTER_TO), validate(f = to, t = from).problems)
    }

    @Test
    fun validate_oneDayPeriod_isFine() {
        assertTrue(validate(f = from, t = from).isValid)
    }

    @Test
    fun validate_periodOfExactlyTheLimit_isFine_andOneDayMoreIsNot() {
        val start = LocalDate.of(2026, 1, 1)
        val lastAllowed = start.plusDays(SpendingDashboardCalculator.MAX_PERIOD_DAYS - 1)
        assertTrue(validate(f = start, t = lastAllowed).isValid)
        assertEquals(setOf(InputProblem.PERIOD_TOO_LONG), validate(f = start, t = lastAllowed.plusDays(1)).problems)
    }

    @Test
    fun validate_reportsEveryProblemAtOnce() {
        val input = validate(min = "abc", max = "-1", f = to, t = from)
        assertFalse(input.isValid)
        assertEquals(
            setOf(InputProblem.MIN_NOT_A_NUMBER, InputProblem.MAX_NEGATIVE, InputProblem.FROM_AFTER_TO),
            input.problems
        )
    }

    // ---- presets

    @Test
    fun preset_thisMonth_isTheFirstOfTheMonthUpToToday() {
        val (start, end) = DashboardPreset.THIS_MONTH.range(LocalDate.of(2026, 10, 14))
        assertEquals(LocalDate.of(2026, 10, 1), start)
        assertEquals(LocalDate.of(2026, 10, 14), end)
    }

    @Test
    fun preset_thisMonth_onTheFirstIsJustOneDay() {
        val (start, end) = DashboardPreset.THIS_MONTH.range(LocalDate.of(2026, 10, 1))
        assertEquals(start, end)
    }

    @Test
    fun preset_last7Days_isTodayAndTheSixDaysBefore() {
        val (start, end) = DashboardPreset.LAST_7_DAYS.range(LocalDate.of(2026, 10, 14))
        assertEquals(LocalDate.of(2026, 10, 8), start)
        assertEquals(LocalDate.of(2026, 10, 14), end)
        assertEquals(7, SpendingDashboardCalculator.dailySpending(emptyList(), start, end).size)
    }

    @Test
    fun preset_last7Days_crossesIntoTheLastMonth() {
        val (start, _) = DashboardPreset.LAST_7_DAYS.range(LocalDate.of(2026, 10, 3))
        assertEquals(LocalDate.of(2026, 9, 27), start)
    }

    @Test
    fun presets_areAlwaysValidPeriods() {
        listOf(DashboardPreset.THIS_MONTH, DashboardPreset.LAST_7_DAYS).forEach { preset ->
            val (start, end) = preset.range(LocalDate.of(2026, 3, 31))
            assertTrue(SpendingDashboardCalculator.validate("", "", start, end).isValid)
        }
    }
}
