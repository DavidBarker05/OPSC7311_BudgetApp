package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class EditCategoryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditCategoryActivity"
        const val EXTRA_CATEGORY_ID = "category_id"
    }

    private var category: Category? = null
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
        setContentView(R.layout.activity_edit_category)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editCategoryRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialSwitch>(R.id.switchBudget).setOnCheckedChangeListener { _, isChecked ->
            findViewById<EditText>(R.id.etCategoryBudget).visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        findViewById<MaterialButton>(R.id.btnSaveCategoryEdit).setOnClickListener { saveChanges() }
        findViewById<MaterialButton>(R.id.btnDeleteCategory).setOnClickListener { confirmDelete() }

        loadCategory()
    }

    private fun loadCategory() {
        val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val found = app.categoryDatabaseSystem.findCategory(categoryId).category
            if (found == null) {
                finish()
                return@launch
            }
            category = found
            selectedIconKey = CategoryGarden.iconKeyFor(found)
            findViewById<EditText>(R.id.etCategoryName).setText(found.categoryName)
            IconPicker.populate(findViewById(R.id.iconGrid), selectedIconKey) { key -> selectedIconKey = key }
            val hasBudget = found.budgetAmount != null
            findViewById<MaterialSwitch>(R.id.switchBudget).isChecked = hasBudget
            findViewById<EditText>(R.id.etCategoryBudget).apply {
                visibility = if (hasBudget) View.VISIBLE else View.GONE
                if (hasBudget) setText(plainAmount(found.budgetAmount ?: 0.0))
            }
        }
    }

    private fun saveChanges() {
        val current = category ?: return
        val newName = findViewById<EditText>(R.id.etCategoryName).text?.toString()?.trim().orEmpty()
        if (newName.isBlank()) {
            Toast.makeText(this, R.string.category_name_required, Toast.LENGTH_SHORT).show()
            return
        }
        val budgetEnabled = findViewById<MaterialSwitch>(R.id.switchBudget).isChecked
        val budgetText = findViewById<EditText>(R.id.etCategoryBudget).text?.toString()?.trim().orEmpty()
            .replace("R", "", ignoreCase = true)
            .replace(",", "")
        val newBudget: Double? = if (!budgetEnabled) {
            null
        } else {
            val amount = budgetText.toDoubleOrNull()
            if (amount == null || amount < 0.0) {
                Toast.makeText(this, R.string.expense_amount_invalid, Toast.LENGTH_SHORT).show()
                return
            }
            amount
        }
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            var working = current
            if (working.categoryName != newName) {
                val renameResult = app.categoryDatabaseSystem.updateCategoryName(working, newName)
                if (renameResult.status == CategoryDatabaseSystem.UpdateCategoryReturnStatus.Failed) {
                    Log.w(TAG, "Failed to rename category: ${renameResult.errMsg}")
                    Toast.makeText(this@EditCategoryActivity, renameResult.errMsg, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                renameResult.category?.let { working = it }
            }
            if (working.iconKey != selectedIconKey) {
                app.categoryDatabaseSystem.updateCategoryIcon(working, selectedIconKey).category?.let { working = it }
            }
            if (working.budgetAmount != newBudget) {
                app.categoryDatabaseSystem.updateCategoryBudget(working, newBudget).category?.let { working = it }
            }
            category = working
            Toast.makeText(this@EditCategoryActivity, R.string.category_updated, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun plainAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()

    private fun confirmDelete() {
        val current = category ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_category)
            .setMessage(getString(R.string.delete_category_confirm, current.categoryName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteCategory(current) }
            .show()
    }

    private fun deleteCategory(target: Category) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            app.categoryDatabaseSystem.deleteCategory(target)
            Toast.makeText(this@EditCategoryActivity, R.string.category_deleted, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
