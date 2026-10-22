package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class UpdateEmailActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "UpdateEmailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = UserSession.currentUser
        if (user == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_update_email)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.updateEmailRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvCurrentEmail).text = user.email
        findViewById<MaterialButton>(R.id.btnUpdateEmail).setOnClickListener { updateEmail() }
    }

    private fun updateEmail() {
        val user = UserSession.currentUser ?: return
        val newEmail = findViewById<EditText>(R.id.etNewEmail).text?.toString()?.trim().orEmpty()
        val password = findViewById<EditText>(R.id.etCurrentPassword).text?.toString().orEmpty()
        if (newEmail.isBlank()) return showError(R.string.update_email_error_empty)
        if (password.isBlank()) return showError(R.string.update_email_error_password_empty)
        clearError()

        val button = findViewById<MaterialButton>(R.id.btnUpdateEmail)
        val app = application as BudgetTreeApplication
        button.isEnabled = false
        lifecycleScope.launch {
            val result = app.userDatabaseSystem.updateEmail(user, newEmail, password)
            button.isEnabled = true
            when (result.status) {
                UpdateReturnStatus.PendingVerification -> {
                    Log.i(TAG, "Verification email sent for the email change")
                    showSentDialog(user.email, newEmail)
                }
                UpdateReturnStatus.NoChange -> showError(R.string.update_email_error_same)
                UpdateReturnStatus.Failed -> {
                    Log.w(TAG, "Failed to start the email change: ${result.errMsg}")
                    showError(result.errMsg ?: getString(R.string.update_email_error_generic))
                }
                UpdateReturnStatus.Succeeded -> finish()
            }
        }
    }

    // The email has not changed yet, so the dialog says what happens next and sends the user back when it is closed
    private fun showSentDialog(currentEmail: String, newEmail: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.update_email_sent_title)
            .setMessage(getString(R.string.update_email_sent_message, newEmail, currentEmail))
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    private fun showError(message: Int) = showError(getString(message))

    private fun clearError() = showError(null as String?)

    private fun showError(message: String?) {
        findViewById<TextView>(R.id.tvUpdateEmailError).apply {
            visibility = if (message != null) View.VISIBLE else View.GONE
            text = message
        }
    }
}
