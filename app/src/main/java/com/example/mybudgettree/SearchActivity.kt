package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class SearchActivity : AppCompatActivity() {
    private val adapter = TransactionAdapter(R.layout.item_search_result)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd / MMM / yyyy", Locale.ENGLISH)
    private var selectedDate = LocalDate.now()
    private var allRows: List<TransactionRow> = emptyList()
    private var hasSearched = false

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
        setContentView(R.layout.activity_search)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.searchRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<RecyclerView>(R.id.rvSearchResults).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = this@SearchActivity.adapter
            isNestedScrollingEnabled = false
        }

        findViewById<View>(R.id.dateField).setOnClickListener { showDatePicker() }
        findViewById<MaterialButton>(R.id.btnRunSearch).setOnClickListener { runSearch() }
        findViewById<EditText>(R.id.etSearchQuery).setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch()
                true
            } else {
                false
            }
        }

        bindDate()
        MainNavigation.bind(this, MainNavigation.Tab.ANALYTICS)
        loadRows()
    }

    override fun onResume() {
        super.onResume()
        loadRows()
    }

    private fun loadRows() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val categoryNames = categories.associate { it.id to it.categoryName }
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            bindCategories(categories.map { it.categoryName })
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
            if (hasSearched) runSearch()
        }
    }

    private fun bindCategories(names: List<String>) {
        val actCategory = findViewById<AutoCompleteTextView>(R.id.actCategory)
        val options = listOf(getString(R.string.select_category)) + names.sorted()
        actCategory.threshold = 0
        actCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        )
        if (actCategory.text.isNullOrBlank()) {
            actCategory.setText(getString(R.string.select_category), false)
        }
        actCategory.setOnClickListener { actCategory.showDropDown() }
    }

    private fun runSearch() {
        hasSearched = true
        val query = findViewById<EditText>(R.id.etSearchQuery).text.toString().trim()
        val category = findViewById<AutoCompleteTextView>(R.id.actCategory).text.toString().trim()
        val anyCategory = category.isEmpty() || category == getString(R.string.select_category)
        val wantIncome = findViewById<RadioButton>(R.id.radioIncome).isChecked
        val results = allRows.filter { row ->
            row.isIncome == wantIncome &&
                row.date == selectedDate &&
                (anyCategory || row.categoryName.equals(category, ignoreCase = true)) &&
                (
                    query.isEmpty() ||
                        row.title.contains(query, ignoreCase = true) ||
                        row.categoryName.contains(query, ignoreCase = true)
                    )
        }
        adapter.submit(results)
        findViewById<RecyclerView>(R.id.rvSearchResults).visibility =
            if (results.isEmpty()) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.tvEmptySearch).visibility =
            if (results.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun bindDate() {
        findViewById<TextView>(R.id.tvSearchDate).text = dateFormatter.format(selectedDate)
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                bindDate()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }
}
