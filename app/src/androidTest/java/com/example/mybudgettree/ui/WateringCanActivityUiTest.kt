package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
            onView(withId(R.id.rvGoals)).check(matches(isDisplayed()))
            onView(withId(R.id.btnSaveMore)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingAddGoalTile_opensNewGoalDialog() {
        UserSession.login(createTestUser(username = "wateringuser2"))

        ActivityScenario.launch(WateringCanActivity::class.java).use {
            // the "+" add-goal tile is the only item seeded for a fresh user with no goals yet
            onView(withText(R.string.more)).perform(click())

            onView(withId(R.id.etNewGoalName)).check(matches(isDisplayed()))
        }
    }
}
