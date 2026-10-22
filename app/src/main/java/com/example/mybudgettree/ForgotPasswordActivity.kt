package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ForgotPasswordActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_forgot_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPasswordRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val emailField = findViewById<EditText>(R.id.etEmail)
        val nextStepButton = findViewById<MaterialButton>(R.id.btnNextStep)

        nextStepButton.setOnClickListener {
            val email = emailField.text?.toString()?.trim().orEmpty()
            if (email.isBlank()) {
                Toast.makeText(this, R.string.email_required, Toast.LENGTH_SHORT).show()
            }
            else {
                sendRecoveryEmail(nextStepButton, email)
            }
        }
        findViewById<MaterialButton>(R.id.btnSignup).setOnClickListener { openSignup() }
        findViewById<TextView>(R.id.tvSignupLink).setOnClickListener { openSignup() }
        findViewById<ImageButton>(R.id.btnFacebook).setOnClickListener { showSocialComingSoon() }
        findViewById<ImageButton>(R.id.btnGoogle).setOnClickListener { showSocialComingSoon() }
    }

    private fun sendRecoveryEmail(nextStepButton: MaterialButton, email: String) {
        val app = application as BudgetTreeApplication
        nextStepButton.isEnabled = false
        lifecycleScope.launch {
            val result = app.userDatabaseSystem.sendPasswordRecoveryEmail(email)
            nextStepButton.isEnabled = true
            if (result.wasSuccessful) {
                Log.i(TAG, "Successfully sent recovery email if account exists")
                startActivity(
                    Intent(this@ForgotPasswordActivity, RecoverySentActivity::class.java)
                        .putExtra(RecoverySentActivity.EXTRA_EMAIL, email)
                )
                finish()
            } else {
                Log.w(TAG, "Password recovery failed: ${result.errMsg}")
                Toast.makeText(this@ForgotPasswordActivity, result.errMsg ?: getString(R.string.recovery_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSignup() {
        startActivity(Intent(this, SignupActivity::class.java))
        finish()
    }

    private fun showSocialComingSoon() {
        Toast.makeText(this, R.string.social_coming_soon, Toast.LENGTH_SHORT).show()
    }
}
