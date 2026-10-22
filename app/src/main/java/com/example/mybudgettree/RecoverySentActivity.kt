package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class RecoverySentActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RecoverySentActivity"

        const val EXTRA_EMAIL = "email"

        // How long "Send again" stays disabled after an email was sent, so it can't be used to spam an inbox
        private const val COOLDOWN_SECONDS = 60L
        private const val STATE_COOLDOWN_ENDS_AT = "cooldown_ends_at"
    }

    private lateinit var sendAgainButton: MaterialButton
    private var cooldownJob: Job? = null

    // When the cooldown ends, as epoch milliseconds. It is saved with the screen's state so rotating the phone can't
    // be used to skip the wait
    private var cooldownEndsAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_recovery_sent)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recoverySentRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        sendAgainButton = findViewById(R.id.btnSendAgain)

        findViewById<MaterialButton>(R.id.btnDone).setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
        sendAgainButton.setOnClickListener { sendRecoveryEmail() }
        findViewById<TextView>(R.id.tvSignupLink).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.btnFacebook).setOnClickListener { showSocialComingSoon() }
        findViewById<ImageButton>(R.id.btnGoogle).setOnClickListener { showSocialComingSoon() }

        // An email was just sent from the previous screen, so the wait starts as soon as this screen opens
        cooldownEndsAt = savedInstanceState?.getLong(STATE_COOLDOWN_ENDS_AT)
            ?: (System.currentTimeMillis() + COOLDOWN_SECONDS * 1000)
        startCooldown()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_COOLDOWN_ENDS_AT, cooldownEndsAt)
    }

    /**
     * Keeps "Send again" disabled until [cooldownEndsAt], showing the seconds left on the button. Starting it again
     * replaces a countdown that is already running
     */
    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = lifecycleScope.launch {
            while (true) {
                val secondsLeft = (cooldownEndsAt - System.currentTimeMillis() + 999) / 1000
                if (secondsLeft <= 0) break
                sendAgainButton.isEnabled = false
                sendAgainButton.text = getString(R.string.send_again_in, secondsLeft)
                delay(250.milliseconds)
            }
            sendAgainButton.text = getString(R.string.send_again)
            sendAgainButton.isEnabled = true
        }
    }

    private fun sendRecoveryEmail() {
        val email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()
        val app = application as BudgetTreeApplication
        sendAgainButton.isEnabled = false
        lifecycleScope.launch {
            val result = app.userDatabaseSystem.sendPasswordRecoveryEmail(email)
            if (result.wasSuccessful) {
                Log.i(TAG, "Successfully sent recovery email if account exists")
                cooldownEndsAt = System.currentTimeMillis() + COOLDOWN_SECONDS * 1000
                startCooldown()
            } else {
                Log.w(TAG, "Password recovery failed: ${result.errMsg}")
                Toast.makeText(this@RecoverySentActivity, result.errMsg, Toast.LENGTH_SHORT).show()
                // Nothing was sent, so there is nothing to wait for
                sendAgainButton.isEnabled = true
            }
        }
    }

    private fun showSocialComingSoon() {
        Toast.makeText(this, R.string.social_coming_soon, Toast.LENGTH_SHORT).show()
    }
}
