package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class GoalDetailActivity : AppCompatActivity() {
    private val adapter = SavingsContributionAdapter(::removeContribution)
    private var goal: SavingsGoal? = null

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
        setContentView(R.layout.activity_goal_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.goalDetailRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnDeleteGoal).setOnClickListener { confirmDeleteGoal() }
        findViewById<ImageButton>(R.id.btnEditGoal).setOnClickListener {
            val id = goal?.id ?: return@setOnClickListener
            startActivity(
                Intent(this, EditSavingsGoalActivity::class.java)
                    .putExtra(EditSavingsGoalActivity.EXTRA_GOAL_ID, id)
            )
        }
        findViewById<MaterialButton>(R.id.btnAddSavings).setOnClickListener {
            val id = goal?.id ?: return@setOnClickListener
            startActivity(
                Intent(this, FillWateringCanActivity::class.java)
                    .putExtra(FillWateringCanActivity.EXTRA_GOAL_ID, id)
            )
        }
        findViewById<RecyclerView>(R.id.rvGoalSavings).apply {
            layoutManager = LinearLayoutManager(this@GoalDetailActivity)
            adapter = this@GoalDetailActivity.adapter
        }
        findViewById<DonutTargetView>(R.id.goalDonut).apply {
            setShowPercent(false)
            setRingColors(getColor(R.color.analysis_donut_track), getColor(R.color.home_header))
        }
        MainNavigation.bind(this, MainNavigation.Tab.GOALS)
    }

    override fun onResume() {
        super.onResume()
        loadGoal()
    }

    private fun loadGoal() {
        val goalId = intent.getLongExtra(EXTRA_GOAL_ID, -1L)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val found = app.savingsGoalDatabaseSystem.findGoal(goalId).goal
            if (found == null) {
                finish()
                return@launch
            }
            goal = found
            findViewById<TextView>(R.id.tvGoalTitle).text = found.goalName
            findViewById<TextView>(R.id.tvGoalBadgeName).text = found.goalName
            findViewById<ImageView>(R.id.ivGoalBadge).apply {
                setImageResource(IconCatalog.resFor(found.iconKey))
                setColorFilter(getColor(R.color.green_text))
            }

            val contributions = app.savingsContributionDatabaseSystem.retrieveAllContributionsForGoal(found).contributions.orEmpty()
            val saved = contributions.sumOf { it.amount }
            val target = found.targetAmount ?: 0.0
            val percent = if (target <= 0.0) 0 else ((saved / target) * 100.0).toInt().coerceIn(0, 100)
            findViewById<TextView>(R.id.tvGoalAmount).text = MoneyFormatter.format(target)
            findViewById<TextView>(R.id.tvAmountSaved).text = MoneyFormatter.format(saved)
            findViewById<TextView>(R.id.tvGoalPercent).text = getString(R.string.budget_percent, percent)
            findViewById<TextView>(R.id.tvGoalTarget).text = MoneyFormatter.format(target)
            findViewById<DonutTargetView>(R.id.goalDonut).setPercent(percent)
            val fill = findViewById<android.view.View>(R.id.goalFill)
            val params = fill.layoutParams as ConstraintLayout.LayoutParams
            params.matchConstraintPercentWidth = percent / 100f
            fill.layoutParams = params
            findViewById<TextView>(R.id.tvGoalStatus).text = when {
                target <= 0.0 -> getString(R.string.expenses_status_no_budget)
                percent >= 100 -> getString(R.string.expenses_status_good, percent)
                percent >= 70 -> getString(R.string.expenses_status_good, percent)
                else -> getString(R.string.expenses_status_watch, percent)
            }

            val sorted = contributions.sortedByDescending { it.date }
            adapter.submit(sorted)
            findViewById<TextView>(R.id.tvEmptyGoal).visibility =
                if (sorted.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun removeContribution(contribution: SavingsContribution) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            app.savingsContributionDatabaseSystem.deleteContribution(contribution)
            android.widget.Toast.makeText(this@GoalDetailActivity, R.string.savings_removed, android.widget.Toast.LENGTH_SHORT).show()
            loadGoal()
        }
    }

    private fun confirmDeleteGoal() {
        val current = goal ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_goal)
            .setMessage(getString(R.string.delete_goal_confirm, current.goalName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                val app = application as BudgetTreeApplication
                lifecycleScope.launch {
                    app.savingsGoalDatabaseSystem.deleteGoal(current)
                    android.widget.Toast.makeText(this@GoalDetailActivity, R.string.goal_deleted, android.widget.Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_GOAL_ID = "goal_id"
    }
}
