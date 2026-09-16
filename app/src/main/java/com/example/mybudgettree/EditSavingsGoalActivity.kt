package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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

class EditSavingsGoalActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditSavingsGoalActivity"
        const val EXTRA_GOAL_ID = "goal_id"
    }

    private var goal: SavingsGoal? = null
    private var selectedIconKey: String? = null

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
        setContentView(R.layout.activity_edit_savings_goal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editGoalRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSaveGoalEdit).setOnClickListener { saveChanges() }
        findViewById<MaterialButton>(R.id.btnDeleteGoalEdit).setOnClickListener { confirmDelete() }

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
            selectedIconKey = found.iconKey ?: IconCatalog.DEFAULT_KEY
            findViewById<EditText>(R.id.etGoalName).setText(found.goalName)
            IconPicker.populate(findViewById(R.id.iconGrid), selectedIconKey) { key -> selectedIconKey = key }
            found.targetAmount?.let { findViewById<EditText>(R.id.etGoalTarget).setText(plainAmount(it)) }
        }
    }

    private fun saveChanges() {
        val current = goal ?: return
        val newName = findViewById<EditText>(R.id.etGoalName).text?.toString()?.trim().orEmpty()
        if (newName.isBlank()) {
            Toast.makeText(this, R.string.goal_name_required, Toast.LENGTH_SHORT).show()
            return
        }
        val targetText = findViewById<EditText>(R.id.etGoalTarget).text?.toString()?.trim().orEmpty()
            .replace("R", "", ignoreCase = true)
            .replace(",", "")
        val newTarget = targetText.toDoubleOrNull()
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            var working = current
            if (working.goalName != newName) {
                val renameResult = app.savingsGoalDatabaseSystem.updateGoalName(working, newName)
                if (renameResult.status == com.example.mybudgettree.database.managers.SavingsGoalDatabaseSystem.UpdateGoalReturnStatus.Failed) {
                    Toast.makeText(this@EditSavingsGoalActivity, renameResult.errMsg, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                renameResult.goal?.let { working = it }
            }
            if (working.iconKey != selectedIconKey) {
                app.savingsGoalDatabaseSystem.updateGoalIcon(working, selectedIconKey).goal?.let { working = it }
            }
            if (working.targetAmount != newTarget) {
                app.savingsGoalDatabaseSystem.updateGoalTarget(working, newTarget).goal?.let { working = it }
            }
            goal = working
            Log.i(TAG, "Updated savings goal '${working.goalName}'")
            Toast.makeText(this@EditSavingsGoalActivity, R.string.goal_updated, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDelete() {
        val current = goal ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_goal)
            .setMessage(getString(R.string.delete_goal_confirm, current.goalName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteGoal(current) }
            .show()
    }

    private fun deleteGoal(target: SavingsGoal) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            app.savingsGoalDatabaseSystem.deleteGoal(target)
            Toast.makeText(this@EditSavingsGoalActivity, R.string.goal_deleted, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun plainAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}
