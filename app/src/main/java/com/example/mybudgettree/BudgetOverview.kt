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
     * all-time [incomes]/[expenses], while [spent] vs [budgetTarget] drive the percent bar
     * so callers can scope the bar to the current month, a single category, etc.
     */
    fun bind(
        activity: Activity,
        incomes: List<Income>,
        expenses: List<Expense>,
        spent: Double,
        budgetTarget: Double
    ) {
        val totalIncome = incomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val percent = if (budgetTarget <= 0.0) {
            0
        } else {
            ((spent / budgetTarget) * 100.0).toInt().coerceAtLeast(0)
        }
        activity.findViewById<TextView>(R.id.tvTotalBalance).text =
            MoneyFormatter.format(totalIncome - totalExpense)
        activity.findViewById<TextView>(R.id.tvTotalExpense).text =
            MoneyFormatter.formatSigned(totalExpense, isIncome = false)
        activity.findViewById<TextView>(R.id.tvBudgetGoal).text = MoneyFormatter.format(budgetTarget)
        activity.findViewById<TextView>(R.id.tvBudgetPercent).text =
            activity.getString(R.string.budget_percent, percent.coerceAtMost(100))
        activity.findViewById<TextView>(R.id.tvExpenseStatus).text = statusText(activity, budgetTarget, percent)
        val fill = activity.findViewById<View>(R.id.budgetFill)
        val params = fill.layoutParams as ConstraintLayout.LayoutParams
        params.matchConstraintPercentWidth = (percent / 100f).coerceIn(0f, 1f)
        fill.layoutParams = params
    }

    private fun statusText(activity: Activity, budgetTarget: Double, percent: Int): String {
        if (budgetTarget <= 0.0) return activity.getString(R.string.expenses_status_no_budget)
        val res = when {
            percent > 100 -> R.string.expenses_status_over
            percent > 70 -> R.string.expenses_status_watch
            else -> R.string.expenses_status_good
        }
        return activity.getString(res, percent)
    }
}
