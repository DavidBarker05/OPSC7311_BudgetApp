package com.example.mybudgettree

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import android.widget.Toast

object MainNavigation {
    enum class Tab { HOME, ANALYTICS, TRANSACTION, CATEGORIES, PROFILE }

    fun bind(activity: Activity, selected: Tab) {
        activity.findViewById<ImageButton>(R.id.navHome).apply {
            isSelected = selected == Tab.HOME
            setOnClickListener { open(activity, HomeActivity::class.java, selected == Tab.HOME) }
        }
        activity.findViewById<ImageButton>(R.id.navAnalysis).apply {
            isSelected = selected == Tab.ANALYTICS
            setOnClickListener { open(activity, AnalysisActivity::class.java, selected == Tab.ANALYTICS) }
        }
        activity.findViewById<ImageButton>(R.id.navTransactions).apply {
            isSelected = selected == Tab.TRANSACTION
            setOnClickListener { open(activity, TransactionActivity::class.java, selected == Tab.TRANSACTION) }
        }
        activity.findViewById<ImageButton>(R.id.navCategories).apply {
            isSelected = selected == Tab.CATEGORIES
            setOnClickListener { open(activity, CategoriesActivity::class.java, selected == Tab.CATEGORIES) }
        }
        // TODO: Open the Goals screen when that UI is implemented.
        activity.findViewById<ImageButton>(R.id.navGoals).setOnClickListener {
            Toast.makeText(activity, R.string.goals_coming_soon, Toast.LENGTH_SHORT).show()
        }
        activity.findViewById<ImageButton>(R.id.navProfile).apply {
            isSelected = selected == Tab.PROFILE
            setOnClickListener { open(activity, ProfileActivity::class.java, selected == Tab.PROFILE) }
        }
    }

    private fun open(activity: Activity, destination: Class<*>, alreadyThere: Boolean) {
        if (alreadyThere) return
        val intent = Intent(activity, destination)
        if (destination == HomeActivity::class.java) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(intent)
        if (activity !is HomeActivity) {
            activity.finish()
        }
    }
}
