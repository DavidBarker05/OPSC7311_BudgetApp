package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {
    private val splashHandler = Handler(Looper.getMainLooper())
    private val openWelcome = Runnable {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_main)
        splashHandler.postDelayed(openWelcome, 2000L)
    }

    override fun onDestroy() {
        splashHandler.removeCallbacks(openWelcome)
        super.onDestroy()
    }
}
