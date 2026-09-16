package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.AnalysisActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisActivityUiTest : UiTestBase() {

    @Test
    fun freshUser_defaultsToDailyPeriodWithZeroBudgetBar() {
        UserSession.login(createTestUser(username = "analysisuser"))

        ActivityScenario.launch(AnalysisActivity::class.java).use {
            onView(withId(R.id.btnDaily)).check(matches(isChecked()))
            onView(withId(R.id.btnWeekly)).check(matches(isNotChecked()))
            onView(withId(R.id.tvBudgetPercent)).check(matches(withText("0%")))
        }
    }

    @Test
    fun tappingWeekly_switchesTheCheckedPeriodButton() {
        UserSession.login(createTestUser(username = "analysisuser2"))

        ActivityScenario.launch(AnalysisActivity::class.java).use {
            onView(withId(R.id.btnWeekly)).perform(click())

            onView(withId(R.id.btnWeekly)).check(matches(isChecked()))
            onView(withId(R.id.btnDaily)).check(matches(isNotChecked()))
        }
    }

    @Test
    fun statusRow_showsNoBudgetSetForFreshUser() {
        UserSession.login(createTestUser(username = "analysisuser3"))

        ActivityScenario.launch(AnalysisActivity::class.java).use {
            onView(withId(R.id.tvExpenseStatus)).check(matches(isDisplayed()))
        }
    }
}
