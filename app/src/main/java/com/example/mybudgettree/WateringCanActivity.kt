package com.example.mybudgettree

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth

class WateringCanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WateringCanActivity"
    }

    private var selectedIconKey: String? = IconCatalog.DEFAULT_KEY
    private var featuredGoal: SavingsGoal? = null
    private val adapter = GoalTileAdapter(
        onGoal = { goal ->
            startActivity(
                Intent(this, GoalDetailActivity::class.java)
                    .putExtra(GoalDetailActivity.EXTRA_GOAL_ID, goal.id)
            )
        },
        onMore = { showNewGoalDialog() }
    )

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
        findViewById<MaterialButton>(R.id.btnWaterTree).setOnClickListener { waterFeaturedTree() }
        findViewById<ImageButton>(R.id.btnEditMonthlyGoal).setOnClickListener { showMonthlyGoalDialog() }
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
            val goals = app.savingsGoalDatabaseSystem.getAllGoalsForUser(user).values.orEmpty()
            val currentMonth = YearMonth.now()
            val expensesThisMonth = app.expenseDatabaseSystem.retrieveAllExpenses(user).values.orEmpty()
                .filter { YearMonth.from(it.dateAsLocalDate()) == currentMonth }
            val incomesThisMonth = app.incomeDatabaseSystem.retrieveAllIncomes(user).values.orEmpty()
                .filter { YearMonth.from(it.dateAsLocalDate()) == currentMonth }
            val spentThisMonth = expensesThisMonth.sumOf { it.amount }
            val monthlyGoal = app.monthlyGoalDatabaseSystem.getGoal(user, currentMonth)
            BudgetOverview.bind(
                this@WateringCanActivity,
                incomesThisMonth,
                expensesThisMonth,
                spentThisMonth,
                monthlyGoal?.maxGoal ?: 0.0,
                monthlyGoal?.minGoal ?: 0.0
            )
            adapter.submit(goals)
            val snapshots = goals.map { goal ->
                val contributions = app.savingsContributionDatabaseSystem
                    .retrieveAllContributionsForGoal(goal).values.orEmpty()
                GoalCanSnapshot(goal, GoalTreeGame.canState(goal, contributions))
            }
            val featured = GoalTreeGame.featured(snapshots)
            featuredGoal = featured?.goal
            val can = featured?.can
            findViewById<TextView>(R.id.tvCanForGoal).text = if (featured == null) {
                getString(R.string.tree_needs_goal)
            } else {
                getString(R.string.watering_can_for, featured.goal.goalName)
            }
            findViewById<WateringCanView>(R.id.wateringCanView).setFillPercent(can?.fillPercent ?: 0)
            findViewById<TextView>(R.id.tvLastFilled).text =
                GoalTreeUi.lastFilledLine(this@WateringCanActivity, can?.lastFilled)
            val level = featured?.goal?.treeLevel ?: 1
            findViewById<MoneyTreeView>(R.id.moneyTreeView).setLevel(level)
            findViewById<TextView>(R.id.tvTreeStage).text = if (featured == null) {
                ""
            } else {
                GoalTreeUi.stageLine(this@WateringCanActivity, featured.goal.goalName, level)
            }
            findViewById<TextView>(R.id.tvGrowthFeedback).text = GoalTreeUi.progressMessage(
                this@WateringCanActivity,
                featured?.goal?.goalName,
                level,
                can
            )
        }
    }

    private fun waterFeaturedTree() {
        val goal = featuredGoal
        if (goal == null) {
            Toast.makeText(this, R.string.tree_needs_goal, Toast.LENGTH_SHORT).show()
            return
        }
        val button = findViewById<MaterialButton>(R.id.btnWaterTree)
        val app = application as BudgetTreeApplication
        button.isEnabled = false
        lifecycleScope.launch {
            val result = app.savingsGoalDatabaseSystem.waterTree(goal, LocalDateTime.now())
            button.isEnabled = true
            val grown = result.value
            if (result.status == UpdateReturnStatus.Succeeded && grown != null) {
                Log.i(TAG, "Watered tree for goal '${goal.goalName}' to level ${grown.treeLevel}")
                Toast.makeText(
                    this@WateringCanActivity,
                    GoalTreeUi.grewMessage(this@WateringCanActivity, goal.goalName, grown.treeLevel),
                    Toast.LENGTH_LONG
                ).show()
                loadGoals()
            } else {
                Log.w(TAG, "Failed to water tree for goal '${goal.goalName}': ${result.errMsg}")
                val message = if (result.errMsg?.contains(GoalTreeGame.EMPTY_CAN_MESSAGE, ignoreCase = true) == true) {
                    getString(R.string.can_empty)
                } else {
                    result.errMsg ?: getString(R.string.can_empty)
                }
                Toast.makeText(this@WateringCanActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNewGoalDialog() {
        val user = UserSession.currentUser ?: return
        selectedIconKey = IconCatalog.DEFAULT_KEY
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_new_savings_goal)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val nameField = dialog.findViewById<EditText>(R.id.etNewGoalName)
        val targetField = dialog.findViewById<EditText>(R.id.etNewGoalTarget)
        IconPicker.populate(dialog.findViewById(R.id.iconGrid), selectedIconKey) { key -> selectedIconKey = key }
        dialog.findViewById<MaterialButton>(R.id.btnCancelGoal).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<MaterialButton>(R.id.btnSaveGoal).setOnClickListener {
            val name = nameField.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                Toast.makeText(this, R.string.goal_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targetText = targetField.text?.toString()?.trim().orEmpty()
                .replace("R", "", ignoreCase = true)
                .replace(",", "")
            val target = targetText.toDoubleOrNull()
            val app = application as BudgetTreeApplication
            lifecycleScope.launch {
                val result = app.savingsGoalDatabaseSystem.createGoal(user, name, selectedIconKey, target)
                if (result.wasSuccessful) {
                    Log.i(TAG, "Created savings goal '$name'")
                    Toast.makeText(this@WateringCanActivity, R.string.goal_created, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadGoals()
                } else {
                    Log.w(TAG, "Failed to create savings goal '$name': ${result.errMsg}")
                    Toast.makeText(
                        this@WateringCanActivity,
                        result.errMsg ?: getString(R.string.goal_name_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.82).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showMonthlyGoalDialog() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_monthly_goal)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val minField = dialog.findViewById<EditText>(R.id.etMonthlyGoalMin)
        val maxField = dialog.findViewById<EditText>(R.id.etMonthlyGoalMax)
        lifecycleScope.launch {
            val existing = app.monthlyGoalDatabaseSystem.getGoal(user, YearMonth.now())
            if (existing != null) {
                minField.setText(plainAmount(existing.minGoal))
                maxField.setText(plainAmount(existing.maxGoal))
            }
        }
        dialog.findViewById<MaterialButton>(R.id.btnCancelMonthlyGoal).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<MaterialButton>(R.id.btnSaveMonthlyGoal).setOnClickListener {
            val min = minField.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            val max = maxField.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            lifecycleScope.launch {
                val result = app.monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.now(), min, max)
                if (result.wasSuccessful) {
                    Log.i(TAG, "Saved monthly goal min=$min max=$max")
                    Toast.makeText(this@WateringCanActivity, R.string.monthly_goal_saved, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadGoals()
                } else {
                    Log.w(TAG, "Failed to save monthly goal: ${result.errMsg}")
                    Toast.makeText(this@WateringCanActivity, result.errMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.82).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun plainAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}
