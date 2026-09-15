package com.example.mybudgettree

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class HelpActivity : AppCompatActivity() {
    private val adapter = HelpAdapter(::toggleFaq, ::openContact)
    private var showingFaq = true
    private var topic = HelpTopic.GENERAL
    private var expandedId: String? = null

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
        setContentView(R.layout.activity_help)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.helpRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHeaderBell).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnFaq).setOnClickListener { showFaqTab() }
        findViewById<MaterialButton>(R.id.btnContact).setOnClickListener { showContactTab() }
        findViewById<MaterialButton>(R.id.btnChipGeneral).setOnClickListener { selectTopic(HelpTopic.GENERAL) }
        findViewById<MaterialButton>(R.id.btnChipAccount).setOnClickListener { selectTopic(HelpTopic.ACCOUNT) }
        findViewById<MaterialButton>(R.id.btnChipServices).setOnClickListener { selectTopic(HelpTopic.SERVICES) }
        findViewById<EditText>(R.id.etHelpSearch).doAfterTextChanged { bindList() }
        findViewById<RecyclerView>(R.id.rvHelp).apply {
            layoutManager = LinearLayoutManager(this@HelpActivity)
            adapter = this@HelpActivity.adapter
        }
        MainNavigation.bind(this, MainNavigation.Tab.PROFILE)
        styleTabs()
        styleChips()
        bindList()
    }

    private fun showFaqTab() {
        showingFaq = true
        styleTabs()
        bindList()
    }

    private fun showContactTab() {
        showingFaq = false
        expandedId = null
        styleTabs()
        bindList()
    }

    private fun selectTopic(selected: HelpTopic) {
        topic = selected
        showingFaq = true
        styleTabs()
        styleChips()
        bindList()
    }

    private fun toggleFaq(entry: FaqEntry) {
        expandedId = if (expandedId == entry.id) null else entry.id
        bindList()
    }

    private fun bindList() {
        val query = findViewById<EditText>(R.id.etHelpSearch).text?.toString()?.trim().orEmpty()
        val items = if (showingFaq) {
            HelpContent.faqs
                .filter { topic == HelpTopic.GENERAL || it.topic == topic }
                .filter { query.isBlank() || getString(it.titleRes).contains(query, ignoreCase = true) }
                .map { HelpListItem.Faq(it, expanded = it.id == expandedId) }
        } else {
            HelpContent.contacts
                .filter { query.isBlank() || getString(it.titleRes).contains(query, ignoreCase = true) }
                .map { HelpListItem.Contact(it) }
        }
        adapter.submit(items)
        findViewById<TextView>(R.id.tvEmptyHelp).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun styleTabs() {
        styleSegment(R.id.btnFaq, showingFaq)
        styleSegment(R.id.btnContact, !showingFaq)
    }

    private fun styleChips() {
        styleChip(R.id.btnChipGeneral, topic == HelpTopic.GENERAL)
        styleChip(R.id.btnChipAccount, topic == HelpTopic.ACCOUNT)
        styleChip(R.id.btnChipServices, topic == HelpTopic.SERVICES)
    }

    private fun styleSegment(buttonId: Int, selected: Boolean) {
        findViewById<MaterialButton>(buttonId).setBackgroundColor(
            getColor(if (selected) R.color.sage_button else R.color.sow_field)
        )
    }

    private fun styleChip(buttonId: Int, selected: Boolean) {
        findViewById<MaterialButton>(buttonId).setBackgroundColor(
            getColor(if (selected) R.color.sow_field else android.R.color.transparent)
        )
    }

    private fun openContact(entry: ContactEntry) {
        val intent = when (entry.kind) {
            ContactKind.EMAIL -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${getString(R.string.help_support_email)}")).apply {
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_support_subject))
            }
            ContactKind.WEBSITE -> viewIntent(getString(R.string.help_website_url))
            ContactKind.FACEBOOK -> viewIntent(getString(R.string.help_facebook_url))
            ContactKind.WHATSAPP -> viewIntent(getString(R.string.help_whatsapp_url))
            ContactKind.INSTAGRAM -> viewIntent(getString(R.string.help_instagram_url))
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.help_link_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun viewIntent(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))
}
