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
    companion object {
        // How long the confirmation stays up before going back to the profile
        private const val SHOW_FOR_MS = 2000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val goToProfile = Runnable { openProfile() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_password_changed)

        findViewById<android.view.View>(R.id.passwordSuccessRoot).setOnClickListener { openProfile() }
        handler.postDelayed(goToProfile, SHOW_FOR_MS)
    }

    // The profile screen is still in the history underneath, so this goes back to it and drops what was opened above it
    private fun openProfile() {
        handler.removeCallbacks(goToProfile)
        startActivity(
            Intent(this, ProfileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(goToProfile)
        super.onDestroy()
    }
}
