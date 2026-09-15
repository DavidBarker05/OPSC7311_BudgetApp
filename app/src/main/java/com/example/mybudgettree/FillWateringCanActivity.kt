package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.example.mybudgettree.database.entries.Category
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class FillWateringCanActivity : AppCompatActivity() {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    private var selectedDate = LocalDate.now()
    private var goals: List<Category> = emptyList()

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
        setContentView(R.layout.activity_fill_watering_can)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fillCanRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View>(R.id.dateField).setOnClickListener { showDatePicker() }
        findViewById<MaterialButton>(R.id.btnSaveSavings).setOnClickListener { saveSavings() }
        bindDate()
        MainNavigation.bind(this, MainNavigation.Tab.GOALS)
        loadGoals()
    }

    private fun loadGoals() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            goals = CategoryGoals.ensureForUser(user, app.categoryDatabaseSystem)
            val names = goals.map { it.categoryName }
            val dropdown = findViewById<AutoCompleteTextView>(R.id.actFillCategory)
            dropdown.threshold = 0
            dropdown.setAdapter(ArrayAdapter(this@FillWateringCanActivity, android.R.layout.simple_dropdown_item_1line, names))
            dropdown.setOnClickListener { dropdown.showDropDown() }
            val preselectId = intent.getLongExtra(GoalDetailActivity.EXTRA_CATEGORY_ID, -1L)
            val preselected = goals.firstOrNull { it.id == preselectId }
            if (preselected != null) dropdown.setText(preselected.categoryName, false)
        }
    }

    private fun saveSavings() {
        val title = findViewById<EditText>(R.id.etFillTitle).text?.toString()?.trim().orEmpty()
        val amountText = findViewById<EditText>(R.id.etFillAmount).text?.toString()?.trim().orEmpty()
            .replace("R", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", "")
        val categoryName = findViewById<AutoCompleteTextView>(R.id.actFillCategory).text?.toString()?.trim().orEmpty()
        val category = goals.firstOrNull { it.categoryName.equals(categoryName, ignoreCase = true) }
        when {
            goals.isEmpty() -> Toast.makeText(this, R.string.no_categories_yet, Toast.LENGTH_SHORT).show()
            title.isBlank() || amountText.isBlank() -> Toast.makeText(this, R.string.expense_fields_required, Toast.LENGTH_SHORT).show()
            category == null -> Toast.makeText(this, R.string.select_category, Toast.LENGTH_SHORT).show()
            else -> {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount < 0.0) {
                    Toast.makeText(this, R.string.expense_amount_invalid, Toast.LENGTH_SHORT).show()
                    return
                }
                val now = LocalTime.now().withSecond(0).withNano(0)
                val app = application as BudgetTreeApplication
                lifecycleScope.launch {
                    val result = app.incomeDatabaseSystem.createIncome(
                        category = category,
                        description = title,
                        amount = amount,
                        date = selectedDate,
                        startTime = now,
                        endTime = now
                    )
                    if (result.wasSuccessful) {
                        Toast.makeText(this@FillWateringCanActivity, R.string.savings_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@FillWateringCanActivity,
                            result.errMsg ?: getString(R.string.expense_fields_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun bindDate() {
        findViewById<TextView>(R.id.tvFillDate).text = dateFormatter.format(selectedDate)
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
