package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
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

class TransactionActivity : AppCompatActivity() {
    private val adapter = TransactionHistoryAdapter(::showMonthPicker, onEntryClick = ::openEditTransaction)
    private var allRows: List<TransactionRow> = emptyList()
    private var typeFilter = TypeFilter.ALL
    private var monthFilter: YearMonth? = null

    private enum class TypeFilter { ALL, INCOME, EXPENSE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState != null) {
            typeFilter = TypeFilter.entries.getOrElse(savedInstanceState.getInt(KEY_FILTER, 0)) {
                TypeFilter.ALL
            }
            monthFilter = savedInstanceState.getString(KEY_MONTH)?.let(YearMonth::parse)
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_transaction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.transactionRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.cardIncome).setOnClickListener {
            typeFilter = if (typeFilter == TypeFilter.INCOME) TypeFilter.ALL else TypeFilter.INCOME
            bindList()
        }
        findViewById<LinearLayout>(R.id.cardExpense).setOnClickListener {
            typeFilter = if (typeFilter == TypeFilter.EXPENSE) TypeFilter.ALL else TypeFilter.EXPENSE
            bindList()
        }

        findViewById<RecyclerView>(R.id.rvTransactions).apply {
            layoutManager = LinearLayoutManager(this@TransactionActivity)
            adapter = this@TransactionActivity.adapter
        }

        applyFilterStyles()
        MainNavigation.bind(this, MainNavigation.Tab.TRANSACTION)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_FILTER, typeFilter.ordinal)
        outState.putString(KEY_MONTH, monthFilter?.toString())
    }

    private fun loadTransactions() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val categoryNames = categories.associate { it.id to it.categoryName }
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            val totalIncome = incomes.sumOf { it.amount }
            val totalExpense = expenses.sumOf { it.amount }

            findViewById<TextView>(R.id.tvTotalBalance).text = MoneyFormatter.format(totalIncome - totalExpense)
            findViewById<TextView>(R.id.tvIncomeTotal).text = MoneyFormatter.format(totalIncome)
            findViewById<TextView>(R.id.tvExpenseTotal).text = MoneyFormatter.format(totalExpense)

            allRows = (
                incomes.map { income ->
                    TransactionRow(
                        id = income.id,
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
                        id = expense.id,
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

            bindList()
        }
    }

    private fun bindList() {
        val filtered = allRows.filter { row ->
            val typeOk = when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.INCOME -> row.isIncome
                TypeFilter.EXPENSE -> !row.isIncome
            }
            val monthOk = monthFilter == null || YearMonth.from(row.date) == monthFilter
            typeOk && monthOk
        }
        val grouped = filtered
            .groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
        val items = mutableListOf<TransactionListItem>()
        grouped.entries.forEachIndexed { index, (month, rows) ->
            items += TransactionListItem.Header(
                title = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                showCalendar = index == 0
            )
            rows.forEach { items += TransactionListItem.Entry(it) }
        }
        adapter.submit(items)
        findViewById<TextView>(R.id.tvEmptyTransactions).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
        applyFilterStyles()
    }

    private fun applyFilterStyles() {
        findViewById<LinearLayout>(R.id.cardIncome).setBackgroundResource(
            if (typeFilter == TypeFilter.INCOME) R.drawable.bg_filter_selected else R.drawable.bg_balance_card
        )
        findViewById<LinearLayout>(R.id.cardExpense).setBackgroundResource(
            if (typeFilter == TypeFilter.EXPENSE) R.drawable.bg_filter_selected else R.drawable.bg_balance_card
        )
    }

    private fun openEditTransaction(row: TransactionRow) {
        startActivity(
            Intent(this, EditTransactionActivity::class.java)
                .putExtra(EditTransactionActivity.EXTRA_TRANSACTION_ID, row.id)
                .putExtra(EditTransactionActivity.EXTRA_IS_INCOME, row.isIncome)
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

    private companion object {
        const val KEY_FILTER = "filter"
        const val KEY_MONTH = "month"
    }
}
