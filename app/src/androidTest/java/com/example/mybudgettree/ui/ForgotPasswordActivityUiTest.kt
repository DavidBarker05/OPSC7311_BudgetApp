package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.FirebaseEmulator
import com.example.mybudgettree.ForgotPasswordActivity
import com.example.mybudgettree.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real [ForgotPasswordActivity] with Espresso against the Firebase emulators. The recovery logic itself is
 * tested with the database tests, so these only check that the screen asks for it and moves on (or stays) correctly
 */
@RunWith(AndroidJUnit4::class)
class ForgotPasswordActivityUiTest : UiTestBase() {

    @Test
    fun validEmail_asksForARecoveryEmail_andOpensTheEmailSentScreen() {
        val user = createTestUser(username = "forgotuser")
        app.userDatabaseSystem.logout()

        ActivityScenario.launch(ForgotPasswordActivity::class.java).use {
            onView(withId(R.id.etEmail)).perform(typeText(user.email), closeSoftKeyboard())
            onView(withId(R.id.btnNextStep)).perform(click())

            // the request is made asynchronously, and the next screen only opens once it has gone through
            eventually { onView(withId(R.id.btnDone)).check(matches(isDisplayed())) }
        }

        assertEquals(1, FirebaseEmulator.recordedCodes(user.email).size)
    }

    @Test
    fun blankEmail_staysOnTheScreen_andAsksForNothing() {
        ActivityScenario.launch(ForgotPasswordActivity::class.java).use {
            onView(withId(R.id.btnNextStep)).perform(click())

            onView(withId(R.id.btnNextStep)).check(matches(isDisplayed()))
            onView(withId(R.id.btnDone)).check(doesNotExist())
        }
    }

    @Test
    fun invalidEmail_staysOnTheScreen_andCanTryAgain() {
        ActivityScenario.launch(ForgotPasswordActivity::class.java).use {
            onView(withId(R.id.etEmail)).perform(typeText("not-an-email"), closeSoftKeyboard())
            onView(withId(R.id.btnNextStep)).perform(click())

            // the button is switched off while the request runs, so it coming back means the request has finished
            eventually { onView(withId(R.id.btnNextStep)).check(matches(isEnabled())) }
            onView(withId(R.id.btnNextStep)).check(matches(isDisplayed()))
            onView(withId(R.id.btnDone)).check(doesNotExist())
        }
        assertTrue(FirebaseEmulator.recordedCodes("not-an-email").isEmpty())
    }
}
