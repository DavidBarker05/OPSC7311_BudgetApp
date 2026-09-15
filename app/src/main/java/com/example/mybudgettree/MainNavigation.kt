package com.example.mybudgettree

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton

object MainNavigation {
    enum class Tab { HOME, ANALYTICS, TRANSACTION, CATEGORIES, GOALS, PROFILE }

    fun bind(activity: Activity, selected: Tab? = null) {
        activity.findViewById<ImageButton>(R.id.navHome).apply {
            isSelected = selected == Tab.HOME || activity is QuicklyAnalysisActivity
            setOnClickListener {
                if (activity is QuicklyAnalysisActivity) {
                    activity.finish()
                    return@setOnClickListener
                }
                open(activity, HomeActivity::class.java, selected == Tab.HOME)
            }
        }
        activity.findViewById<ImageButton>(R.id.navAnalysis).apply {
            isSelected = selected == Tab.ANALYTICS || activity is SearchActivity
            setOnClickListener {
                if (activity is SearchActivity) {
                    activity.finish()
                    return@setOnClickListener
                }
                open(activity, AnalysisActivity::class.java, selected == Tab.ANALYTICS)
            }
        }
        activity.findViewById<ImageButton>(R.id.navTransactions).apply {
            isSelected = selected == Tab.TRANSACTION
            setOnClickListener { open(activity, TransactionActivity::class.java, selected == Tab.TRANSACTION) }
        }
        activity.findViewById<ImageButton>(R.id.navCategories).apply {
            isSelected = selected == Tab.CATEGORIES
            setOnClickListener {
                if (activity is CategoryDetailActivity || activity is SowExpensesActivity) {
                    open(activity, CategoriesActivity::class.java, alreadyThere = false)
                    return@setOnClickListener
                }
                open(activity, CategoriesActivity::class.java, selected == Tab.CATEGORIES)
            }
        }
        activity.findViewById<ImageButton>(R.id.navGoals).apply {
            isSelected = selected == Tab.GOALS
            setOnClickListener {
                if (activity is GoalDetailActivity || activity is FillWateringCanActivity) {
                    open(activity, WateringCanActivity::class.java, alreadyThere = false)
                    return@setOnClickListener
                }
                open(activity, WateringCanActivity::class.java, selected == Tab.GOALS)
            }
        }
        activity.findViewById<ImageButton>(R.id.navProfile).apply {
            isSelected = selected == Tab.PROFILE
            setOnClickListener {
                if (activity is EditProfileActivity || activity is HelpActivity) {
                    open(activity, ProfileActivity::class.java, alreadyThere = false)
                    return@setOnClickListener
                }
                open(activity, ProfileActivity::class.java, selected == Tab.PROFILE)
            }
        }
    }

    private fun open(activity: Activity, destination: Class<*>, alreadyThere: Boolean) {
        if (alreadyThere) return
        val intent = Intent(activity, destination)
        if (destination == HomeActivity::class.java ||
            destination == CategoriesActivity::class.java ||
            destination == WateringCanActivity::class.java ||
            destination == ProfileActivity::class.java
        ) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(intent)
        if (activity !is HomeActivity) {
            activity.finish()
        }
    }
}
