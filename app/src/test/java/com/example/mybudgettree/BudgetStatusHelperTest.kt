package com.example.mybudgettree

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetStatusHelperTest {

    // --- level(spent, minGoal, maxGoal) — used when a real Monthly Goal minimum exists ---

    @Test
    fun level_noGoalsSet_isNone() {
        assertEquals(BudgetLevel.NONE, BudgetStatusHelper.level(spent = 50.0, minGoal = 0.0, maxGoal = 0.0))
    }

    @Test
    fun level_underMinimum_isGood() {
        assertEquals(BudgetLevel.GOOD, BudgetStatusHelper.level(spent = 500.0, minGoal = 1000.0, maxGoal = 2000.0))
    }

    @Test
    fun level_overMinimumUnderMaximum_isWarning() {
        assertEquals(BudgetLevel.WARNING, BudgetStatusHelper.level(spent = 1500.0, minGoal = 1000.0, maxGoal = 2000.0))
    }

    @Test
    fun level_overMaximum_isDanger() {
        assertEquals(BudgetLevel.DANGER, BudgetStatusHelper.level(spent = 2500.0, minGoal = 1000.0, maxGoal = 2000.0))
    }

    @Test
    fun level_exactlyAtMinimum_isNotYetWarning() {
        // crossing is defined as strictly greater than, not equal to
        assertEquals(BudgetLevel.GOOD, BudgetStatusHelper.level(spent = 1000.0, minGoal = 1000.0, maxGoal = 2000.0))
    }

    @Test
    fun level_exactlyAtMaximum_isNotYetDanger() {
        assertEquals(BudgetLevel.WARNING, BudgetStatusHelper.level(spent = 2000.0, minGoal = 1000.0, maxGoal = 2000.0))
    }

    @Test
    fun level_maxOnlyNoMin_overMax_isDanger() {
        assertEquals(BudgetLevel.DANGER, BudgetStatusHelper.level(spent = 300.0, minGoal = 0.0, maxGoal = 200.0))
    }

    // --- levelForPercent(percent, hasTarget) — used for a category's own budget (no minimum) ---

    @Test
    fun levelForPercent_noTarget_isNone() {
        assertEquals(BudgetLevel.NONE, BudgetStatusHelper.levelForPercent(percent = 150, hasTarget = false))
    }

    @Test
    fun levelForPercent_under70_isGood() {
        assertEquals(BudgetLevel.GOOD, BudgetStatusHelper.levelForPercent(percent = 50, hasTarget = true))
    }

    @Test
    fun levelForPercent_over70Under100_isWarning() {
        assertEquals(BudgetLevel.WARNING, BudgetStatusHelper.levelForPercent(percent = 85, hasTarget = true))
    }

    @Test
    fun levelForPercent_over100_isDanger() {
        assertEquals(BudgetLevel.DANGER, BudgetStatusHelper.levelForPercent(percent = 120, hasTarget = true))
    }

    @Test
    fun levelForPercent_exactly70_isNotYetWarning() {
        assertEquals(BudgetLevel.GOOD, BudgetStatusHelper.levelForPercent(percent = 70, hasTarget = true))
    }

    @Test
    fun levelForPercent_exactly100_isNotYetDanger() {
        assertEquals(BudgetLevel.WARNING, BudgetStatusHelper.levelForPercent(percent = 100, hasTarget = true))
    }

    // --- contrastingTextColor(backgroundColor) — luminance-based white/dark text pick ---

    @Test
    fun contrastingTextColor_black_returnsWhite() {
        assertEquals(-0x1, BudgetStatusHelper.contrastingTextColor(0xFF000000.toInt()))
    }

    @Test
    fun contrastingTextColor_white_returnsDarkText() {
        assertEquals(-15060184, BudgetStatusHelper.contrastingTextColor(0xFFFFFFFF.toInt()))
    }

    @Test
    fun contrastingTextColor_lightBlue_returnsDarkText() {
        // this is the exact case that was previously an app bug: white text was hardcoded
        // on top of the light analysis_progress_blue fill, making it unreadable
        val lightBlue = 0xFF7EC8F8.toInt()
        assertEquals(-15060184, BudgetStatusHelper.contrastingTextColor(lightBlue))
    }

    @Test
    fun contrastingTextColor_darkRed_returnsWhite() {
        val destructiveRed = 0xFFB3452C.toInt()
        assertEquals(-0x1, BudgetStatusHelper.contrastingTextColor(destructiveRed))
    }
}
