package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.HomeActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the bottom navigation bar all the way around, starting and ending on Home, to catch
 * broken wiring between the six main screens (see [MainNavigation]).
 */
@RunWith(AndroidJUnit4::class)
class MainNavigationUiTest : UiTestBase() {

    @Test
    fun bottomNav_visitsEveryMainScreenAndReturnsHome() {
        UserSession.login(createTestUser(username = "navuser"))

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.tvWelcomeSanctuary)).check(matches(isDisplayed()))

            onView(withId(R.id.navAnalysis)).perform(click())
            onView(withId(R.id.tvBudgetPercent)).check(matches(isDisplayed()))

            onView(withId(R.id.navTransactions)).perform(click())
            onView(withId(R.id.tvEmptyTransactions)).check(matches(isDisplayed()))

            onView(withId(R.id.navCategories)).perform(click())
            onView(withId(R.id.rvCategories)).check(matches(isDisplayed()))

            onView(withId(R.id.navGoals)).perform(click())
            onView(withId(R.id.rvGoals)).check(matches(isDisplayed()))

            onView(withId(R.id.navProfile)).perform(click())
            onView(withId(R.id.tvProfileName)).check(matches(isDisplayed()))

            onView(withId(R.id.navHome)).perform(click())
            onView(withId(R.id.tvWelcomeSanctuary)).check(matches(isDisplayed()))
        }
    }
}
