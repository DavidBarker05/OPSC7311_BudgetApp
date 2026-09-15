package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.Category
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class GoalDetailActivity : AppCompatActivity() {
    private val adapter = TransactionHistoryAdapter(::showMonthPicker, R.layout.item_search_result)
    private var category: Category? = null
    private var allRows: List<TransactionRow> = emptyList()
    private var monthFilter: YearMonth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_goal_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.goalDetailRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnAddSavings).setOnClickListener {
            val id = category?.id ?: return@setOnClickListener
            startActivity(
                Intent(this, FillWateringCanActivity::class.java)
                    .putExtra(EXTRA_CATEGORY_ID, id)
            )
        }
        findViewById<RecyclerView>(R.id.rvGoalSavings).apply {
            layoutManager = LinearLayoutManager(this@GoalDetailActivity)
            adapter = this@GoalDetailActivity.adapter
        }
        findViewById<DonutTargetView>(R.id.goalDonut).apply {
            setShowPercent(false)
            setRingColors(getColor(R.color.analysis_donut_track), getColor(R.color.home_header))
        }
        MainNavigation.bind(this, MainNavigation.Tab.GOALS)
    }

    override fun onResume() {
        super.onResume()
        loadGoal()
    }

    private fun loadGoal() {
        val user = UserSession.currentUser ?: return
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val found = app.categoryDatabaseSystem.findCategory(categoryId).category
            if (found == null) {
                finish()
                return@launch
            }
            category = found
            findViewById<TextView>(R.id.tvGoalTitle).text = found.categoryName
            findViewById<TextView>(R.id.tvGoalBadgeName).text = found.categoryName
            findViewById<ImageView>(R.id.ivGoalBadge).apply {
                setImageResource(CategoryGoals.iconRes(found.categoryName))
                setColorFilter(getColor(R.color.green_text))
            }
            val allCategories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            val spending = CategoryGoals.spendingOnly(allCategories)
            val spendingBudget = spending.mapNotNull { it.budgetAmount }.sum()
            val expensePercent = if (spendingBudget <= 0.0) 0 else ((expenses.sumOf { it.amount } / spendingBudget) * 100.0).toInt()
            findViewById<TextView>(R.id.tvGoalStatus).text = when {
                spendingBudget <= 0.0 -> getString(R.string.expenses_status_no_budget)
                expensePercent > 100 -> getString(R.string.expenses_status_over, expensePercent)
                expensePercent > 70 -> getString(R.string.expenses_status_watch, expensePercent)
                else -> getString(R.string.expenses_status_good, expensePercent.coerceAtMost(100))
            }

            val savings = app.incomeDatabaseSystem.retrieveAllIncomesForCategory(found).incomes.orEmpty()
            val saved = savings.sumOf { it.amount }
            val target = found.budgetAmount ?: 0.0
            val percent = if (target <= 0.0) 0 else ((saved / target) * 100.0).toInt().coerceIn(0, 100)
            findViewById<TextView>(R.id.tvGoalAmount).text = MoneyFormatter.format(target)
            findViewById<TextView>(R.id.tvAmountSaved).text = MoneyFormatter.format(saved)
            findViewById<TextView>(R.id.tvGoalPercent).text = getString(R.string.budget_percent, percent)
            findViewById<TextView>(R.id.tvGoalTarget).text = MoneyFormatter.format(target)
            findViewById<DonutTargetView>(R.id.goalDonut).setPercent(percent)
            val fill = findViewById<View>(R.id.goalFill)
            val params = fill.layoutParams as ConstraintLayout.LayoutParams
            params.matchConstraintPercentWidth = percent / 100f
            fill.layoutParams = params

            val icon = CategoryGoals.iconRes(found.categoryName)
            allRows = savings.map { income ->
                TransactionRow(
                    title = income.description.substringBefore('\n'),
                    categoryName = found.categoryName,
                    amount = income.amount,
                    isIncome = true,
                    date = income.date,
                    time = income.startTime,
                    imagePath = income.imagePath,
                    iconRes = icon
                )
            }.sortedWith(compareByDescending<TransactionRow> { it.date }.thenByDescending { it.time })
            bindList()
        }
    }

    private fun bindList() {
        val filtered = allRows.filter { monthFilter == null || YearMonth.from(it.date) == monthFilter }
        val grouped = filtered.groupBy { YearMonth.from(it.date) }.toSortedMap(compareByDescending { it })
        val items = mutableListOf<TransactionListItem>()
        grouped.entries.forEachIndexed { index, (month, rows) ->
            items += TransactionListItem.Header(
                title = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                showCalendar = index == 0
            )
            rows.forEach { items += TransactionListItem.Entry(it) }
        }
        adapter.submit(items)
        findViewById<TextView>(R.id.tvEmptyGoal).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showMonthPicker() {
        val initial = monthFilter?.atDay(1) ?: LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, _ ->
                monthFilter = YearMonth.of(year, month + 1)
                bindList()
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    companion object {
        const val EXTRA_CATEGORY_ID = "category_id"
    }
}
