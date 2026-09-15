package com.example.mybudgettree

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class GoalSnapshot(
    val goalPercent: Int,
    val budgetGoal: Double,
    val revenueLastWeek: Double,
    val foodLastWeek: Double,
    val filledDrops: Int
) {
    companion object {
        fun from(
            incomes: List<Income>,
            expenses: List<Expense>,
            categories: List<Category>,
            today: LocalDate = LocalDate.now()
        ): GoalSnapshot {
            val budgetGoal = CategoryGoals.spendingOnly(categories).mapNotNull { it.budgetAmount }.sum()
            val totalExpense = expenses.sumOf { it.amount }
            val goalPercent = if (budgetGoal <= 0.0) {
                0
            } else {
                ((totalExpense / budgetGoal) * 100.0).toInt().coerceIn(0, 100)
            }
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val foodIds = categories.filter { it.categoryName.equals("Food", ignoreCase = true) }.map { it.id }.toSet()
            val revenueLastWeek = incomes.filter { it.date in weekStart..today }.sumOf { it.amount }
            val foodLastWeek = expenses.filter { it.date in weekStart..today && it.categoryId in foodIds }.sumOf { it.amount }
            val filledDrops = if (budgetGoal <= 0.0) 0 else (goalPercent / 20.0).roundToInt().coerceIn(0, 5)
            return GoalSnapshot(goalPercent, budgetGoal, revenueLastWeek, foodLastWeek, filledDrops)
        }

        fun bindProgress(activity: Activity, snapshot: GoalSnapshot) {
            activity.findViewById<TextView>(R.id.tvBudgetPercent).text =
                activity.getString(R.string.budget_percent, snapshot.goalPercent)
            activity.findViewById<TextView>(R.id.tvBudgetGoal).text = MoneyFormatter.format(snapshot.budgetGoal)
            val fill = activity.findViewById<android.view.View>(R.id.budgetFill)
            val params = fill.layoutParams as ConstraintLayout.LayoutParams
            params.matchConstraintPercentWidth = (snapshot.goalPercent / 100f).coerceIn(0f, 1f)
            fill.layoutParams = params
        }

        fun bindDrops(drops: List<ImageView>, filled: Int) {
            drops.forEachIndexed { index, image ->
                image.alpha = if (index < filled) 1f else 0.28f
            }
        }
    }
}
