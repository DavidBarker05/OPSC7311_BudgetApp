package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

class NewPasswordActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NewPasswordActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_new_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.newPasswordRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val newPassword = findViewById<TextInputEditText>(R.id.etNewPassword)
        val confirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)

        findViewById<MaterialButton>(R.id.btnChangePassword).setOnClickListener {
            val password = newPassword.text?.toString().orEmpty()
            val confirm = confirmPassword.text?.toString().orEmpty()
            when {
                password.isBlank() || confirm.isBlank() -> {
                    Toast.makeText(this, R.string.password_required, Toast.LENGTH_SHORT).show()
                }
                password != confirm -> {
                    Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show()
                }
                else -> savePasswordAndContinue(password)
            }
        }
    }

    private fun savePasswordAndContinue(password: String) {
        val email = PasswordResetSession.email
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            if (!email.isNullOrBlank()) {
                val found = app.userDatabaseSystem.findUserByEmail(email)
                val user = found.user
                if (found.wasSuccessful && user != null) {
                    app.userDatabaseSystem.updatePassword(user, password)
                } else {
                    Log.w(TAG, "Password reset requested for unknown email")
                }
            } else {
                Log.w(TAG, "Password reset attempted with no email in session")
            }
            startActivity(Intent(this@NewPasswordActivity, PasswordChangedActivity::class.java))
            finish()
        }
    }
}
