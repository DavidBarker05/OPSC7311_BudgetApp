package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

class HomeActivity : AppCompatActivity() {
    private val adapter = TransactionAdapter()
    private var allRows: List<TransactionRow> = emptyList()
    private var selectedPeriod = Period.MONTHLY

    private enum class Period { DAILY, WEEKLY, MONTHLY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<TextView>(R.id.tvUserName).text = UserSession.currentUser?.displayName.orEmpty()
        findViewById<ImageButton>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<RecyclerView>(R.id.rvTransactions).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
        }

        val periodToggle = findViewById<MaterialButtonToggleGroup>(R.id.periodToggle)
        periodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedPeriod = when (checkedId) {
                R.id.btnDaily -> Period.DAILY
                R.id.btnWeekly -> Period.WEEKLY
                else -> Period.MONTHLY
            }
            applyPeriodStyles()
            showFilteredTransactions()
        }
        applyPeriodStyles()

        findViewById<View>(R.id.savingsGoalsCard).setOnClickListener {
            startActivity(Intent(this, QuicklyAnalysisActivity::class.java))
        }
        findViewById<DonutTargetView>(R.id.homeGoalDonut).apply {
            setShowPercent(true)
            setRingColors(getColor(R.color.home_donut_track), getColor(R.color.analysis_progress_blue))
            setTextColor(BudgetStatusHelper.contrastingTextColor(getColor(R.color.home_header)))
        }
        MainNavigation.bind(this, MainNavigation.Tab.HOME)
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            findViewById<TextView>(R.id.tvUserName).text = user.displayName
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val categoryNames = categories.associate { it.id to it.categoryName }
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()

            val monthlyGoal = app.monthlyGoalDatabaseSystem.getGoal(user, YearMonth.now())
            val snapshot = GoalSnapshot.from(expenses, monthlyGoal)
            GoalSnapshot.bindProgress(this@HomeActivity, snapshot)

            val savingsSnapshot = SavingsSnapshot.compute(app, user)
            findViewById<DonutTargetView>(R.id.homeGoalDonut).setPercent(savingsSnapshot.percentOfTarget)
            findViewById<TextView>(R.id.tvRevenueLastWeek).text = MoneyFormatter.format(savingsSnapshot.savedThisMonth)
            findViewById<TextView>(R.id.tvFoodLastWeek).text = MoneyFormatter.format(savingsSnapshot.savedThisWeek)
            GoalSnapshot.bindDrops(
                listOf(
                    findViewById(R.id.drop1),
                    findViewById(R.id.drop2),
                    findViewById(R.id.drop3),
                    findViewById(R.id.drop4),
                    findViewById(R.id.drop5)
                ),
                savingsSnapshot.filledDrops
            )

            allRows = (
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
                ).sortedWith(compareByDescending<TransactionRow> { it.date }.thenByDescending { it.time })

            showFilteredTransactions()
        }
    }

    private fun showFilteredTransactions() {
        val today = LocalDate.now()
        val filtered = allRows.filter { row ->
            when (selectedPeriod) {
                Period.DAILY -> row.date == today
                Period.WEEKLY -> {
                    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    !row.date.isBefore(weekStart) && !row.date.isAfter(today)
                }
                Period.MONTHLY -> row.date.year == today.year && row.date.month == today.month
            }
        }
        adapter.submit(filtered)
        findViewById<TextView>(R.id.tvEmptyTransactions).visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE

        val periodIncome = filtered.filter { it.isIncome }.sumOf { it.amount }
        val periodExpense = filtered.filter { !it.isIncome }.sumOf { it.amount }
        findViewById<TextView>(R.id.tvTotalBalance).text = MoneyFormatter.format(periodIncome - periodExpense)
        findViewById<TextView>(R.id.tvTotalExpense).text = MoneyFormatter.formatSigned(periodExpense, isIncome = false)
    }

    private fun applyPeriodStyles() {
        stylePeriodButton(R.id.btnDaily, selectedPeriod == Period.DAILY)
        stylePeriodButton(R.id.btnWeekly, selectedPeriod == Period.WEEKLY)
        stylePeriodButton(R.id.btnMonthly, selectedPeriod == Period.MONTHLY)
    }

    private fun stylePeriodButton(buttonId: Int, selected: Boolean) {
        findViewById<MaterialButton>(buttonId).setBackgroundColor(
            getColor(if (selected) R.color.period_selected else R.color.white)
        )
    }
}
