package com.example.mybudgettree.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.DeviceId
import com.example.mybudgettree.EditTransactionActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.UserSession
import com.example.mybudgettree.database.entries.DeviceImages
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * Drives [EditTransactionActivity] for an income or expense that has an image on another device only. The rules for
 * deciding when to ask, and what saying no stores, are tested without a screen, so these check the question appears
 * once and that answering it is saved straight away
 */
@RunWith(AndroidJUnit4::class)
class ImageOnAnotherDeviceUiTest : UiTestBase() {

    private val otherDevice = "some-other-device"

    private fun expenseWithImageOnAnotherDevice(username: String): String = runBlocking {
        val user = createTestUser(username = username)
        UserSession.login(user)
        val category = app.categoryDatabaseSystem.createCategory(user, "Food").value!!
        app.expenseDatabaseSystem.createExpense(
            category, "Lunch", 50.0, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 5),
            imagePath = "/somewhere/else.jpg", deviceId = otherDevice
        ).value!!.id
    }

    private fun incomeWithImageOnAnotherDevice(username: String): String = runBlocking {
        val user = createTestUser(username = username)
        UserSession.login(user)
        val category = app.categoryDatabaseSystem.createCategory(user, "Work").value!!
        app.incomeDatabaseSystem.createIncome(
            category, "Payslip", 500.0, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 5),
            imagePath = "/somewhere/else.jpg", deviceId = otherDevice
        ).value!!.id
    }

    private fun launch(id: String, isIncome: Boolean): ActivityScenario<EditTransactionActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), EditTransactionActivity::class.java)
            .putExtra(EditTransactionActivity.EXTRA_TRANSACTION_ID, id)
            .putExtra(EditTransactionActivity.EXTRA_IS_INCOME, isIncome)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun expense_withAnImageOnlyOnAnotherDevice_asksAboutAddingOneHere() {
        val id = expenseWithImageOnAnotherDevice("otherimgexpense")

        launch(id, isIncome = false).use {
            eventually { onView(withText(R.string.image_other_device_expense_message)).inRoot(isDialog()).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun income_withAnImageOnlyOnAnotherDevice_asksAboutAddingOneHere() {
        val id = incomeWithImageOnAnotherDevice("otherimgincome")

        launch(id, isIncome = true).use {
            eventually { onView(withText(R.string.image_other_device_income_message)).inRoot(isDialog()).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun expense_sayingNo_remembersItAndNeverAsksAgain() {
        val id = expenseWithImageOnAnotherDevice("otherimgno")

        launch(id, isIncome = false).use {
            eventually { onView(withText(R.string.no)).inRoot(isDialog()).perform(click()) }
            eventually {
                val saved = runBlocking { app.expenseDatabaseSystem.findExpense(id).value!! }
                assertEquals(DeviceImages.DECLINED, saved.imagePaths[DeviceId.get(app)])
            }
        }

        launch(id, isIncome = false).use {
            eventually { onView(withText("Lunch")).check(matches(isDisplayed())) }
            onView(withText(R.string.image_other_device_expense_message)).check(doesNotExist())
        }
    }

    @Test
    fun income_sayingNo_remembersIt() {
        val id = incomeWithImageOnAnotherDevice("otherimgnoincome")

        launch(id, isIncome = true).use {
            eventually { onView(withText(R.string.no)).inRoot(isDialog()).perform(click()) }
            eventually {
                val saved = runBlocking { app.incomeDatabaseSystem.findIncome(id).value!! }
                assertEquals(DeviceImages.DECLINED, saved.imagePaths[DeviceId.get(app)])
            }
        }
    }

    @Test
    fun expense_sayingYes_opensThePictureChoice_andStoresNothingYet() {
        val id = expenseWithImageOnAnotherDevice("otherimgyes")

        launch(id, isIncome = false).use {
            eventually { onView(withText(R.string.yes)).inRoot(isDialog()).perform(click()) }
            // the choice between taking a photo and picking one is a dialog of its own
            eventually { onView(withText(R.string.image_other_device_expense_message)).check(doesNotExist()) }
        }

        val saved = runBlocking { app.expenseDatabaseSystem.findExpense(id).value!! }
        assertFalse(saved.imagePaths.containsKey(DeviceId.get(app)))
    }

    @Test
    fun expense_thatAlreadyHasAnImageOnThisDevice_isNotAsked() {
        val user = createTestUser(username = "otherimgown")
        UserSession.login(user)
        val id = runBlocking {
            val category = app.categoryDatabaseSystem.createCategory(user, "Food").value!!
            app.expenseDatabaseSystem.createExpense(
                category, "Lunch", 50.0, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 5),
                imagePath = "/somewhere/mine.jpg", deviceId = DeviceId.get(app)
            ).value!!.id
        }

        launch(id, isIncome = false).use {
            eventually { onView(withText("Lunch")).check(matches(isDisplayed())) }
            onView(withText(R.string.image_other_device_expense_message)).check(doesNotExist())
        }
    }
}
