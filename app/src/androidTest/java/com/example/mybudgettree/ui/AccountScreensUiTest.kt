package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.EditProfileActivity
import com.example.mybudgettree.FirebaseEmulator
import com.example.mybudgettree.NewPasswordActivity
import com.example.mybudgettree.PasswordChangedActivity
import com.example.mybudgettree.ProfileActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UpdateEmailActivity
import com.example.mybudgettree.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the account screens reached from edit profile (update email and change password) with Espresso against the
 * Firebase emulators. The rules behind them are tested with the database tests, so these check what the screens show
 * and where they go
 */
@RunWith(AndroidJUnit4::class)
class AccountScreensUiTest : UiTestBase() {

    private fun type(id: Int, text: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard())
    }

    // ---- edit profile

    @Test
    fun editProfile_hasButtonsForUpdatingTheEmailAndChangingThePassword_andNoEmailField() {
        UserSession.login(createTestUser(username = "accountbuttons"))

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.btnOpenUpdateEmail)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.btnOpenChangePassword)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.etEmail)).check(doesNotExist())
        }
    }

    @Test
    fun editProfile_updateEmailButton_opensTheUpdateEmailScreen() {
        UserSession.login(createTestUser(username = "accountopenemail"))

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.btnOpenUpdateEmail)).perform(scrollTo(), click())

            eventually { onView(withId(R.id.etNewEmail)).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun editProfile_changePasswordButton_opensTheChangePasswordScreen() {
        UserSession.login(createTestUser(username = "accountopenpassword"))

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.btnOpenChangePassword)).perform(scrollTo(), click())

            eventually { onView(withId(R.id.etCurrentPassword)).check(matches(isDisplayed())) }
        }
    }

    // ---- currency

    @Test
    fun editProfile_showsTheCurrentCurrency() {
        UserSession.login(createTestUser(username = "currencyshown"))

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.tvCurrency)).perform(scrollTo()).check(matches(withText("ZAR (R)")))
        }
    }

    @Test
    fun editProfile_changingTheCurrency_warnsThatNothingIsConverted_thenSavesIt() {
        val user = createTestUser(username = "currencychange")
        UserSession.login(user)

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.rowCurrency)).perform(scrollTo(), click())
            eventually { onView(withText("USD ($)")).inRoot(isDialog()).perform(click()) }
            eventually { onView(withText("Change currency to USD?")).inRoot(isDialog()).check(matches(isDisplayed())) }
            onView(withText(R.string.currency_change)).inRoot(isDialog()).perform(click())

            eventually { onView(withId(R.id.tvCurrency)).perform(scrollTo()).check(matches(withText("USD ($)"))) }
        }

        eventually { assertEquals("USD", runBlocking { app.userDatabaseSystem.getCurrentUser()!!.currency }) }
    }

    @Test
    fun editProfile_cancellingTheWarning_keepsTheCurrency() {
        UserSession.login(createTestUser(username = "currencycancel"))

        ActivityScenario.launch(EditProfileActivity::class.java).use {
            onView(withId(R.id.rowCurrency)).perform(scrollTo(), click())
            eventually { onView(withText("EUR (€)")).inRoot(isDialog()).perform(click()) }
            eventually { onView(withText(R.string.cancel)).inRoot(isDialog()).perform(click()) }

            onView(withId(R.id.tvCurrency)).perform(scrollTo()).check(matches(withText("ZAR (R)")))
        }
    }

    // ---- update email

    @Test
    fun updateEmail_showsTheCurrentEmail() {
        val user = createTestUser(username = "accountshowemail")
        UserSession.login(user)

        ActivityScenario.launch(UpdateEmailActivity::class.java).use {
            onView(withId(R.id.tvCurrentEmail)).check(matches(withText(user.email)))
        }
    }

    @Test
    fun updateEmail_withTheRightPassword_tellsTheUserWhereTheEmailWasSent() {
        UserSession.login(createTestUser(username = "accountsendemail"))

        ActivityScenario.launch(UpdateEmailActivity::class.java).use {
            type(R.id.etNewEmail, FirebaseEmulator.uniqueEmail("new"))
            type(R.id.etCurrentPassword, "password123")
            onView(withId(R.id.btnUpdateEmail)).perform(scrollTo(), click())

            eventually { onView(withText(R.string.update_email_sent_title)).inRoot(isDialog()).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun updateEmail_withAWrongPassword_showsAnErrorAndSendsNothing() {
        UserSession.login(createTestUser(username = "accountwrongpw"))
        val newEmail = FirebaseEmulator.uniqueEmail("new")

        ActivityScenario.launch(UpdateEmailActivity::class.java).use {
            type(R.id.etNewEmail, newEmail)
            type(R.id.etCurrentPassword, "not-the-password")
            onView(withId(R.id.btnUpdateEmail)).perform(scrollTo(), click())

            eventually { onView(withId(R.id.tvUpdateEmailError)).perform(scrollTo()).check(matches(isDisplayed())) }
            onView(withText(R.string.update_email_sent_title)).check(doesNotExist())
        }
    }

    @Test
    fun updateEmail_leftBlank_showsAnErrorStraightAway() {
        UserSession.login(createTestUser(username = "accountblankemail"))

        ActivityScenario.launch(UpdateEmailActivity::class.java).use {
            onView(withId(R.id.btnUpdateEmail)).perform(scrollTo(), click())

            onView(withText(R.string.update_email_error_empty)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    // ---- change password

    @Test
    fun changePassword_whenTheTwoNewPasswordsDiffer_showsAnErrorAndStays() {
        UserSession.login(createTestUser(username = "accountmismatch"))

        ActivityScenario.launch(NewPasswordActivity::class.java).use {
            type(R.id.etCurrentPassword, "password123")
            type(R.id.etNewPassword, "brandnewpass1")
            type(R.id.etConfirmPassword, "somethingelse1")
            onView(withId(R.id.btnChangePassword)).perform(scrollTo(), click())

            onView(withText(R.string.password_error_mismatch)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun changePassword_withAWrongCurrentPassword_saysSoUnderThatField() {
        UserSession.login(createTestUser(username = "accountwrongcurrent"))

        ActivityScenario.launch(NewPasswordActivity::class.java).use {
            type(R.id.etCurrentPassword, "not-the-password")
            type(R.id.etNewPassword, "brandnewpass1")
            type(R.id.etConfirmPassword, "brandnewpass1")
            onView(withId(R.id.btnChangePassword)).perform(scrollTo(), click())

            eventually { onView(withText(R.string.password_error_current_wrong)).perform(scrollTo()).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun changePassword_success_endsBackOnTheProfile_andTheNewPasswordWorks() {
        val user = createTestUser(username = "accountchange")
        UserSession.login(user)

        ActivityScenario.launch(ProfileActivity::class.java).use {
            onView(withId(R.id.rowEditProfile)).perform(click())
            eventually { onView(withId(R.id.btnOpenChangePassword)).perform(scrollTo(), click()) }
            eventually { onView(withId(R.id.etCurrentPassword)).check(matches(isDisplayed())) }

            type(R.id.etCurrentPassword, "password123")
            type(R.id.etNewPassword, "brandnewpass1")
            type(R.id.etConfirmPassword, "brandnewpass1")
            onView(withId(R.id.btnChangePassword)).perform(scrollTo(), click())

            // the confirmation is shown for a couple of seconds, and then the profile is shown again
            eventually { onView(withId(R.id.tvProfileName)).check(matches(isDisplayed())) }
        }

        app.userDatabaseSystem.logout()
        assertTrue(runBlocking { app.userDatabaseSystem.login(user.email, "brandnewpass1").wasSuccessful })
    }

    // ---- password changed

    @Test
    fun passwordChanged_showsTheConfirmationMessage() {
        ActivityScenario.launch(PasswordChangedActivity::class.java).use {
            onView(withId(R.id.tvSuccess)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun passwordChanged_takesTheUserBackToTheirProfileAfterAMoment() {
        UserSession.login(createTestUser(username = "accountconfirmed"))

        ActivityScenario.launch(PasswordChangedActivity::class.java).use {
            eventually { onView(withId(R.id.tvProfileName)).check(matches(isDisplayed())) }
        }
    }
}
