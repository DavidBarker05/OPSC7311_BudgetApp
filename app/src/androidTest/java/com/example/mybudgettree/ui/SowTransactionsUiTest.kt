package com.example.mybudgettree.ui

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.CategoryDetailActivity
import com.example.mybudgettree.R
import com.example.mybudgettree.SowExpensesActivity
import com.example.mybudgettree.SowIncomeActivity
import com.example.mybudgettree.UserSession
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the screens that add an expense or an income, against the Firebase emulators. These go through the real
 * screens, which is the only place the values passed to the database (like the device ID) are put together. Picking a
 * receipt needs the camera or gallery, so these cover saving without one. Saving with an image is covered by the
 * database tests
 */
@RunWith(AndroidJUnit4::class)
class SowTransactionsUiTest : UiTestBase() {

    private fun setUp(username: String): Pair<User, Category> = runBlocking {
        val user = createTestUser(username = username)
        UserSession.login(user)
        user to app.categoryDatabaseSystem.createCategory(user, "Food").value!!
    }

    private fun launch(screen: Class<*>, category: Category): ActivityScenario<*> =
        ActivityScenario.launch<androidx.appcompat.app.AppCompatActivity>(
            Intent(ApplicationProvider.getApplicationContext(), screen)
                .putExtra(CategoryDetailActivity.EXTRA_CATEGORY_ID, category.id)
        )

    private fun type(id: Int, text: String) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard())
    }

    private fun save(scenario: ActivityScenario<*>) {
        onView(withId(R.id.btnSaveExpense)).perform(scrollTo(), click())
        // the screen closes itself once the item is saved
        eventually { assertEquals(Lifecycle.State.DESTROYED, scenario.state) }
    }

    // ---- expenses

    @Test
    fun expense_filledIn_isSavedAndTheScreenCloses() {
        val (_, category) = setUp("sowexpense")

        launch(SowExpensesActivity::class.java, category).use { scenario ->
            eventually { type(R.id.etExpenseTitle, "Lunch") }
            type(R.id.etAmount, "45.50")
            save(scenario)
        }

        val saved = runBlocking { app.expenseDatabaseSystem.retrieveAllExpensesForCategory(category).values.orEmpty() }
        assertEquals(1, saved.size)
        assertEquals("Lunch", saved.single().description)
        assertEquals(45.5, saved.single().amount, 0.0)
        assertTrue(saved.single().imagePaths.isEmpty())
    }

    @Test
    fun expense_withNoTitleOrAmount_isNotSaved() {
        val (_, category) = setUp("sowexpenseblank")

        launch(SowExpensesActivity::class.java, category).use { scenario ->
            eventually { type(R.id.etExpenseTitle, "") }
            onView(withId(R.id.btnSaveExpense)).perform(scrollTo(), click())

            assertTrue(scenario.state != Lifecycle.State.DESTROYED)
        }

        val saved = runBlocking { app.expenseDatabaseSystem.retrieveAllExpensesForCategory(category).values.orEmpty() }
        assertTrue(saved.isEmpty())
    }

    // ---- incomes

    @Test
    fun income_filledIn_isSavedAndTheScreenCloses() {
        val (_, category) = setUp("sowincome")

        launch(SowIncomeActivity::class.java, category).use { scenario ->
            eventually { type(R.id.etExpenseTitle, "Payslip") }
            type(R.id.etAmount, "1200")
            save(scenario)
        }

        val saved = runBlocking { app.incomeDatabaseSystem.retrieveAllIncomesForCategory(category).values.orEmpty() }
        assertEquals(1, saved.size)
        assertEquals("Payslip", saved.single().description)
        assertEquals(1200.0, saved.single().amount, 0.0)
        assertTrue(saved.single().imagePaths.isEmpty())
    }

    @Test
    fun income_withNoTitleOrAmount_isNotSaved() {
        val (_, category) = setUp("sowincomeblank")

        launch(SowIncomeActivity::class.java, category).use { scenario ->
            eventually { type(R.id.etExpenseTitle, "") }
            onView(withId(R.id.btnSaveExpense)).perform(scrollTo(), click())

            assertTrue(scenario.state != Lifecycle.State.DESTROYED)
        }

        val saved = runBlocking { app.incomeDatabaseSystem.retrieveAllIncomesForCategory(category).values.orEmpty() }
        assertTrue(saved.isEmpty())
    }
}
