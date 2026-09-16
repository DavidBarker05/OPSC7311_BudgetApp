package com.example.mybudgettree

import android.app.Activity
import android.view.View

enum class BudgetLevel { GOOD, WARNING, DANGER, NONE }

/**
 * Shared status/color logic for every budget progress bar in the app. When a real
 * minimum is known (the overall Monthly Goal), status is based on the actual spent
 * amount crossing the minimum/maximum. Where only a single target is known (a
 * category's own budget, which has no minimum), status falls back to percent
 * thresholds instead.
 */
object BudgetStatusHelper {
    fun level(spent: Double, minGoal: Double, maxGoal: Double): BudgetLevel {
        if (maxGoal <= 0.0 && minGoal <= 0.0) return BudgetLevel.NONE
        return when {
            maxGoal > 0.0 && spent > maxGoal -> BudgetLevel.DANGER
            minGoal > 0.0 && spent > minGoal -> BudgetLevel.WARNING
            else -> BudgetLevel.GOOD
        }
    }

    fun levelForPercent(percent: Int, hasTarget: Boolean): BudgetLevel {
        if (!hasTarget) return BudgetLevel.NONE
        return when {
            percent > 100 -> BudgetLevel.DANGER
            percent > 70 -> BudgetLevel.WARNING
            else -> BudgetLevel.GOOD
        }
    }

    fun colorRes(level: BudgetLevel): Int = when (level) {
        BudgetLevel.DANGER -> R.color.destructive_red
        BudgetLevel.WARNING -> R.color.warning_orange
        BudgetLevel.GOOD, BudgetLevel.NONE -> R.color.analysis_progress_blue
    }

    /**
     * @param hasMinGoal True when [level] was computed via [level] (a real minimum/maximum),
     * false when it was computed via [levelForPercent] (a single target with no minimum,
     * e.g. a category's own budget) — the two cases need different wording.
     */
    fun statusText(activity: Activity, level: BudgetLevel, percent: Int, hasMinGoal: Boolean): String = when (level) {
        BudgetLevel.NONE -> activity.getString(R.string.expenses_status_no_budget)
        BudgetLevel.DANGER -> activity.getString(
            if (hasMinGoal) R.string.expenses_status_over_max else R.string.expenses_status_over, percent
        )
        BudgetLevel.WARNING -> activity.getString(
            if (hasMinGoal) R.string.expenses_status_over_min else R.string.expenses_status_watch, percent
        )
        BudgetLevel.GOOD -> activity.getString(R.string.expenses_status_good, percent)
    }

    fun tintFill(fill: View, activity: Activity, level: BudgetLevel) {
        fill.background?.mutate()?.setTint(activity.getColor(colorRes(level)))
    }
}
