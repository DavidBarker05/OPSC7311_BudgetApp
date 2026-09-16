package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
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
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton

class SecurityPinActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_security_pin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.securityPinRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val pinFields = listOf(
            findViewById<EditText>(R.id.etPin1),
            findViewById<EditText>(R.id.etPin2),
            findViewById<EditText>(R.id.etPin3),
            findViewById<EditText>(R.id.etPin4),
            findViewById<EditText>(R.id.etPin5),
            findViewById<EditText>(R.id.etPin6)
        )
        setupPinEntry(pinFields)

        findViewById<MaterialButton>(R.id.btnAccept).setOnClickListener {
            Toast.makeText(this, R.string.password_reset_coming_soon, Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnSendAgain).setOnClickListener {
            Toast.makeText(this, R.string.password_reset_coming_soon, Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvSignupLink).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnFacebook).setOnClickListener { showSocialComingSoon() }
        findViewById<ImageButton>(R.id.btnGoogle).setOnClickListener { showSocialComingSoon() }
    }

    private fun setupPinEntry(fields: List<EditText>) {
        fields.forEachIndexed { index, field ->
            field.doAfterTextChanged { text ->
                if (text?.length == 1 && index < fields.lastIndex) {
                    fields[index + 1].requestFocus()
                }
            }
            field.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    field.text.isNullOrEmpty() &&
                    index > 0
                ) {
                    fields[index - 1].requestFocus()
                    fields[index - 1].text.clear()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun showSocialComingSoon() {
        Toast.makeText(this, R.string.social_coming_soon, Toast.LENGTH_SHORT).show()
    }
}
