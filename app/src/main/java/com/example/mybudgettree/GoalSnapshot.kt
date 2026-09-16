package com.example.mybudgettree

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.MonthlyGoal
import java.time.LocalDate
import java.time.YearMonth

data class GoalSnapshot(
    val goalPercent: Int,
    val budgetGoal: Double,
    val minGoal: Double,
    val spentThisMonth: Double
) {
    companion object {
        fun from(
            expenses: List<Expense>,
            monthlyGoal: MonthlyGoal?,
            today: LocalDate = LocalDate.now()
        ): GoalSnapshot {
            val budgetGoal = monthlyGoal?.maxGoal ?: 0.0
            val minGoal = monthlyGoal?.minGoal ?: 0.0
            val currentMonth = YearMonth.from(today)
            val spentThisMonth = expenses.filter { YearMonth.from(it.date) == currentMonth }.sumOf { it.amount }
            val goalPercent = if (budgetGoal <= 0.0) {
                0
            } else {
                ((spentThisMonth / budgetGoal) * 100.0).toInt().coerceIn(0, 100)
            }
            return GoalSnapshot(goalPercent, budgetGoal, minGoal, spentThisMonth)
        }

        fun bindProgress(activity: Activity, snapshot: GoalSnapshot) {
            val tvBudgetPercent = activity.findViewById<TextView>(R.id.tvBudgetPercent)
            val tvBudgetGoal = activity.findViewById<TextView>(R.id.tvBudgetGoal)
            tvBudgetPercent.text = activity.getString(R.string.budget_percent, snapshot.goalPercent)
            tvBudgetGoal.text = MoneyFormatter.format(snapshot.budgetGoal)
            val fill = activity.findViewById<android.view.View>(R.id.budgetFill)
            val params = fill.layoutParams as ConstraintLayout.LayoutParams
            params.matchConstraintPercentWidth = (snapshot.goalPercent / 100f).coerceIn(0f, 1f)
            fill.layoutParams = params
            val level = BudgetStatusHelper.level(snapshot.spentThisMonth, snapshot.minGoal, snapshot.budgetGoal)
            BudgetStatusHelper.tintFill(fill, activity, level)
            tvBudgetPercent.setTextColor(
                BudgetStatusHelper.contrastingTextColor(activity.getColor(BudgetStatusHelper.colorRes(level)))
            )
            tvBudgetGoal.setTextColor(
                BudgetStatusHelper.contrastingTextColor(activity.getColor(R.color.budget_track))
            )
        }

        fun bindDrops(drops: List<ImageView>, filled: Int) {
            drops.forEachIndexed { index, image ->
                image.alpha = if (index < filled) 1f else 0.28f
            }
        }
    }
}
