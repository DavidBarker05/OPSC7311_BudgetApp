package com.example.mybudgettree.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.mybudgettree.BudgetTreeApplication
import com.example.mybudgettree.UserSession
import com.example.mybudgettree.FirebaseEmulator
import com.example.mybudgettree.database.entries.User
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.time.LocalDate

/**
 * Base class for Espresso UI tests that drive real Activities. They run against the Firebase Emulator Suite instead of
 * the real project (see [FirebaseEmulator]), so `firebase emulators:start` must be running before the tests start.
 * Both emulators are wiped before and after each test, so tests start from a clean slate and never touch real data
 */
abstract class UiTestBase {
    protected lateinit var app: BudgetTreeApplication

    @Before
    fun setUpApp() {
        FirebaseEmulator.connect()
        app = ApplicationProvider.getApplicationContext()
        FirebaseEmulator.wipe()
        app.userDatabaseSystem.logout()
        UserSession.logout()
    }

    @After
    fun tearDownApp() {
        UserSession.logout()
        app.userDatabaseSystem.logout()
        FirebaseEmulator.wipe()
    }

    /**
     * Polls for [text] to appear on screen for up to [timeoutMs], instead of asserting once.
     * Espresso only tracks work already posted to the main thread's message queue, so it can
     * decide the app is "idle" and check the view hierarchy before an async coroutine (e.g.
     * [com.example.mybudgettree.CategoriesActivity] seeding default categories from Room) has
     * actually finished and updated the UI
     */
    protected fun waitForText(text: String, timeoutMs: Long = 5000, intervalMs: Long = 200) {
        eventually(timeoutMs, intervalMs) { onView(withText(text)).check(matches(isDisplayed())) }
    }

    /**
     * Retries [block] until it stops throwing or [timeoutMs] runs out, then rethrows the last
     * failure. Wrap any Espresso check whose result depends on async work (data loaded from
     * Room, or a new Activity being launched after a click) rather than assuming it has
     * already happened by the time Espresso looks.
     */
    protected fun eventually(timeoutMs: Long = 5000, intervalMs: Long = 200, block: () -> Unit) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                block()
                return
            } catch (e: Throwable) {
                lastError = e
                Thread.sleep(intervalMs)
            }
        }
        throw lastError ?: AssertionError("Timed out waiting for condition")
    }

    /**
     * Creates a real account in the emulator and leaves it signed in
     *
     * @param username Only used to make the email address (`<username>@example.com`), since people log in with their email now
     */
    protected fun createTestUser(username: String = "testuser", password: String = "password123"): User = runBlocking {
        val result = app.userDatabaseSystem.createUser(
            email = "$username@example.com",
            password = password,
            phoneNumber = "08212%05d".format(userCounter.incrementAndGet()),
            displayName = "Test User",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR"
        )
        checkNotNull(result.value) { "Could not create the test user: ${result.errMsg}" }
    }

    private companion object {
        val userCounter = java.util.concurrent.atomic.AtomicInteger()
    }
}
