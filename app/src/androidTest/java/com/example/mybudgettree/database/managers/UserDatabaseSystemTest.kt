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

    @Test
    fun createUser_usernameWithSpaces_fails() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david smith",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "0821111111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_usernameWithSymbols_fails() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david_smith!",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "0821111111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_usernameWithNonEnglishLetters_fails() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "dävid",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "0821111111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_alphanumericUsername_succeeds() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david123",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "0821111111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun updateUsername_invalidCharacters_fails() = runBlocking {
        val user = createTestUser("david")
        val result = userDatabaseSystem.updateUsername(user, "david smith")
        assertEquals(UserDatabaseSystem.UpdateUserReturnStatus.Failed, result.status)
    }

    @Test
    fun updateUsername_validAlphanumeric_succeeds() = runBlocking {
        val user = createTestUser("david")
        val result = userDatabaseSystem.updateUsername(user, "david123")
        assertEquals(UserDatabaseSystem.UpdateUserReturnStatus.Succeeded, result.status)
        assertEquals("david123", result.user?.username)
    }

    @Test
    fun createUser_phoneNumberWithSpacesAndDashes_isNormalized() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "082 111-1111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertTrue(result.wasSuccessful)
        assertEquals("0821111111", result.user?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberWithParentheses_isNormalized() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "(082) 111 1111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertTrue(result.wasSuccessful)
        assertEquals("0821111111", result.user?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberWithCountryCode_isKeptWithPlus() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "+27 82 111 1111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertTrue(result.wasSuccessful)
        assertEquals("+27821111111", result.user?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberTooShort_fails() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "12345",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_phoneNumberTooLong_fails() = runBlocking {
        val result = userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "1234567890123456",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun findUserByPhoneNumber_withUnnormalizedInput_findsNormalizedUser() = runBlocking {
        userDatabaseSystem.createUser(
            username = "david",
            password = "password123",
            email = "david@example.com",
            phoneNumber = "0821111111",
            displayName = "David",
            dateOfBirth = LocalDate.of(2000, 1, 1),
            currency = "ZAR",
            profilePhotoPath = null
        )
        val result = userDatabaseSystem.findUserByPhoneNumber("082 111-1111")
        assertTrue(result.wasSuccessful)
        assertEquals("david", result.user?.username)
    }

    @Test
    fun updatePhoneNumber_withSpacesAndDashes_isNormalized() = runBlocking {
        val user = createTestUser("david")
        val result = userDatabaseSystem.updatePhoneNumber(user, "083 222-2222")
        assertEquals(UserDatabaseSystem.UpdateUserReturnStatus.Succeeded, result.status)
        assertEquals("0832222222", result.user?.phoneNumber)
    }
}
