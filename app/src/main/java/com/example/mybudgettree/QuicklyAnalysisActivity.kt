package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class QuicklyAnalysisActivity : AppCompatActivity() {
    private val adapter = TransactionAdapter()

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
        setContentView(R.layout.activity_quickly_analysis)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.quicklyAnalysisRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnChartSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<RecyclerView>(R.id.rvQuickTransactions).apply {
            layoutManager = LinearLayoutManager(this@QuicklyAnalysisActivity)
            adapter = this@QuicklyAnalysisActivity.adapter
        }
        findViewById<DonutTargetView>(R.id.quicklyGoalDonut).apply {
            setShowPercent(true)
            setRingColors(getColor(R.color.analysis_donut_track), getColor(R.color.analysis_progress_blue))
        }
        MainNavigation.bind(this, MainNavigation.Tab.HOME)
    }

    override fun onResume() {
        super.onResume()
        loadQuickly()
    }

    private fun loadQuickly() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        val today = LocalDate.now()
        lifecycleScope.launch {
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val categoryNames = categories.associate { it.id to it.categoryName }
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            val monthlyGoal = app.monthlyGoalDatabaseSystem.getGoal(user, YearMonth.from(today))

            val savingsSnapshot = SavingsSnapshot.compute(app, user, today)
            findViewById<DonutTargetView>(R.id.quicklyGoalDonut).setPercent(savingsSnapshot.percentOfTarget)
            findViewById<TextView>(R.id.tvRevenueLastWeek).text = MoneyFormatter.format(savingsSnapshot.savedThisMonth)
            findViewById<TextView>(R.id.tvFoodLastWeek).text = MoneyFormatter.format(savingsSnapshot.savedThisWeek)

            val month = YearMonth.from(today)
            findViewById<TextView>(R.id.tvChartTitle).text = getString(
                R.string.month_expenses,
                month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            )
            val chart = AnalysisCalculator.snapshot(
                incomes = incomes,
                expenses = expenses,
                monthlyGoal = monthlyGoal,
                period = AnalysisPeriod.WEEKLY,
                anchorDate = today,
                weekLabels = listOf(
                    getString(R.string.week_1),
                    getString(R.string.week_2),
                    getString(R.string.week_3),
                    getString(R.string.week_4)
                )
            )
            findViewById<AnalysisChartView>(R.id.chartView).setData(
                labels = chart.buckets.map { it.label },
                incomeValues = chart.buckets.map { 0.0 },
                expenseValues = chart.buckets.map { it.expense },
                expenseOnly = true
            )

            val monthRows = (
                incomes.map { income ->
                    TransactionRow(
                        title = income.description,
                        categoryName = categoryNames[income.categoryId] ?: "",
                        amount = income.amount,
                        isIncome = true,
                        date = income.date,
                        time = income.startTime,
                        imagePath = income.imagePath
                    )
                } + expenses.map { expense ->
                    TransactionRow(
                        title = expense.description,
                        categoryName = categoryNames[expense.categoryId] ?: "",
                        amount = expense.amount,
                        isIncome = false,
                        date = expense.date,
                        time = expense.startTime,
                        imagePath = expense.imagePath
                    )
                }
                ).filter { YearMonth.from(it.date) == month }
                .sortedWith(compareByDescending<TransactionRow> { it.date }.thenByDescending { it.time })
            adapter.submit(monthRows)
            findViewById<TextView>(R.id.tvEmptyQuick).visibility =
                if (monthRows.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
