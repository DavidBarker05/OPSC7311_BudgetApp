package com.example.mybudgettree

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income

object BudgetOverview {
    /**
     * Binds the shared garden-header component: Total Balance/Total Expense always reflect
     * the [incomes]/[expenses] the caller passes in (scoped to whatever period/category makes
     * sense there), while [spent] vs [budgetTarget] drive the percent bar. When [minGoal] is
     * a real minimum (the overall Monthly Goal), the bar's color/status reflects crossing the
     * minimum (warning) or maximum (danger); otherwise it falls back to percent thresholds
     * (used for a single category's own budget, which has no minimum).
     */
    fun bind(
        activity: Activity,
        incomes: List<Income>,
        expenses: List<Expense>,
        spent: Double,
        budgetTarget: Double,
        minGoal: Double = 0.0
    ) {
        val totalIncome = incomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val percent = if (budgetTarget <= 0.0) {
            0
        } else {
            ((spent / budgetTarget) * 100.0).toInt().coerceAtLeast(0)
        }
        val hasMinGoal = minGoal > 0.0
        val level = if (hasMinGoal) {
            BudgetStatusHelper.level(spent, minGoal, budgetTarget)
        } else {
            BudgetStatusHelper.levelForPercent(percent, budgetTarget > 0.0)
        }
        activity.findViewById<TextView>(R.id.tvTotalBalance).text =
            MoneyFormatter.format(totalIncome - totalExpense)
        activity.findViewById<TextView>(R.id.tvTotalExpense).text =
            MoneyFormatter.formatSigned(totalExpense, isIncome = false)
        val tvBudgetGoal = activity.findViewById<TextView>(R.id.tvBudgetGoal)
        val tvBudgetPercent = activity.findViewById<TextView>(R.id.tvBudgetPercent)
        tvBudgetGoal.text = MoneyFormatter.format(budgetTarget)
        tvBudgetPercent.text = activity.getString(R.string.budget_percent, percent.coerceAtMost(100))
        activity.findViewById<TextView>(R.id.tvExpenseStatus).text =
            BudgetStatusHelper.statusText(activity, level, percent, hasMinGoal)
        val fill = activity.findViewById<View>(R.id.budgetFill)
        val params = fill.layoutParams as ConstraintLayout.LayoutParams
        params.matchConstraintPercentWidth = (percent / 100f).coerceIn(0f, 1f)
        fill.layoutParams = params
        BudgetStatusHelper.tintFill(fill, activity, level)
        tvBudgetPercent.setTextColor(
            BudgetStatusHelper.contrastingTextColor(activity.getColor(BudgetStatusHelper.colorRes(level)))
        )
        tvBudgetGoal.setTextColor(
            BudgetStatusHelper.contrastingTextColor(activity.getColor(R.color.budget_track))
        )
    }
}
