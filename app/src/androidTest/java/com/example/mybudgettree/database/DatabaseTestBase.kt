package com.example.mybudgettree.database

import androidx.test.core.app.ApplicationProvider
import com.example.mybudgettree.FirebaseEmulator
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.time.LocalDate

/**
 * Base class for database tests. They run against the Firebase Emulator Suite instead of the real project (see
 * [FirebaseEmulator]), so `firebase emulators:start` must be running before the tests start
 *
 * The emulators are wiped once per run, not per test (see [FirebaseEmulator]). Tests can't affect each other because
 * each one makes its own user, and all data is stored under the user that owns it
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
    protected val userTreeDatabaseSystem = UserTreeDatabaseSystem(auth, firestore)
    protected val monthlyGoalDatabaseSystem = MonthlyGoalDatabaseSystem(auth, firestore)
    protected val imageStorageSystem: ImageStorageSystem = LocalImageStorageSystem(ApplicationProvider.getApplicationContext())

    init {
        FirebaseEmulator.connect()
    }

    @Before
    fun setUpDatabase() {
        auth.signOut()
    }

    @After
    fun tearDownDatabase() {
        auth.signOut()
    }

    /**
     * Creates a user and leaves them signed in. Creating a second user signs the first one out, because Firebase only
     * keeps one user signed in at a time
     *
     * @param name The start of the email address, to make the emulator dashboard easier to read. A random part is
     * added, so the same name can be used in many tests
     */
    protected fun createTestUser(name: String = "testuser"): User = runBlocking {
        val result = userDatabaseSystem.createUser(
            email = FirebaseEmulator.uniqueEmail(name),
            password = "password123",
            phoneNumber = FirebaseEmulator.nextPhoneNumber(),
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
