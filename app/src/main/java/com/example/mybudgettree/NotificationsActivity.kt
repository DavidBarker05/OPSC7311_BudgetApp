package com.example.mybudgettree

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {
    private val adapter = NotificationAdapter(::showNotificationDetail)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UserSession.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_notifications)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notificationsRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.rvNotifications).apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = this@NotificationsActivity.adapter
        }

        MainNavigation.bind(this)
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        val user = UserSession.currentUser ?: return
        val app = application as BudgetTreeApplication
        lifecycleScope.launch {
            val categories = app.categoryDatabaseSystem.getAllCategoriesForUser(user).categories.orEmpty()
            val expenses = app.expenseDatabaseSystem.retrieveAllExpenses(user).expenses.orEmpty()
            val incomes = app.incomeDatabaseSystem.retrieveAllIncomes(user).incomes.orEmpty()
            val rows = NotificationFeed.build(this@NotificationsActivity, categories, expenses, incomes)
            adapter.submit(rows)
            findViewById<TextView>(R.id.tvEmptyNotifications).visibility =
                if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showNotificationDetail(notification: WalletNotification) {
        val message = buildString {
            append(notification.body)
            if (!notification.highlight.isNullOrBlank()) {
                append("\n\n")
                append(notification.highlight)
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
        if (notification.type == WalletNotification.Type.TRANSACTION) {
            dialog.setNeutralButton(R.string.view_transactions) { _, _ ->
                startActivity(Intent(this, TransactionActivity::class.java))
            }
        }
        dialog.show()
    }
}
