package com.example.mybudgettree

import android.app.DatePickerDialog
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
import com.example.mybudgettree.database.entries.Category
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class CategoryDetailActivity : AppCompatActivity() {
    private val adapter = TransactionHistoryAdapter(::showMonthPicker, R.layout.item_search_result, ::openEditTransaction)
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
        setContentView(R.layout.activity_category_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryDetailRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnEditCategory).setOnClickListener { openEditCategory() }
        findViewById<ImageButton>(R.id.btnEditMonthlyGoal).setOnClickListener { openEditCategory() }
        findViewById<MaterialButton>(R.id.btnAddExpense).setOnClickListener {
            val id = category?.id ?: return@setOnClickListener
            startActivity(
                Intent(this, SowExpensesActivity::class.java)
                    .putExtra(EXTRA_CATEGORY_ID, id)
            )
        }
        findViewById<MaterialButton>(R.id.btnAddIncome).setOnClickListener {
            val id = category?.id ?: return@setOnClickListener
            startActivity(
                Intent(this, SowIncomeActivity::class.java)
                    .putExtra(EXTRA_CATEGORY_ID, id)
            )
        }
        findViewById<RecyclerView>(R.id.rvCategoryExpenses).apply {
            layoutManager = LinearLayoutManager(this@CategoryDetailActivity)
            adapter = this@CategoryDetailActivity.adapter
        }
        MainNavigation.bind(this, MainNavigation.Tab.CATEGORIES)
    }

    override fun onResume() {
        super.onResume()
        loadCategory()
    }

    private fun loadCategory() {
        val user = UserSession.currentUser ?: return
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID).orEmpty()
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val found = app.categoryDatabaseSystem.findCategory(categoryId).value
            if (found == null) {
                finish()
                return@launch
            }
            category = found
            findViewById<TextView>(R.id.tvCategoryTitle).text = found.categoryName
            val currentMonth = YearMonth.now()
            val expensesThisMonth = app.expenseDatabaseSystem.retrieveAllExpenses(user).values.orEmpty()
                .filter { YearMonth.from(it.dateAsLocalDate()) == currentMonth }
            val incomesThisMonth = app.incomeDatabaseSystem.retrieveAllIncomes(user).values.orEmpty()
                .filter { YearMonth.from(it.dateAsLocalDate()) == currentMonth }

            val icon = CategoryGarden.iconRes(found)
            val categoryExpenses = app.expenseDatabaseSystem.retrieveAllExpensesForCategory(found).values.orEmpty()
            val categoryIncomes = app.incomeDatabaseSystem.retrieveAllIncomesForCategory(found).values.orEmpty()
            val spentThisMonth = categoryExpenses.filter { YearMonth.from(it.dateAsLocalDate()) == currentMonth }.sumOf { it.amount }
            BudgetOverview.bind(this@CategoryDetailActivity, incomesThisMonth, expensesThisMonth, spentThisMonth, found.budgetAmount ?: 0.0)
            val expenseRows = categoryExpenses.map { expense ->
                TransactionRow(
                    id = expense.id,
                    title = expense.description.substringBefore('\n'),
                    categoryName = found.categoryName,
                    amount = expense.amount,
                    isIncome = false,
                    date = expense.dateAsLocalDate(),
                    time = expense.startTimeAsLocalTime(),
                    imagePath = expense.imagePathFor(DeviceId.get(app)),
                    iconRes = icon
                )
            }
            val incomeRows = categoryIncomes.map { income ->
                TransactionRow(
                    id = income.id,
                    title = income.description.substringBefore('\n'),
                    categoryName = found.categoryName,
                    amount = income.amount,
                    isIncome = true,
                    date = income.dateAsLocalDate(),
                    time = income.startTimeAsLocalTime(),
                    imagePath = income.imagePathFor(DeviceId.get(app)),
                    iconRes = icon
                )
            }
            allRows = (expenseRows + incomeRows)
                .sortedWith(compareByDescending<TransactionRow> { it.date }.thenByDescending { it.time })
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
        findViewById<TextView>(R.id.tvEmptyCategory).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditTransaction(row: TransactionRow) {
        startActivity(
            Intent(this, EditTransactionActivity::class.java)
                .putExtra(EditTransactionActivity.EXTRA_TRANSACTION_ID, row.id)
                .putExtra(EditTransactionActivity.EXTRA_IS_INCOME, row.isIncome)
        )
    }

    private fun openEditCategory() {
        val id = category?.id ?: return
        startActivity(
            Intent(this, EditCategoryActivity::class.java)
                .putExtra(EditCategoryActivity.EXTRA_CATEGORY_ID, id)
        )
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
