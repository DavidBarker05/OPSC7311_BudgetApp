package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import com.example.mybudgettree.database.entries.Expense
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class SpendingDashboardActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SpendingDashboard"
        private const val KEY_FROM = "from"
        private const val KEY_TO = "to"
        private const val KEY_CATEGORY = "category"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    private val shortFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val adapter = DashboardCategoryAdapter { select(it) }

    private var from: LocalDate = DashboardPreset.THIS_MONTH.range(LocalDate.now()).first
    private var to: LocalDate = LocalDate.now()
    private var selectedCategoryId: String? = null

    // What is on screen now, which only changes when the input was valid and the data loaded
    private var expenses: List<Expense> = emptyList()
    private var spending: List<CategorySpending> = emptyList()
    private var shownFrom: LocalDate = from
    private var shownTo: LocalDate = to
    private var shownMin: Double? = null
    private var shownMax: Double? = null

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
        setContentView(R.layout.activity_spending_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboardRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.rvDashboard).apply {
            layoutManager = LinearLayoutManager(this@SpendingDashboardActivity)
            adapter = this@SpendingDashboardActivity.adapter
        }
        findViewById<TextView>(R.id.tvFromDate).setOnClickListener { pickDate(from) { from = it; bindDates() } }
        findViewById<TextView>(R.id.tvToDate).setOnClickListener { pickDate(to) { to = it; bindDates() } }
        findViewById<MaterialButton>(R.id.btnThisMonth).setOnClickListener { applyPreset(DashboardPreset.THIS_MONTH) }
        findViewById<MaterialButton>(R.id.btnLast7Days).setOnClickListener { applyPreset(DashboardPreset.LAST_7_DAYS) }
        findViewById<MaterialButton>(R.id.btnShow).setOnClickListener { show() }

        if (savedInstanceState != null) {
            // The fields' text is restored by Android. The dates and the chosen category are saved by hand
            from = LocalDate.parse(savedInstanceState.getString(KEY_FROM, from.toString()))
            to = LocalDate.parse(savedInstanceState.getString(KEY_TO, to.toString()))
            selectedCategoryId = savedInstanceState.getString(KEY_CATEGORY)
            bindDates()
            show()
        } else {
            bindDates()
            prefillFromMonthlyGoal()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FROM, from.toString())
        outState.putString(KEY_TO, to.toString())
        outState.putString(KEY_CATEGORY, selectedCategoryId)
    }

    // The saved goal for this month is where the minimum and maximum start, then the dashboard is shown with them
    private fun prefillFromMonthlyGoal() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val goal = app.monthlyGoalDatabaseSystem.getGoal(user, YearMonth.now())
            if (goal != null && goal.minGoal > 0.0) findViewById<EditText>(R.id.etMin).setText(plainAmount(goal.minGoal))
            if (goal != null && goal.maxGoal > 0.0) findViewById<EditText>(R.id.etMax).setText(plainAmount(goal.maxGoal))
            show()
        }
    }

    private fun applyPreset(preset: DashboardPreset) {
        val (start, end) = preset.range(LocalDate.now())
        from = start
        to = end
        bindDates()
        show()
    }

    private fun pickDate(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).show()
    }

    private fun bindDates() {
        findViewById<TextView>(R.id.tvFromDate).text = dateFormatter.format(from)
        findViewById<TextView>(R.id.tvToDate).text = dateFormatter.format(to)
    }

    /**
     * Checks what was entered. If it is fine, loads the spending for the period and shows it, otherwise shows what is
     * wrong and leaves the last good result on screen
     */
    private fun show() {
        val input = SpendingDashboardCalculator.validate(
            findViewById<EditText>(R.id.etMin).text?.toString().orEmpty(),
            findViewById<EditText>(R.id.etMax).text?.toString().orEmpty(),
            from,
            to
        )
        bindProblems(input)
        if (!input.isValid) return

        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user)
            val found = app.expenseDatabaseSystem.retrieveAllExpensesBetweenDates(user, input.from, input.to)
            if (!categories.wasSuccessful || !found.wasSuccessful) {
                Log.w(TAG, "Could not load the dashboard: ${categories.errMsg ?: found.errMsg}")
                Toast.makeText(this@SpendingDashboardActivity, R.string.dashboard_load_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            expenses = found.values.orEmpty()
            shownFrom = input.from
            shownTo = input.to
            shownMin = input.min
            shownMax = input.max
            spending = SpendingDashboardCalculator.categoryTotals(
                expenses, categories.values.orEmpty(), input.from, input.to, input.min, input.max
            )
            // Keep the category the user was looking at, or else start on the one with the most spending
            if (spending.none { it.category.id == selectedCategoryId }) selectedCategoryId = spending.firstOrNull()?.category?.id
            bindResults()
        }
    }

    private fun bindProblems(input: DashboardInput) {
        val problems = input.problems
        showError(R.id.tvMinError, when {
            InputProblem.MIN_NOT_A_NUMBER in problems -> R.string.dashboard_error_not_a_number
            InputProblem.MIN_NEGATIVE in problems -> R.string.dashboard_error_negative
            InputProblem.MIN_ABOVE_MAX in problems -> R.string.dashboard_error_min_above_max
            else -> null
        })
        showError(R.id.tvMaxError, when {
            InputProblem.MAX_NOT_A_NUMBER in problems -> R.string.dashboard_error_not_a_number
            InputProblem.MAX_NEGATIVE in problems -> R.string.dashboard_error_negative
            else -> null
        })
        showError(R.id.tvPeriodError, when {
            InputProblem.FROM_AFTER_TO in problems -> R.string.dashboard_error_from_after_to
            InputProblem.PERIOD_TOO_LONG in problems -> R.string.dashboard_error_too_long
            else -> null
        })
    }

    // Shows the message under a field, or hides the line when there is nothing wrong with it
    private fun showError(view: Int, message: Int?) {
        findViewById<TextView>(view).apply {
            visibility = if (message != null) View.VISIBLE else View.GONE
            if (message != null) setText(message)
        }
    }

    private fun select(item: CategorySpending) {
        selectedCategoryId = item.category.id
        bindResults()
    }

    private fun bindResults() {
        adapter.submit(spending, selectedCategoryId)
        findViewById<View>(R.id.tvDashboardEmpty).visibility = if (spending.isEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.tvDashboardHint).visibility = if (spending.isEmpty()) View.GONE else View.VISIBLE
        bindChart()
    }

    private fun bindChart() {
        val chart = findViewById<SpendingChartView>(R.id.chartSpending)
        val title = findViewById<TextView>(R.id.tvChartTitle)
        val empty = findViewById<View>(R.id.tvChartEmpty)
        val selected = spending.firstOrNull { it.category.id == selectedCategoryId }
        if (selected == null) {
            title.visibility = View.GONE
            chart.visibility = View.GONE
            empty.visibility = View.GONE
            return
        }

        val days = SpendingDashboardCalculator.dailySpending(
            expenses.filter { it.categoryId == selected.category.id }, shownFrom, shownTo
        )
        title.visibility = View.VISIBLE
        chart.visibility = View.VISIBLE
        empty.visibility = if (selected.total <= 0.0) View.VISIBLE else View.GONE
        chart.setData(days, shownMin, shownMax)

        title.text = getString(
            R.string.dashboard_chart_title, selected.category.categoryName, shortFormatter.format(shownFrom), shortFormatter.format(shownTo)
        )
        chart.contentDescription = "${title.text}"
    }

    private fun plainAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}
