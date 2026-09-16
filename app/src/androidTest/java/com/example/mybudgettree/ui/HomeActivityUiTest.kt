package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.HomeActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeActivityUiTest : UiTestBase() {

    @Test
    fun freshUser_showsZeroedOutBalancesAndWelcomeHeader() {
        UserSession.login(createTestUser(username = "homeuser"))

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.tvWelcomeSanctuary)).check(matches(isDisplayed()))
            onView(withId(R.id.tvTotalBalance)).check(matches(withText("R0.00")))
            onView(withId(R.id.tvTotalExpense)).check(matches(withText("-R0.00")))
            onView(withId(R.id.tvEmptyTransactions)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun homeTab_isMarkedSelectedInBottomNav() {
        UserSession.login(createTestUser(username = "homeuser2"))

        ActivityScenario.launch(HomeActivity::class.java).use {
            onView(withId(R.id.navHome)).check(matches(isSelected()))
        }
    }
}
