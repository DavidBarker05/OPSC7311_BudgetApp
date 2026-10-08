package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import android.widget.EditText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.EditProfileActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real [EditProfileActivity] delete-account confirmation dialog with Espresso,
 * against an isolated in-memory database (see [UiTestBase])
 */
@RunWith(AndroidJUnit4::class)
class EditProfileDeleteAccountUiTest : UiTestBase() {

    @Test
    fun deleteAccount_confirmed_removesUserAndReturnsToWelcome() {
        val user = createTestUser(username = "deleteme")
        UserSession.login(user)

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.btnDeleteAccount)).perform(scrollTo(), click())
            onView(withText(R.string.delete)).perform(click())
            // deleting asks for the password again
            onView(isAssignableFrom(EditText::class.java)).perform(typeText("password123"), closeSoftKeyboard())
            onView(withText(R.string.delete)).perform(click())

            // the user is deleted asynchronously before the Welcome screen is launched
            eventually { onView(withId(R.id.btnLogin)).check(matches(isDisplayed())) }
        }

        val canStillLogIn = runBlocking { app.userDatabaseSystem.login("deleteme@example.com", "password123").wasSuccessful }
        assertFalse(canStillLogIn)
        assertFalse(UserSession.isLoggedIn())
    }

    @Test
    fun deleteAccount_cancelled_keepsUserAndStaysOnProfile() {
        val user = createTestUser(username = "keepme")
        UserSession.login(user)

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.btnDeleteAccount)).perform(scrollTo(), click())
            onView(withText(R.string.cancel)).perform(click())

            onView(withId(R.id.btnDeleteAccount)).check(matches(isDisplayed()))
        }

        val stillExists = runBlocking { app.userDatabaseSystem.getCurrentUser() != null }
        assertTrue(stillExists)
    }
}
