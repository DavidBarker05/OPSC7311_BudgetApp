package com.example.mybudgettree.database

import androidx.test.core.app.ApplicationProvider
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.SavingsGoal
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.managers.CategoryDatabaseSystem
import com.example.mybudgettree.database.managers.ExpenseDatabaseSystem
import com.example.mybudgettree.database.managers.IncomeDatabaseSystem
import com.example.mybudgettree.database.managers.MonthlyGoalDatabaseSystem
import com.example.mybudgettree.database.managers.SavingsContributionDatabaseSystem
import com.example.mybudgettree.database.managers.SavingsGoalDatabaseSystem
import com.example.mybudgettree.database.managers.UserDatabaseSystem
import com.example.mybudgettree.database.managers.UserTreeDatabaseSystem
import com.example.mybudgettree.imagestorage.ImageStorageSystem
import com.example.mybudgettree.imagestorage.LocalImageStorageSystem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for database tests. They run against the Firebase Emulator Suite instead of the real project, so
 * `firebase emulators:start` must be running on the computer before the tests start. The emulators are reached from the
 * Android emulator through 10.0.2.2, the alias for the computer's localhost
 *
 * Each test starts and ends with both emulators wiped, so tests can't affect each other
 */
abstract class DatabaseTestBase {
    protected val auth: FirebaseAuth = FirebaseAuth.getInstance()
    protected val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    protected val userDatabaseSystem = UserDatabaseSystem(auth, firestore)
    protected val categoryDatabaseSystem = CategoryDatabaseSystem(auth, firestore)
    protected val expenseDatabaseSystem = ExpenseDatabaseSystem(auth, firestore)
    protected val incomeDatabaseSystem = IncomeDatabaseSystem(auth, firestore)
    protected val savingsGoalDatabaseSystem = SavingsGoalDatabaseSystem(auth, firestore)
    protected val savingsContributionDatabaseSystem = SavingsContributionDatabaseSystem(auth, firestore)
    protected val userTreeDatabaseSystem = UserTreeDatabaseSystem(firestore)
    protected val monthlyGoalDatabaseSystem = MonthlyGoalDatabaseSystem(auth, firestore)
    protected val imageStorageSystem: ImageStorageSystem = LocalImageStorageSystem(ApplicationProvider.getApplicationContext())

    companion object {
        private const val EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_PORT = 9099
        private const val FIRESTORE_PORT = 8080

        private val userCounter = AtomicInteger()

        // Pointing at the emulators has to happen once, before Firestore does anything. Touching this runs it
        private val connectedToEmulators: Boolean by lazy {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, AUTH_PORT)
            FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            true
        }

        private fun wipe(url: String) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "DELETE"
                check(connection.responseCode in 200..299) { "Could not wipe the emulator at $url: ${connection.responseCode}" }
            } finally {
                connection.disconnect()
            }
        }

        private fun wipeEmulators() {
            val projectId = FirebaseApp.getInstance().options.projectId
            wipe("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts")
            wipe("http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/(default)/documents")
        }
    }

    init {
        check(connectedToEmulators)
    }

    @Before
    fun setUpDatabase() {
        auth.signOut()
        wipeEmulators()
    }

    @After
    fun tearDownDatabase() {
        auth.signOut()
        wipeEmulators()
    }

    /**
     * Creates a user and leaves them signed in. Creating a second user signs the first one out, because Firebase only
     * keeps one user signed in at a time
     *
     * @param name Used to make the email address, so tests can create several different users
     */
    protected fun createTestUser(name: String = "testuser"): User = runBlocking {
        val result = userDatabaseSystem.createUser(
            email = "$name@example.com",
            password = "password123",
            phoneNumber = "08212%05d".format(userCounter.incrementAndGet()),
            displayName = "Test User",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR"
        )
        checkNotNull(result.value) { "Could not create the test user: ${result.errMsg}" }
    }

    protected fun createTestCategory(user: User, categoryName: String = "Groceries"): Category = runBlocking {
        categoryDatabaseSystem.createCategory(user, categoryName).value!!
    }

    protected fun createTestGoal(user: User, goalName: String = "Wedding"): SavingsGoal = runBlocking {
        savingsGoalDatabaseSystem.createGoal(user, goalName).value!!
    }
}
