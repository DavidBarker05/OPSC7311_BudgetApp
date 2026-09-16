package com.example.mybudgettree.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.mybudgettree.BudgetTreeApplication
import com.example.mybudgettree.UserSession
import com.example.mybudgettree.database.AppDatabase
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem
import com.example.mybudgettree.database.managers.ExpenseDatabaseSystem
import com.example.mybudgettree.database.managers.IncomeDatabaseSystem
import com.example.mybudgettree.database.managers.MonthlyGoalDatabaseSystem
import com.example.mybudgettree.database.managers.SavingsContributionDatabaseSystem
import com.example.mybudgettree.database.managers.SavingsGoalDatabaseSystem
import com.example.mybudgettree.database.managers.UserDatabaseSystem
import com.example.mybudgettree.database.managers.UserTreeDatabaseSystem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.time.LocalDate

/**
 * Base class for Espresso UI tests that drive real Activities. [BudgetTreeApplication] normally
 * points at the persistent on-device database, which would leak state between test runs (e.g.
 * "email already in use" on the second run of a signup test). Before each test we swap the
 * running app's database systems for a fresh in-memory database, the same way
 * [com.example.mybudgettree.database.DatabaseTestBase] does for DB-layer tests, so UI tests
 * start from a clean slate and never touch real user data
 */
abstract class UiTestBase {
    protected lateinit var app: BudgetTreeApplication
    protected lateinit var db: AppDatabase

    @Before
    fun setUpApp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        app.database = db
        app.userDatabaseSystem = UserDatabaseSystem(db.userDao())
        app.categoryDatabaseSystem = CategoryDatabaseSystem(db.categoryDao(), app.userDatabaseSystem)
        app.expenseDatabaseSystem = ExpenseDatabaseSystem(db.expenseDao(), app.userDatabaseSystem, app.categoryDatabaseSystem)
        app.incomeDatabaseSystem = IncomeDatabaseSystem(db.incomeDao(), app.userDatabaseSystem, app.categoryDatabaseSystem)
        app.savingsGoalDatabaseSystem = SavingsGoalDatabaseSystem(db.savingsGoalDao(), app.userDatabaseSystem)
        app.savingsContributionDatabaseSystem =
            SavingsContributionDatabaseSystem(db.savingsContributionDao(), app.savingsGoalDatabaseSystem)
        app.userTreeDatabaseSystem = UserTreeDatabaseSystem(db.userTreeDao())
        app.monthlyGoalDatabaseSystem = MonthlyGoalDatabaseSystem(db.monthlyGoalDao())
        UserSession.logout()
    }

    @After
    fun tearDownApp() {
        UserSession.logout()
        db.close()
    }

    /**
     * Polls for [text] to appear on screen for up to [timeoutMs], instead of asserting once.
     * Espresso only tracks work already posted to the main thread's message queue, so it can
     * decide the app is "idle" and check the view hierarchy before an async coroutine (e.g.
     * [com.example.mybudgettree.CategoriesActivity] seeding default categories from Room) has
     * actually finished and updated the UI
     */
    protected fun waitForText(text: String, timeoutMs: Long = 5000, intervalMs: Long = 200) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                onView(withText(text)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                lastError = e
                Thread.sleep(intervalMs)
            }
        }
        throw lastError ?: AssertionError("Timed out waiting for text: $text")
    }

    protected fun createTestUser(username: String = "testuser", password: String = "password123"): User = runBlocking {
        app.userDatabaseSystem.createUser(
            username = username,
            password = password,
            email = "$username@example.com",
            phoneNumber = "0821234567".plus(username.hashCode().toString().takeLast(2)),
            displayName = "Test User",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR"
        ).user!!
    }
}
