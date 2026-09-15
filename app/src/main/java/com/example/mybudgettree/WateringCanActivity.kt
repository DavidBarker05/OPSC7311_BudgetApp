package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class WateringCanActivity : AppCompatActivity() {
    private val adapter = GoalTileAdapter { goal ->
        startActivity(
            Intent(this, GoalDetailActivity::class.java)
                .putExtra(GoalDetailActivity.EXTRA_CATEGORY_ID, goal.id)
        )
    }

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
        setContentView(R.layout.activity_watering_can)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.wateringCanRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<RecyclerView>(R.id.rvGoals).apply {
            layoutManager = GridLayoutManager(this@WateringCanActivity, 3)
            adapter = this@WateringCanActivity.adapter
        }
        findViewById<MaterialButton>(R.id.btnSaveMore).setOnClickListener {
            startActivity(Intent(this, FillWateringCanActivity::class.java))
        }
        MainNavigation.bind(this, MainNavigation.Tab.GOALS)
    }

    override fun onResume() {
        super.onResume()
        loadGoals()
    }

    private fun loadGoals() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val goals = CategoryGoals.ensureForUser(user, app.categoryDatabaseSystem)
            val allCategories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            BudgetOverview.bind(this@WateringCanActivity, incomes, expenses, allCategories)
            adapter.submit(goals)
            val goalIds = goals.map { it.id }.toSet()
            val saved = incomes.filter { it.categoryId in goalIds }.sumOf { it.amount }
            val target = goals.mapNotNull { it.budgetAmount }.sum()
            val percent = if (target <= 0.0) 0 else ((saved / target) * 100.0).toInt().coerceIn(0, 100)
            findViewById<WateringCanView>(R.id.wateringCanView).setFillPercent(percent)
        }
    }
}
