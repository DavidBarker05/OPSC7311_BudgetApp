package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UserDatabaseSystemTest : DatabaseTestBase() {

    @Test
    fun createUser_success() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "0821111111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertTrue(result.wasSuccessful)
        assertEquals("david", result.user?.username)
    }

    @Test
    fun createUser_duplicateUsername_fails() = runBlocking {
        createTestUser("david")
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "different@example.com",
            phoneNumber = "0822222222",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_duplicateEmail_fails() = runBlocking {
        val user = createTestUser("david")
        val result = userDatabaseSystem.createUser(
            username = "someoneelse",
            password = "password123",
            email = user.email,
            phoneNumber = "0823333333",
            displayName = "Someone Else",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun findUser_notFound_fails() = runBlocking {
        val result = userDatabaseSystem.findUser("doesnotexist")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun findUser_found_succeeds() = runBlocking {
        createTestUser("david")
        val result = userDatabaseSystem.findUser("david")
        assertTrue(result.wasSuccessful)
        assertEquals("david", result.user?.username)
    }
}
