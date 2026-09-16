package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.ProfileActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActivityUiTest : UiTestBase() {

    @Test
    fun showsLoggedInUsersNameAndId() {
        val user = createTestUser(username = "profileuser")
        UserSession.login(user)

        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.tvProfileName)).check(matches(withText(user.displayName)))
            onView(withId(R.id.tvProfileId)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingEditProfile_opensEditProfileScreen() {
        UserSession.login(createTestUser(username = "profileuser2"))

        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.rowEditProfile)).perform(click())

            onView(withId(R.id.etDisplayName)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingLogout_returnsToWelcomeAndClearsSession() {
        UserSession.login(createTestUser(username = "profileuser3"))

        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.rowLogout)).perform(click())

            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
        }

        assertFalse(UserSession.isLoggedIn())
    }
}
