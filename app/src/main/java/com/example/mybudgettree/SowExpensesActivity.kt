package com.example.mybudgettree

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.mybudgettree.database.entries.Category
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SowExpensesActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SowExpensesActivity"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    private var selectedDate = LocalDate.now()
    private var categories: List<Category> = emptyList()
    private var receiptPath: String? = null
    private var cameraFile: File? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) saveReceiptFromUri(uri)
    }
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraFile
        if (success && file != null) saveReceiptFromFile(file)
    }
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_SHORT).show()
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
        setContentView(R.layout.activity_sow_expenses)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sowRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View>(R.id.dateField).setOnClickListener { showDatePicker() }
        findViewById<View>(R.id.receiptArea).setOnClickListener { showReceiptChooser() }
        findViewById<MaterialButton>(R.id.btnSaveExpense).setOnClickListener { saveExpense() }
        bindDate()
        MainNavigation.bind(this, MainNavigation.Tab.CATEGORIES)
        loadCategories()
    }

    private fun loadCategories() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            categories = CategoryGarden.sort(
                CategoryGoals.spendingOnly(
                    app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
                )
            )
            val names = categories.map { it.categoryName }
            val dropdown = findViewById<AutoCompleteTextView>(R.id.actSowCategory)
            dropdown.threshold = 0
            dropdown.setAdapter(ArrayAdapter(this@SowExpensesActivity, android.R.layout.simple_dropdown_item_1line, names))
            dropdown.setOnClickListener { dropdown.showDropDown() }
            val preselectId = intent.getLongExtra(CategoryDetailActivity.EXTRA_CATEGORY_ID, -1L)
            val preselected = categories.firstOrNull { it.id == preselectId } ?: categories.firstOrNull()
            if (preselected != null) dropdown.setText(preselected.categoryName, false)
        }
    }

    private fun saveExpense() {
        if (UserSession.currentUser == null) return
        val title = findViewById<EditText>(R.id.etExpenseTitle).text?.toString()?.trim().orEmpty()
        val message = findViewById<EditText>(R.id.etMessage).text?.toString()?.trim().orEmpty()
        val amountText = findViewById<EditText>(R.id.etAmount).text?.toString()?.trim().orEmpty()
            .replace("R", "", ignoreCase = true)
            .replace("$", "")
            .replace(",", "")
        val categoryName = findViewById<AutoCompleteTextView>(R.id.actSowCategory).text?.toString()?.trim().orEmpty()
        val category = categories.firstOrNull { it.categoryName.equals(categoryName, ignoreCase = true) }
        when {
            categories.isEmpty() -> Toast.makeText(this, R.string.no_categories_yet, Toast.LENGTH_SHORT).show()
            title.isBlank() || amountText.isBlank() -> Toast.makeText(this, R.string.expense_fields_required, Toast.LENGTH_SHORT).show()
            category == null -> Toast.makeText(this, R.string.select_category, Toast.LENGTH_SHORT).show()
            else -> {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount < 0.0) {
                    Toast.makeText(this, R.string.expense_amount_invalid, Toast.LENGTH_SHORT).show()
                    return
                }
                val description = if (message.isBlank()) title else "$title\n$message"
                val now = LocalTime.now().withSecond(0).withNano(0)
                val app = application as BudgetTreeApplication
                lifecycleScope.launch {
                    val result = app.expenseDatabaseSystem.createExpense(
                        category = category,
                        description = description,
                        amount = amount,
                        date = selectedDate,
                        startTime = now,
                        endTime = now,
                        imagePath = receiptPath
                    )
                    if (result.wasSuccessful) {
                        Log.i(TAG, "Saved expense '$title' for category '${category.categoryName}'")
                        Toast.makeText(this@SowExpensesActivity, R.string.expense_saved, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.w(TAG, "Failed to save expense '$title': ${result.errMsg}")
                        Toast.makeText(
                            this@SowExpensesActivity,
                            result.errMsg ?: getString(R.string.expense_fields_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun bindDate() {
        findViewById<TextView>(R.id.tvSowDate).text = dateFormatter.format(selectedDate)
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

    private fun showReceiptChooser() {
        AlertDialog.Builder(this)
            .setItems(arrayOf(getString(R.string.upload_image), getString(R.string.take_photo))) { _, which ->
                if (which == 0) pickImage.launch("image/*") else ensureCameraPermission()
            }
            .show()
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val file = File(cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
        cameraFile = file
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePicture.launch(uri)
    }

    private fun saveReceiptFromUri(uri: Uri) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            receiptPath = app.imageStorageSystem.saveImage(bytes, "expense_${System.currentTimeMillis()}.jpg")
            showReceiptPreview()
        }
    }

    private fun saveReceiptFromFile(file: File) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            receiptPath = app.imageStorageSystem.saveImage(bytes, "expense_${System.currentTimeMillis()}.jpg")
            showReceiptPreview()
        }
    }

    private fun showReceiptPreview() {
        val path = receiptPath ?: return
        val bitmap = BitmapFactory.decodeFile(path)
        findViewById<TextView>(R.id.tvReceiptHint).visibility = View.GONE
        findViewById<ImageView>(R.id.ivReceiptPreview).apply {
            visibility = View.VISIBLE
            setImageBitmap(bitmap)
        }
        Toast.makeText(this, R.string.receipt_added, Toast.LENGTH_SHORT).show()
    }
}
