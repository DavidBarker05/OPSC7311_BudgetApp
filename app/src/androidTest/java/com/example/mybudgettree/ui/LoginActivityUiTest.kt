package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.LoginActivity
import com.example.mybudgettree.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real [LoginActivity] UI with Espresso against an isolated in-memory database
 * (see [UiTestBase]) instead of the on-device one
 */
@RunWith(AndroidJUnit4::class)
class LoginActivityUiTest : UiTestBase() {

    @Test
    fun validCredentials_navigatesToHome() {
        createTestUser(username = "loginuser", password = "correctPass1")

        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etUsernameOrEmail)).perform(typeText("loginuser"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText("correctPass1"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            onView(withId(R.id.tvWelcomeSanctuary)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun wrongPassword_staysOnLoginScreen() {
        createTestUser(username = "loginuser2", password = "correctPass1")

        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etUsernameOrEmail)).perform(typeText("loginuser2"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText("wrongPassword"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            // no navigation should have happened — the login button is still on screen
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canLogInWithEmailInsteadOfUsername() {
        createTestUser(username = "loginuser3", password = "correctPass1")

        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etUsernameOrEmail)).perform(typeText("loginuser3@example.com"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(typeText("correctPass1"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(click())

            onView(withId(R.id.tvWelcomeSanctuary)).check(matches(isDisplayed()))
        }
    }
}
