package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton

class ForgotPasswordActivity : AppCompatActivity() {
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
        findViewById<MaterialButton>(R.id.btnNextStep).setOnClickListener {
            val email = emailField.text?.toString()?.trim().orEmpty()
            when {
                email.isBlank() -> Toast.makeText(this, R.string.email_required, Toast.LENGTH_SHORT).show()
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Toast.makeText(this, R.string.email_invalid, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val pin = PasswordResetSession.start(email)
                    AlertDialog.Builder(this)
                        .setTitle(R.string.pin_dialog_title)
                        .setMessage(getString(R.string.pin_dialog_message, pin))
                        .setPositiveButton(R.string.ok) { _, _ ->
                            startActivity(Intent(this, SecurityPinActivity::class.java))
                        }
                        .show()
                }
            }
        }
        findViewById<MaterialButton>(R.id.btnSignup).setOnClickListener { openSignup() }
        findViewById<TextView>(R.id.tvSignupLink).setOnClickListener { openSignup() }
        findViewById<ImageButton>(R.id.btnFacebook).setOnClickListener { showSocialComingSoon() }
        findViewById<ImageButton>(R.id.btnGoogle).setOnClickListener { showSocialComingSoon() }
    }

    private fun openSignup() {
        startActivity(Intent(this, SignupActivity::class.java))
    }

    private fun showSocialComingSoon() {
        Toast.makeText(this, R.string.social_coming_soon, Toast.LENGTH_SHORT).show()
    }
}
