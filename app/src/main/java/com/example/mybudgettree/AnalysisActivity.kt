package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
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
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import java.time.LocalDate

class AnalysisActivity : AppCompatActivity() {
    private var selectedPeriod = AnalysisPeriod.DAILY
    private var anchorDate = LocalDate.now()
    private var incomes: List<Income> = emptyList()
    private var expenses: List<Expense> = emptyList()
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState != null) {
            selectedPeriod = AnalysisPeriod.entries.getOrElse(savedInstanceState.getInt(KEY_PERIOD, 0)) {
                AnalysisPeriod.DAILY
            }
            anchorDate = LocalDate.parse(savedInstanceState.getString(KEY_ANCHOR, LocalDate.now().toString()))
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_analysis)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analysisRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnChartCalendar).setOnClickListener { showDatePicker() }
        findViewById<ImageButton>(R.id.btnChartSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        val periodToggle = findViewById<MaterialButtonToggleGroup>(R.id.periodToggle)
        periodToggle.check(buttonIdFor(selectedPeriod))
        periodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedPeriod = when (checkedId) {
                R.id.btnWeekly -> AnalysisPeriod.WEEKLY
                R.id.btnMonthly -> AnalysisPeriod.MONTHLY
                R.id.btnYear -> AnalysisPeriod.YEARLY
                else -> AnalysisPeriod.DAILY
            }
            lifecycleScope.launch { bindSnapshot() }
        }

        MainNavigation.bind(this, MainNavigation.Tab.ANALYTICS)
    }

    override fun onResume() {
        super.onResume()
        loadAnalysis()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PERIOD, selectedPeriod.ordinal)
        outState.putString(KEY_ANCHOR, anchorDate.toString())
    }

    private fun loadAnalysis() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            bindSnapshot()
        }
    }

    private suspend fun bindSnapshot() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        val monthlyGoal = app.monthlyGoalDatabaseSystem.getGoal(user, java.time.YearMonth.from(anchorDate))
        val snapshot = AnalysisCalculator.snapshot(
            context = this,
            incomes = incomes,
            expenses = expenses,
            monthlyGoal = monthlyGoal,
            period = selectedPeriod,
            anchorDate = anchorDate
        )
        findViewById<TextView>(R.id.tvTotalBalance).text = MoneyFormatter.format(snapshot.totalBalance)
        findViewById<TextView>(R.id.tvTotalExpense).text =
            MoneyFormatter.formatSigned(snapshot.totalExpense, isIncome = false)
        findViewById<TextView>(R.id.tvPeriodIncome).text = MoneyFormatter.format(snapshot.periodIncome)
        findViewById<TextView>(R.id.tvPeriodExpense).text = MoneyFormatter.format(snapshot.periodExpense)
        findViewById<TextView>(R.id.tvBudgetGoal).text = MoneyFormatter.format(snapshot.budgetGoal)
        findViewById<TextView>(R.id.tvBudgetPercent).text =
            getString(R.string.budget_percent, snapshot.expensePercent)
        val level = BudgetStatusHelper.level(snapshot.monthExpense, snapshot.minGoal, snapshot.budgetGoal)
        findViewById<TextView>(R.id.tvExpenseStatus).text =
            BudgetStatusHelper.statusText(this, level, snapshot.expensePercent, snapshot.minGoal > 0.0)
        updateBudgetFill(snapshot.expensePercent.coerceIn(0, 100), level)
        applyPeriodStyles()

        findViewById<AnalysisChartView>(R.id.chartView).setData(
            labels = snapshot.buckets.map { it.label },
            incomeValues = snapshot.buckets.map { it.income },
            expenseValues = snapshot.buckets.map { it.expense }
        )
    }

    private fun updateBudgetFill(percent: Int, level: BudgetLevel) {
        val fill = findViewById<android.view.View>(R.id.budgetFill)
        val params = fill.layoutParams as ConstraintLayout.LayoutParams
        params.matchConstraintPercentWidth = (percent / 100f).coerceIn(0f, 1f)
        fill.layoutParams = params
        BudgetStatusHelper.tintFill(fill, this, level)
    }

    private fun applyPeriodStyles() {
        stylePeriodButton(R.id.btnDaily, selectedPeriod == AnalysisPeriod.DAILY)
        stylePeriodButton(R.id.btnWeekly, selectedPeriod == AnalysisPeriod.WEEKLY)
        stylePeriodButton(R.id.btnMonthly, selectedPeriod == AnalysisPeriod.MONTHLY)
        stylePeriodButton(R.id.btnYear, selectedPeriod == AnalysisPeriod.YEARLY)
        val headerColor = getColor(
            when (selectedPeriod) {
                AnalysisPeriod.DAILY, AnalysisPeriod.WEEKLY -> R.color.home_header
                AnalysisPeriod.MONTHLY -> R.color.analysis_header_sage
                AnalysisPeriod.YEARLY -> R.color.analysis_header_muted
            }
        )
        findViewById<ConstraintLayout>(R.id.analysisRoot).setBackgroundColor(headerColor)
    }

    private fun stylePeriodButton(buttonId: Int, selected: Boolean) {
        findViewById<MaterialButton>(buttonId).setBackgroundColor(
            getColor(if (selected) R.color.period_selected else android.R.color.transparent)
        )
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                anchorDate = LocalDate.of(year, month + 1, day)
                lifecycleScope.launch { bindSnapshot() }
            },
            anchorDate.year,
            anchorDate.monthValue - 1,
            anchorDate.dayOfMonth
        ).show()
    }

    private fun buttonIdFor(period: AnalysisPeriod): Int = when (period) {
        AnalysisPeriod.DAILY -> R.id.btnDaily
        AnalysisPeriod.WEEKLY -> R.id.btnWeekly
        AnalysisPeriod.MONTHLY -> R.id.btnMonthly
        AnalysisPeriod.YEARLY -> R.id.btnYear
    }

    private companion object {
        const val KEY_PERIOD = "period"
        const val KEY_ANCHOR = "anchor"
    }
}
