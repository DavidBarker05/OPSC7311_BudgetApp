package com.example.mybudgettree

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.YearMonth

class CategoriesActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CategoriesActivity"
    }

    private val adapter = CategoryGardenAdapter(
        onCategory = { category ->
            startActivity(
                Intent(this, CategoryDetailActivity::class.java)
                    .putExtra(CategoryDetailActivity.EXTRA_CATEGORY_ID, category.id)
            )
        },
        onMore = { showNewCategoryDialog() }
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
        setContentView(R.layout.activity_categories)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoriesRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<RecyclerView>(R.id.rvCategories).apply {
            layoutManager = GridLayoutManager(this@CategoriesActivity, 3)
            adapter = this@CategoriesActivity.adapter
        }
        findViewById<ImageButton>(R.id.btnEditMonthlyGoal).setOnClickListener { showMonthlyGoalDialog() }
        MainNavigation.bind(this, MainNavigation.Tab.CATEGORIES)
    }

    override fun onResume() {
        super.onResume()
        loadGarden()
    }

    private fun loadGarden() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            var categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val hasAnyDefaultCategory = categories.any { existing ->
                CategoryGarden.defaultNames.any { it.equals(existing.categoryName, ignoreCase = true) }
            }
            if (!hasAnyDefaultCategory) {
                CategoryGarden.defaultNames.forEach { name ->
                    app.categoryDatabaseSystem.createCategory(user, name)
                }
                categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            }
            val currentMonth = YearMonth.now()
            val expensesThisMonth = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
                .filter { YearMonth.from(it.date) == currentMonth }
            val incomesThisMonth = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
                .filter { YearMonth.from(it.date) == currentMonth }
            val spentThisMonth = expensesThisMonth.sumOf { it.amount }
            val monthlyGoal = app.monthlyGoalDatabaseSystem.getGoal(user, currentMonth)
            BudgetOverview.bind(this@CategoriesActivity, incomesThisMonth, expensesThisMonth, spentThisMonth, monthlyGoal?.maxGoal ?: 0.0)
            adapter.submit(CategoryGarden.sort(categories))
        }
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
                    Toast.makeText(this@CategoriesActivity, R.string.monthly_goal_saved, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadGarden()
                } else {
                    Log.w(TAG, "Failed to save monthly goal: ${result.errMsg}")
                    Toast.makeText(this@CategoriesActivity, result.errMsg, Toast.LENGTH_SHORT).show()
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

    private fun showNewCategoryDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_new_category)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val nameField = dialog.findViewById<EditText>(R.id.etNewCategoryName)
        dialog.findViewById<MaterialButton>(R.id.btnCancelCategory).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<MaterialButton>(R.id.btnSaveCategory).setOnClickListener {
            val name = nameField.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                Toast.makeText(this, R.string.category_name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val user = UserSession.currentUser ?: return@setOnClickListener
            val app = application as BudgetTreeApplication
            lifecycleScope.launch {
                val result = app.categoryDatabaseSystem.createCategory(user, name)
                if (result.wasSuccessful) {
                    Log.i(TAG, "Created category '$name'")
                    Toast.makeText(this@CategoriesActivity, R.string.category_created, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadGarden()
                } else {
                    Log.w(TAG, "Failed to create category '$name': ${result.errMsg}")
                    Toast.makeText(
                        this@CategoriesActivity,
                        result.errMsg ?: getString(R.string.category_name_required),
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
}
