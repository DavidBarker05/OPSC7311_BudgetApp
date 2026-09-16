package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.CategoriesActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriesActivityUiTest : UiTestBase() {

    @Test
    fun freshUser_seedsDefaultCategories_withoutSavings() {
        UserSession.login(createTestUser(username = "categoryuser"))

        ActivityScenario.launch(CategoriesActivity::class.java).use {
            waitForText("Food") // categories are seeded asynchronously on first load
            onView(withText("Transport")).check(matches(isDisplayed()))
            onView(withText("Rent")).check(matches(isDisplayed()))
            // Savings is deliberately excluded from the seeded defaults — it's tracked via
            // the Watering Can goals instead, see CategoryGardenTest
            onView(withText("Savings")).check(doesNotExist())
        }
    }

    @Test
    fun tappingACategoryTile_opensCategoryDetail() {
        UserSession.login(createTestUser(username = "categoryuser2"))

        ActivityScenario.launch(CategoriesActivity::class.java).use {
            waitForText("Food") // categories are seeded asynchronously on first load
            onView(withText("Food")).perform(click())

            onView(withId(R.id.tvCategoryTitle)).check(matches(withText("Food")))
        }
    }
}
