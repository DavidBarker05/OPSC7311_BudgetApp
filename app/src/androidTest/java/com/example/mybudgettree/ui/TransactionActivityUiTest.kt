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
import com.example.mybudgettree.TransactionActivity
import com.example.mybudgettree.UserSession
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionActivityUiTest : UiTestBase() {

    @Test
    fun freshUser_showsZeroBalanceAndEmptyState() {
        UserSession.login(createTestUser(username = "transactionuser"))

        ActivityScenario.launch(TransactionActivity::class.java).use {
            onView(withId(R.id.tvTotalBalance)).check(matches(withText("R0.00")))
            onView(withId(R.id.tvEmptyTransactions)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun tappingIncomeAndExpenseFilters_doesNotCrash() {
        UserSession.login(createTestUser(username = "transactionuser2"))

        ActivityScenario.launch(TransactionActivity::class.java).use {
            onView(withId(R.id.cardIncome)).perform(click())
            onView(withId(R.id.tvEmptyTransactions)).check(matches(isDisplayed()))

            onView(withId(R.id.cardExpense)).perform(click())
            onView(withId(R.id.tvEmptyTransactions)).check(matches(isDisplayed()))
        }
    }
}
