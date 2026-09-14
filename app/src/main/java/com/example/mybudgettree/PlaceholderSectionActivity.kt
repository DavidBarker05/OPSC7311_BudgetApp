package com.example.mybudgettree

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

abstract class PlaceholderSectionActivity : AppCompatActivity() {
    abstract val titleRes: Int
    abstract val tab: MainNavigation.Tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_section_placeholder)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sectionRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<TextView>(R.id.tvSectionTitle).setText(titleRes)
        MainNavigation.bind(this, tab)
    }
}

class AnalysisActivity : PlaceholderSectionActivity() {
    override val titleRes: Int = R.string.analytics
    override val tab: MainNavigation.Tab = MainNavigation.Tab.ANALYTICS
}

class TransactionActivity : PlaceholderSectionActivity() {
    override val titleRes: Int = R.string.transactions
    override val tab: MainNavigation.Tab = MainNavigation.Tab.TRANSACTION
}

class CategoriesActivity : PlaceholderSectionActivity() {
    override val titleRes: Int = R.string.categories
    override val tab: MainNavigation.Tab = MainNavigation.Tab.CATEGORIES
}

class ProfileActivity : PlaceholderSectionActivity() {
    override val titleRes: Int = R.string.profile
    override val tab: MainNavigation.Tab = MainNavigation.Tab.PROFILE
}

class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_section_placeholder)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sectionRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<TextView>(R.id.tvSectionTitle).setText(R.string.notifications)
        findViewById<TextView>(R.id.tvSectionMessage).setText(R.string.no_notifications)
        findViewById<android.view.View>(R.id.bottomNav).visibility = android.view.View.GONE
    }
}
