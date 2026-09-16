package com.example.mybudgettree

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class EditTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_IS_INCOME = "is_income"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var isIncome = false
    private var selectedDate = LocalDate.now()
    private var selectedStartTime: LocalTime = LocalTime.MIDNIGHT
    private var selectedEndTime: LocalTime = LocalTime.MIDNIGHT
    private var imagePath: String? = null
    private var imageCleared = false

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
    private var cameraFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        isIncome = intent.getBooleanExtra(EXTRA_IS_INCOME, false)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_edit_transaction)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editTransactionRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<TextView>(R.id.tvEditTransactionTitle).setText(
            if (isIncome) R.string.edit_income else R.string.edit_expense
        )
        findViewById<MaterialButton>(R.id.btnDeleteTransaction).setText(
            if (isIncome) R.string.delete_income else R.string.delete_expense
        )
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.dateField).setOnClickListener { showDatePicker() }
        findViewById<View>(R.id.tvStartTime).setOnClickListener { showTimePicker(isStart = true) }
        findViewById<View>(R.id.tvEndTime).setOnClickListener { showTimePicker(isStart = false) }
        findViewById<View>(R.id.receiptArea).setOnClickListener { showReceiptChooser() }
        findViewById<MaterialButton>(R.id.btnRemoveImage).setOnClickListener {
            imagePath = null
            imageCleared = true
            showReceiptPreview()
        }
        findViewById<MaterialButton>(R.id.btnViewImage).setOnClickListener {
            val path = imagePath ?: return@setOnClickListener
            startActivity(
                Intent(this, ViewImageActivity::class.java)
                    .putExtra(ViewImageActivity.EXTRA_IMAGE_PATH, path)
            )
        }
        findViewById<MaterialButton>(R.id.btnSaveTransaction).setOnClickListener { saveChanges() }
        findViewById<MaterialButton>(R.id.btnDeleteTransaction).setOnClickListener { confirmDelete() }

        loadTransaction()
    }

    private fun loadTransaction() {
        val id = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            if (isIncome) {
                val income = app.incomeDatabaseSystem.findIncome(id).income
                if (income == null) {
                    finish()
                    return@launch
                }
                findViewById<EditText>(R.id.etTransactionTitle).setText(income.description)
                findViewById<EditText>(R.id.etTransactionAmount).setText(plainAmount(income.amount))
                selectedDate = income.date
                selectedStartTime = income.startTime
                selectedEndTime = income.endTime
                imagePath = income.imagePath
            } else {
                val expense = app.expenseDatabaseSystem.findExpense(id).expense
                if (expense == null) {
                    finish()
                    return@launch
                }
                findViewById<EditText>(R.id.etTransactionTitle).setText(expense.description)
                findViewById<EditText>(R.id.etTransactionAmount).setText(plainAmount(expense.amount))
                selectedDate = expense.date
                selectedStartTime = expense.startTime
                selectedEndTime = expense.endTime
                imagePath = expense.imagePath
            }
            bindDate()
            bindTimes()
            showReceiptPreview()
        }
    }

    private fun saveChanges() {
        val id = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val title = findViewById<EditText>(R.id.etTransactionTitle).text?.toString()?.trim().orEmpty()
        val amountText = findViewById<EditText>(R.id.etTransactionAmount).text?.toString()?.trim().orEmpty()
            .replace("R", "", ignoreCase = true)
            .replace(",", "")
        if (title.isBlank() || amountText.isBlank()) {
            Toast.makeText(this, R.string.expense_fields_required, Toast.LENGTH_SHORT).show()
            return
        }
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount < 0.0) {
            Toast.makeText(this, R.string.expense_amount_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            if (isIncome) {
                val income = app.incomeDatabaseSystem.findIncome(id).income ?: return@launch
                var working = income
                if (working.description != title) app.incomeDatabaseSystem.updateIncomeDescription(working, title).income?.let { working = it }
                if (working.amount != amount) app.incomeDatabaseSystem.updateIncomeAmount(working, amount).income?.let { working = it }
                if (working.date != selectedDate) app.incomeDatabaseSystem.updateIncomeDate(working, selectedDate).income?.let { working = it }
                if (working.startTime != selectedStartTime) {
                    val result = app.incomeDatabaseSystem.updateIncomeStartTime(working, selectedStartTime)
                    if (result.status == com.example.mybudgettree.database.managers.IncomeDatabaseSystem.UpdateIncomeReturnStatus.Failed) {
                        Toast.makeText(this@EditTransactionActivity, result.errMsg, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    result.income?.let { working = it }
                }
                if (working.endTime != selectedEndTime) {
                    val result = app.incomeDatabaseSystem.updateIncomeEndTime(working, selectedEndTime)
                    if (result.status == com.example.mybudgettree.database.managers.IncomeDatabaseSystem.UpdateIncomeReturnStatus.Failed) {
                        Toast.makeText(this@EditTransactionActivity, result.errMsg, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    result.income?.let { working = it }
                }
                if (imageCleared || working.imagePath != imagePath) app.incomeDatabaseSystem.updateIncomeImage(working, imagePath)
                Toast.makeText(this@EditTransactionActivity, R.string.income_updated, Toast.LENGTH_SHORT).show()
            } else {
                val expense = app.expenseDatabaseSystem.findExpense(id).expense ?: return@launch
                var working = expense
                if (working.description != title) app.expenseDatabaseSystem.updateExpenseDescription(working, title).expense?.let { working = it }
                if (working.amount != amount) app.expenseDatabaseSystem.updateExpenseAmount(working, amount).expense?.let { working = it }
                if (working.date != selectedDate) app.expenseDatabaseSystem.updateExpenseDate(working, selectedDate).expense?.let { working = it }
                if (working.startTime != selectedStartTime) {
                    val result = app.expenseDatabaseSystem.updateExpenseStartTime(working, selectedStartTime)
                    if (result.status == com.example.mybudgettree.database.managers.ExpenseDatabaseSystem.UpdateExpenseReturnStatus.Failed) {
                        Toast.makeText(this@EditTransactionActivity, result.errMsg, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    result.expense?.let { working = it }
                }
                if (working.endTime != selectedEndTime) {
                    val result = app.expenseDatabaseSystem.updateExpenseEndTime(working, selectedEndTime)
                    if (result.status == com.example.mybudgettree.database.managers.ExpenseDatabaseSystem.UpdateExpenseReturnStatus.Failed) {
                        Toast.makeText(this@EditTransactionActivity, result.errMsg, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    result.expense?.let { working = it }
                }
                if (imageCleared || working.imagePath != imagePath) app.expenseDatabaseSystem.updateExpenseImage(working, imagePath)
                Toast.makeText(this@EditTransactionActivity, R.string.expense_updated, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun confirmDelete() {
        val message = if (isIncome) R.string.delete_income_confirm else R.string.delete_expense_confirm
        val title = if (isIncome) R.string.delete_income else R.string.delete_expense
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteTransaction() }
            .show()
    }

    private fun deleteTransaction() {
        val id = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            if (isIncome) {
                val income = app.incomeDatabaseSystem.findIncome(id).income
                if (income != null) app.incomeDatabaseSystem.deleteIncome(income)
                Toast.makeText(this@EditTransactionActivity, R.string.income_deleted, Toast.LENGTH_SHORT).show()
            } else {
                val expense = app.expenseDatabaseSystem.findExpense(id).expense
                if (expense != null) app.expenseDatabaseSystem.deleteExpense(expense)
                Toast.makeText(this@EditTransactionActivity, R.string.expense_deleted, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun bindDate() {
        findViewById<TextView>(R.id.tvTransactionDate).text = dateFormatter.format(selectedDate)
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

    private fun bindTimes() {
        findViewById<TextView>(R.id.tvStartTime).text = timeFormatter.format(selectedStartTime)
        findViewById<TextView>(R.id.tvEndTime).text = timeFormatter.format(selectedEndTime)
    }

    private fun showTimePicker(isStart: Boolean) {
        val initial = if (isStart) selectedStartTime else selectedEndTime
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val time = LocalTime.of(hour, minute)
                if (isStart) selectedStartTime = time else selectedEndTime = time
                bindTimes()
            },
            initial.hour,
            initial.minute,
            true
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
        val file = File(cacheDir, "transaction_${System.currentTimeMillis()}.jpg")
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
            imagePath = app.imageStorageSystem.saveImage(bytes, "transaction_${System.currentTimeMillis()}.jpg")
            imageCleared = false
            showReceiptPreview()
        }
    }

    private fun saveReceiptFromFile(file: File) {
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            imagePath = app.imageStorageSystem.saveImage(bytes, "transaction_${System.currentTimeMillis()}.jpg")
            imageCleared = false
            showReceiptPreview()
        }
    }

    private fun showReceiptPreview() {
        val path = imagePath
        val hint = findViewById<TextView>(R.id.tvReceiptHint)
        val preview = findViewById<ImageView>(R.id.ivReceiptPreview)
        val removeButton = findViewById<MaterialButton>(R.id.btnRemoveImage)
        val viewButton = findViewById<MaterialButton>(R.id.btnViewImage)
        if (path.isNullOrBlank()) {
            hint.visibility = View.VISIBLE
            hint.setText(R.string.no_image)
            preview.visibility = View.GONE
            removeButton.visibility = View.GONE
            viewButton.visibility = View.GONE
            return
        }
        val bitmap = BitmapFactory.decodeFile(path)
        if (bitmap == null) {
            hint.visibility = View.VISIBLE
            hint.setText(R.string.no_image)
            preview.visibility = View.GONE
            removeButton.visibility = View.GONE
            viewButton.visibility = View.GONE
            return
        }
        hint.visibility = View.GONE
        preview.visibility = View.VISIBLE
        preview.setImageBitmap(bitmap)
        removeButton.visibility = View.VISIBLE
        viewButton.visibility = View.VISIBLE
    }

    private fun plainAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
}
