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
            if (categories.isEmpty()) {
                CategoryGarden.defaultNames.forEach { name ->
                    app.categoryDatabaseSystem.createCategory(user, name)
                }
                categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            }
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            BudgetOverview.bind(this@CategoriesActivity, incomes, expenses, categories)
            adapter.submit(CategoryGarden.sort(CategoryGoals.spendingOnly(categories)))
        }
    }

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
