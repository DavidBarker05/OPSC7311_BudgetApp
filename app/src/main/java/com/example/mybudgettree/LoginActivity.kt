package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val usernameOrEmail = findViewById<EditText>(R.id.etUsernameOrEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<MaterialButton>(R.id.btnLogin)

        loginButton.setOnClickListener {
            val identifier = usernameOrEmail.text?.toString()?.trim().orEmpty()
            val passwordValue = password.text?.toString().orEmpty()
            if (identifier.isBlank() || passwordValue.isBlank()) {
                Toast.makeText(this, R.string.login_fields_required, Toast.LENGTH_SHORT).show()
            } else {
                signIn(loginButton, identifier, passwordValue)
            }
        }
        findViewById<MaterialButton>(R.id.btnSignup).setOnClickListener { openSignup() }
        findViewById<TextView>(R.id.tvSignupLink).setOnClickListener { openSignup() }
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnFacebook).setOnClickListener { showSocialComingSoon() }
        findViewById<ImageButton>(R.id.btnGoogle).setOnClickListener { showSocialComingSoon() }
    }

    private fun signIn(loginButton: MaterialButton, identifier: String, password: String) {
        val app = application as BudgetTreeApplication
        loginButton.isEnabled = false
        lifecycleScope.launch {
            val result = app.userDatabaseSystem.login(identifier, password)
            loginButton.isEnabled = true
            val user = result.user
            if (result.wasSuccessful && user != null) {
                UserSession.login(user)
                startActivity(
                    Intent(this@LoginActivity, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            } else {
                Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_SHORT).show()
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
