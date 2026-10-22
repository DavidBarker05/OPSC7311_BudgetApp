package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class NewPasswordActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "NewPasswordActivity"

        // Firebase's default rule for a new password
        private const val MIN_PASSWORD_LENGTH = 6
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
        setContentView(R.layout.activity_new_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newPasswordRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener { changePassword() }
    }

    private fun changePassword() {
        val user = UserSession.currentUser ?: return
        val current = findViewById<TextInputEditText>(R.id.etCurrentPassword).text?.toString().orEmpty()
        val new = findViewById<TextInputEditText>(R.id.etNewPassword).text?.toString().orEmpty()
        val confirm = findViewById<TextInputEditText>(R.id.etConfirmPassword).text?.toString().orEmpty()
        val currentLayout = findViewById<TextInputLayout>(R.id.tilCurrentPassword)
        val newLayout = findViewById<TextInputLayout>(R.id.tilNewPassword)
        val confirmLayout = findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        currentLayout.error = if (current.isBlank()) getString(R.string.password_error_current_empty) else null
        newLayout.error = when {
            new.length < MIN_PASSWORD_LENGTH -> getString(R.string.password_error_too_short, MIN_PASSWORD_LENGTH)
            new == current -> getString(R.string.password_error_same)
            else -> null
        }
        confirmLayout.error = if (confirm != new) getString(R.string.password_error_mismatch) else null
        if (currentLayout.error != null || newLayout.error != null || confirmLayout.error != null) return

        val button = findViewById<MaterialButton>(R.id.btnChangePassword)
        val app = application as BudgetTreeApplication
        button.isEnabled = false
        lifecycleScope.launch {
            val result = app.userDatabaseSystem.updatePassword(user, new, current)
            if (result.status == UpdateReturnStatus.Succeeded) {
                Log.i(TAG, "Password changed")
                startActivity(Intent(this@NewPasswordActivity, PasswordChangedActivity::class.java))
                finish()
            } else {
                Log.w(TAG, "Failed to change the password: ${result.errMsg}")
                button.isEnabled = true
                // A wrong current password belongs under that field, anything else is shown as a message
                if (result.errMsg?.contains("current password", ignoreCase = true) == true) {
                    currentLayout.error = getString(R.string.password_error_current_wrong)
                } else {
                    Toast.makeText(this@NewPasswordActivity, result.errMsg ?: getString(R.string.password_error_generic), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
