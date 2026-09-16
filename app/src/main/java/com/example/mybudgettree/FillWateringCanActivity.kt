package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.mybudgettree.database.entries.SavingsGoal
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class FillWateringCanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FillWateringCanActivity"
        const val EXTRA_GOAL_ID = "goal_id"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    private var selectedDate = LocalDate.now()
    private var goals: List<SavingsGoal> = emptyList()

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
            goals = app.savingsGoalDatabaseSystem.getAllGoalsForUser(user).goals.orEmpty()
            val names = goals.map { it.goalName }
            val dropdown = findViewById<AutoCompleteTextView>(R.id.actFillCategory)
            dropdown.threshold = 0
            dropdown.setAdapter(ArrayAdapter(this@FillWateringCanActivity, android.R.layout.simple_dropdown_item_1line, names))
            dropdown.setOnClickListener { dropdown.showDropDown() }
            val preselectId = intent.getLongExtra(EXTRA_GOAL_ID, -1L)
            val preselected = goals.firstOrNull { it.id == preselectId }
            if (preselected != null) dropdown.setText(preselected.goalName, false)
        }
    }

    private fun saveSavings() {
        val amountText = findViewById<EditText>(R.id.etFillAmount).text?.toString()?.trim().orEmpty()
            .replace("R", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", "")
        val goalName = findViewById<AutoCompleteTextView>(R.id.actFillCategory).text?.toString()?.trim().orEmpty()
        val goal = goals.firstOrNull { it.goalName.equals(goalName, ignoreCase = true) }
        when {
            goals.isEmpty() -> Toast.makeText(this, R.string.no_goals_yet, Toast.LENGTH_SHORT).show()
            amountText.isBlank() -> Toast.makeText(this, R.string.expense_fields_required, Toast.LENGTH_SHORT).show()
            goal == null -> Toast.makeText(this, R.string.select_goal, Toast.LENGTH_SHORT).show()
            else -> {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount < 0.0) {
                    Toast.makeText(this, R.string.expense_amount_invalid, Toast.LENGTH_SHORT).show()
                    return
                }
                val app = application as BudgetTreeApplication
                lifecycleScope.launch {
                    val result = app.savingsContributionDatabaseSystem.createContribution(
                        goal = goal,
                        amount = amount,
                        date = selectedDate
                    )
                    if (result.wasSuccessful) {
                        Log.i(TAG, "Saved savings deposit for goal '${goal.goalName}'")
                        Toast.makeText(this@FillWateringCanActivity, R.string.savings_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.w(TAG, "Failed to save savings deposit for goal '${goal.goalName}': ${result.errMsg}")
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
