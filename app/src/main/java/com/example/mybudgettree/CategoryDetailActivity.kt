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
        findViewById<MaterialButton>(R.id.btnSowExpenses).setOnClickListener {
            val id = category?.id ?: return@setOnClickListener
            startActivity(
                Intent(this, SowExpensesActivity::class.java)
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
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val found = app.categoryDatabaseSystem.findCategory(categoryId).category
            if (found == null) {
                finish()
                return@launch
            }
            category = found
            findViewById<TextView>(R.id.tvCategoryTitle).text = found.categoryName
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            BudgetOverview.bind(this@CategoryDetailActivity, incomes, expenses, categories)

            val icon = CategoryGarden.iconRes(found.categoryName)
            val categoryExpenses = app.expenseDatabaseSystem.retrieveAllExpensesForCategory(found).expenses.orEmpty()
            allRows = categoryExpenses.map { expense ->
                TransactionRow(
                    title = expense.description.substringBefore('\n'),
                    categoryName = found.categoryName,
                    amount = expense.amount,
                    isIncome = false,
                    date = expense.date,
                    time = expense.startTime,
                    imagePath = expense.imagePath,
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
        findViewById<TextView>(R.id.tvEmptyCategory).visibility =
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
