package com.example.mybudgettree

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar

class SignupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SignupActivity"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd / MM / yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val fullName = findViewById<EditText>(R.id.etFullName)
        val email = findViewById<EditText>(R.id.etEmail)
        val phone = findViewById<EditText>(R.id.etPhone)
        val dateOfBirth = findViewById<EditText>(R.id.etDateOfBirth)
        val password = findViewById<TextInputEditText>(R.id.etPassword)
        val confirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val signupButton = findViewById<MaterialButton>(R.id.btnSignup)

        dateOfBirth.setOnClickListener { showDatePicker(dateOfBirth) }

        signupButton.setOnClickListener {
            val nameValue = fullName.text?.toString()?.trim().orEmpty()
            val emailValue = email.text?.toString()?.trim().orEmpty()
            val phoneValue = phone.text?.toString()?.trim().orEmpty()
            val dobValue = dateOfBirth.text?.toString()?.trim().orEmpty()
            val passwordValue = password.text?.toString().orEmpty()
            val confirmValue = confirmPassword.text?.toString().orEmpty()

            when {
                nameValue.isBlank() || emailValue.isBlank() || phoneValue.isBlank() ||
                    dobValue.isBlank() || passwordValue.isBlank() || confirmValue.isBlank() -> {
                    Toast.makeText(this, R.string.signup_fields_required, Toast.LENGTH_SHORT).show()
                }
                passwordValue != confirmValue -> {
                    Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val parsedDob = parseDateOfBirth(dobValue)
                    if (parsedDob == null) {
                        Toast.makeText(this, R.string.signup_invalid_date, Toast.LENGTH_SHORT).show()
                    } else {
                        createAccount(
                            signupButton = signupButton,
                            fullName = nameValue,
                            email = emailValue,
                            phone = phoneValue,
                            dateOfBirth = parsedDob,
                            password = passwordValue
                        )
                    }
                }
            }
        }
        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun createAccount(
        signupButton: MaterialButton,
        fullName: String,
        email: String,
        phone: String,
        dateOfBirth: LocalDate,
        password: String
    ) {
        val app = application as BudgetTreeApplication
        signupButton.isEnabled = false
        lifecycleScope.launch {
            val username = uniqueUsernameFromEmail(app, email)
            val result = app.userDatabaseSystem.createUser(
                username = username,
                password = password,
                email = email,
                phoneNumber = phone,
                displayName = fullName,
                dateOfBirth = dateOfBirth,
                currency = "ZAR"
            )
            signupButton.isEnabled = true
            if (result.wasSuccessful) {
                val createdUser = result.user
                if (createdUser != null) {
                    app.userTreeDatabaseSystem.createUserTree(createdUser, YearMonth.now())
                }
                Log.i(TAG, "Signup succeeded for username '$username', navigating to LoginActivity")
                Toast.makeText(this@SignupActivity, R.string.signup_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                finish()
            } else {
                Log.w(TAG, "Signup failed for username '$username': ${result.errMsg}")
                Toast.makeText(
                    this@SignupActivity,
                    result.errMsg ?: getString(R.string.signup_fields_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun uniqueUsernameFromEmail(app: BudgetTreeApplication, email: String): String {
        val base = email.substringBefore("@").filter { it.isLetterOrDigit() }.ifBlank { "user" }
        if (!app.userDatabaseSystem.doesUserExist(base)) return base
        var suffix = 2
        while (app.userDatabaseSystem.doesUserExist("$base$suffix")) {
            suffix++
        }
        return "$base$suffix"
    }

    private fun parseDateOfBirth(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, dateFormatter)
        } catch (_: Exception) {
            null
        }
    }

    private fun showDatePicker(target: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                target.setText(
                    "%02d / %02d / %04d".format(dayOfMonth, month + 1, year)
                )
            },
            calendar.get(Calendar.YEAR) - 18,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
