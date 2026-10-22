package com.example.mybudgettree.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.MoneyFormatter
import com.example.mybudgettree.R
import com.example.mybudgettree.SpendingDashboardActivity
import com.example.mybudgettree.UserSession
import com.example.mybudgettree.database.entries.User
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Drives the real [SpendingDashboardActivity] with Espresso against the Firebase emulators. The sums, colours and
 * validation rules are tested without a screen in SpendingDashboardCalculatorTest, so these check that the screen puts
 * them on show
 */
@RunWith(AndroidJUnit4::class)
class SpendingDashboardActivityUiTest : UiTestBase() {

    // Spends today, so it is inside the dashboard's starting period (the first of the month up to today)
    private fun spend(user: User, categoryName: String, amount: Double) = runBlocking {
        val category = app.categoryDatabaseSystem.createCategory(user, categoryName).value!!
        app.expenseDatabaseSystem.createExpense(category, "Spend", amount, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 5))
    }

    private fun typeLimits(min: String, max: String) {
        onView(withId(R.id.etMin)).perform(replaceText(min), closeSoftKeyboard())
        onView(withId(R.id.etMax)).perform(replaceText(max), closeSoftKeyboard())
        onView(withId(R.id.btnShow)).perform(scrollTo(), click())
    }

    @Test
    fun showsEachCategoryWithItsSpending() {
        val user = createTestUser(username = "dashuser")
        spend(user, "Food", 120.0)
        spend(user, "Fuel", 30.0)
        UserSession.login(user)

        ActivityScenario.launch(SpendingDashboardActivity::class.java).use {
            eventually { onView(withText("Food")).perform(scrollTo()).check(matches(isDisplayed())) }
            onView(withText(MoneyFormatter.format(120.0))).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("Fuel")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun minimumAndMaximumStartFromTheSavedMonthlyGoal() {
        val user = createTestUser(username = "dashgoaluser")
        runBlocking { app.monthlyGoalDatabaseSystem.saveGoal(user, YearMonth.now(), 100.0, 250.0) }
        UserSession.login(user)

        ActivityScenario.launch(SpendingDashboardActivity::class.java).use {
            eventually {
                onView(withId(R.id.etMin)).check(matches(withText("100")))
                onView(withId(R.id.etMax)).check(matches(withText("250")))
            }
        }
    }

    @Test
    fun spendingOverTheMaximum_isSaidInWords_notOnlyShownInRed() {
        val user = createTestUser(username = "dashoveruser")
        spend(user, "Food", 300.0)
        UserSession.login(user)

        ActivityScenario.launch(SpendingDashboardActivity::class.java).use {
            eventually { onView(withText("Food")).perform(scrollTo()).check(matches(isDisplayed())) }
            typeLimits(min = "100", max = "200")

            val label = app.getString(R.string.dashboard_over_max, MoneyFormatter.format(100.0))
            eventually { onView(withText(label)).perform(scrollTo()).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun minimumAboveMaximum_showsAnError() {
        val user = createTestUser(username = "dasherroruser")
        UserSession.login(user)

        ActivityScenario.launch(SpendingDashboardActivity::class.java).use {
            typeLimits(min = "500", max = "100")

            onView(withText(R.string.dashboard_error_min_above_max)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingAnotherCategory_graphsThatCategory() {
        val user = createTestUser(username = "dashtapuser")
        spend(user, "Food", 200.0)
        spend(user, "Fuel", 50.0)
        UserSession.login(user)

        ActivityScenario.launch(SpendingDashboardActivity::class.java).use {
            // starts on the category with the most spending
            eventually { onView(withId(R.id.tvChartTitle)).perform(scrollTo()).check(matches(withSubstring("Food,"))) }

            onView(withText("Fuel")).perform(scrollTo(), click())

            eventually { onView(withId(R.id.tvChartTitle)).perform(scrollTo()).check(matches(withSubstring("Fuel,"))) }
        }
    }
}
