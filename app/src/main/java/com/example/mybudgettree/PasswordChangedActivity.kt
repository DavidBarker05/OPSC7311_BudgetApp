package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class PasswordChangedActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goToLogin = Runnable { openLogin() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_password_changed)

        findViewById<android.view.View>(R.id.passwordSuccessRoot).setOnClickListener { openLogin() }
        handler.postDelayed(goToLogin, 2500L)
    }

    private fun openLogin() {
        handler.removeCallbacks(goToLogin)
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(goToLogin)
        super.onDestroy()
    }
}
