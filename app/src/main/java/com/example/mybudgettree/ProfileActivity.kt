package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View>(R.id.rowEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        findViewById<View>(R.id.rowHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        findViewById<View>(R.id.rowLogout).setOnClickListener { logout() }
        MainNavigation.bind(this, MainNavigation.Tab.PROFILE)
    }

    override fun onResume() {
        super.onResume()
        bindProfile()
    }

    private fun bindProfile() {
        val user = UserSession.currentUser ?: return
        findViewById<TextView>(R.id.tvProfileName).text = user.displayName
        findViewById<TextView>(R.id.tvProfileId).text = getString(R.string.profile_id, user.username)
        val photo = findViewById<ShapeableImageView>(R.id.ivProfilePhoto)
        val app = application as BudgetTreeApplication
        lifecycleScope.launch { ProfilePhoto.bind(photo, user.profilePhotoPath, app) }
    }

    private fun logout() {
        UserSession.logout()
        startActivity(
            Intent(this, WelcomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
