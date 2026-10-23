package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import com.example.mybudgettree.WateringCanActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WateringCanActivityUiTest : UiTestBase() {

    @Test
    fun freshUser_showsGoalGridAndSaveButton() {
        UserSession.login(createTestUser(username = "wateringuser"))

        ActivityScenario.launch(WateringCanActivity::class.java).use {
            // the tree is the first thing on the tab; the goals and save button are further down the page
            eventually { onView(withId(R.id.moneyTreeView)).check(matches(isDisplayed())) }
            onView(withId(R.id.rvGoals)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.btnSaveMore)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingAddGoalTile_opensNewGoalDialog() {
        UserSession.login(createTestUser(username = "wateringuser2"))

        ActivityScenario.launch(WateringCanActivity::class.java).use {
            // the "+" add-goal tile is below the tree, so it can start off screen, and it only appears once the goals
            // have loaded. Scrolling to it is retried until it is there, rather than waiting for it to already be visible
            eventually { onView(withText(R.string.more)).perform(scrollTo()) }
            onView(withText(R.string.more)).perform(click())

            eventually { onView(withId(R.id.etNewGoalName)).check(matches(isDisplayed())) }
        }
    }
}
