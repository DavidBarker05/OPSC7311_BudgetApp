package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class UserDatabaseSystemTest : DatabaseTestBase() {

    private suspend fun signUp(
        email: String = "david@example.com",
        password: String = "password123",
        phoneNumber: String = "0821111111",
        displayName: String = "David",
        currency: String = "ZAR"
    ) = userDatabaseSystem.createUser(
        email = email,
        password = password,
        phoneNumber = phoneNumber,
        displayName = displayName,
        dateOfBirth = LocalDate.of(2000, 1, 1),
        currency = currency
    )

    // ---- createUser

    @Test
    fun createUser_success() = runBlocking {
        val result = signUp()
        assertTrue(result.wasSuccessful)
        assertEquals("david@example.com", result.value?.email)
        assertEquals("David", result.value?.displayName)
        assertEquals("2000-01-01", result.value?.dateOfBirth)
        assertTrue(result.value!!.uid.isNotBlank())
    }

    @Test
    fun createUser_leavesTheUserSignedIn() = runBlocking {
        val created = signUp().value!!
        assertEquals(created, userDatabaseSystem.getCurrentUser())
    }

    @Test
    fun createUser_alsoCreatesTheMoneyTree() = runBlocking {
        val created = signUp().value!!
        val tree = userTreeDatabaseSystem.findUserTree(created)
        assertNotNull(tree)
        assertEquals(YearMonth.now().toString(), tree?.yearMonth)
    }

    @Test
    fun createUser_duplicateEmail_fails() = runBlocking {
        signUp()
        val result = signUp(phoneNumber = "0823333333")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_duplicatePhoneNumber_fails() = runBlocking {
        signUp()
        userDatabaseSystem.logout()
        val result = signUp(email = "other@example.com")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_duplicatePhoneNumber_rollsBackTheAccount() = runBlocking {
        signUp()
        userDatabaseSystem.logout()
        signUp(email = "other@example.com")
        userDatabaseSystem.logout()
        // The half-created account must have been deleted again, so it can't be logged in to
        assertFalse(userDatabaseSystem.login("other@example.com", "password123").wasSuccessful)
    }

    @Test
    fun createUser_duplicatePhoneNumber_unnormalizedFormatAlsoFails() = runBlocking {
        signUp(phoneNumber = "0821111111")
        userDatabaseSystem.logout()
        val result = signUp(email = "other@example.com", phoneNumber = "082 111-1111")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_weakPassword_fails() = runBlocking {
        val result = signUp(password = "123")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_invalidEmail_fails() = runBlocking {
        val result = signUp(email = "not-an-email")
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_blankFields_fail() = runBlocking {
        assertFalse(signUp(email = "").wasSuccessful)
        assertFalse(signUp(password = "").wasSuccessful)
        assertFalse(signUp(phoneNumber = "").wasSuccessful)
        assertFalse(signUp(displayName = "").wasSuccessful)
        assertFalse(signUp(currency = "").wasSuccessful)
    }

    @Test
    fun createUser_phoneNumberWithSpacesAndDashes_isNormalized() = runBlocking {
        val result = signUp(phoneNumber = "082 111-1111")
        assertTrue(result.wasSuccessful)
        assertEquals("0821111111", result.value?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberWithParentheses_isNormalized() = runBlocking {
        val result = signUp(phoneNumber = "(082) 111 1111")
        assertTrue(result.wasSuccessful)
        assertEquals("0821111111", result.value?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberWithCountryCode_isKeptWithPlus() = runBlocking {
        val result = signUp(phoneNumber = "+27 82 111 1111")
        assertTrue(result.wasSuccessful)
        assertEquals("+27821111111", result.value?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberTooShort_fails() = runBlocking {
        assertFalse(signUp(phoneNumber = "12345").wasSuccessful)
    }

    @Test
    fun createUser_phoneNumberTooLong_fails() = runBlocking {
        assertFalse(signUp(phoneNumber = "1234567890123456").wasSuccessful)
    }

    // ---- login / current user / logout

    @Test
    fun login_correctCredentials_succeeds() = runBlocking {
        val created = signUp().value!!
        userDatabaseSystem.logout()
        val result = userDatabaseSystem.login("david@example.com", "password123")
        assertTrue(result.wasSuccessful)
        assertEquals(created, result.value)
    }

    @Test
    fun login_wrongPassword_fails() = runBlocking {
        signUp()
        userDatabaseSystem.logout()
        val result = userDatabaseSystem.login("david@example.com", "wrongpassword")
        assertFalse(result.wasSuccessful)
        assertNull(auth.currentUser)
    }

    @Test
    fun login_unknownEmail_givesTheSameErrorAsAWrongPassword() = runBlocking {
        signUp()
        userDatabaseSystem.logout()
        val wrongPassword = userDatabaseSystem.login("david@example.com", "wrongpassword")
        val unknownEmail = userDatabaseSystem.login("nobody@example.com", "password123")
        assertFalse(unknownEmail.wasSuccessful)
        assertEquals(wrongPassword.errMsg, unknownEmail.errMsg)
    }

    @Test
    fun login_blankFields_fail() = runBlocking {
        assertFalse(userDatabaseSystem.login("", "password123").wasSuccessful)
        assertFalse(userDatabaseSystem.login("david@example.com", "").wasSuccessful)
    }

    @Test
    fun getCurrentUser_afterLogout_isNull() = runBlocking {
        signUp()
        userDatabaseSystem.logout()
        assertNull(userDatabaseSystem.getCurrentUser())
    }

    // ---- isPhoneNumberInUse

    @Test
    fun isPhoneNumberInUse_takenNumber_isTrue_evenWhenFormattedDifferently() = runBlocking {
        signUp(phoneNumber = "0821111111")
        assertTrue(userDatabaseSystem.isPhoneNumberInUse("0821111111"))
        assertTrue(userDatabaseSystem.isPhoneNumberInUse("082 111-1111"))
    }

    @Test
    fun isPhoneNumberInUse_freeNumber_isFalse() = runBlocking {
        signUp(phoneNumber = "0821111111")
        assertFalse(userDatabaseSystem.isPhoneNumberInUse("0829999999"))
        assertFalse(userDatabaseSystem.isPhoneNumberInUse(""))
    }

    // ---- updates

    @Test
    fun updateDisplayName_succeeds_andIsSaved() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updateDisplayName(user, "Dave")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("Dave", userDatabaseSystem.getCurrentUser()?.displayName)
    }

    @Test
    fun updateDisplayName_blank_fails_andSame_noChange() = runBlocking {
        val user = signUp().value!!
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updateDisplayName(user, " ").status)
        assertEquals(UpdateReturnStatus.NoChange, userDatabaseSystem.updateDisplayName(user, user.displayName).status)
    }

    @Test
    fun updateCurrency_succeeds_andIsSaved() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updateCurrency(user, "USD")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("USD", userDatabaseSystem.getCurrentUser()?.currency)
    }

    @Test
    fun updateDateOfBirth_succeeds_andIsSaved() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updateDateOfBirth(user, LocalDate.of(1999, 12, 31))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(LocalDate.of(1999, 12, 31), userDatabaseSystem.getCurrentUser()?.dateOfBirthAsLocalDate())
    }

    @Test
    fun updatePhoneNumber_withSpacesAndDashes_isNormalized() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updatePhoneNumber(user, "083 222-2222")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals("0832222222", result.value?.phoneNumber)
        assertEquals("0832222222", userDatabaseSystem.getCurrentUser()?.phoneNumber)
    }

    @Test
    fun updatePhoneNumber_freesTheOldNumber_andClaimsTheNewOne() = runBlocking {
        val user = signUp(phoneNumber = "0821111111").value!!
        userDatabaseSystem.updatePhoneNumber(user, "0832222222")
        assertFalse(userDatabaseSystem.isPhoneNumberInUse("0821111111"))
        assertTrue(userDatabaseSystem.isPhoneNumberInUse("0832222222"))
    }

    @Test
    fun updatePhoneNumber_numberTakenByAnotherUser_fails() = runBlocking {
        signUp(email = "first@example.com", phoneNumber = "0821111111")
        val second = signUp(email = "second@example.com", phoneNumber = "0832222222").value!!
        val result = userDatabaseSystem.updatePhoneNumber(second, "0821111111")
        assertEquals(UpdateReturnStatus.Failed, result.status)
        assertEquals("0832222222", userDatabaseSystem.getCurrentUser()?.phoneNumber)
    }

    @Test
    fun updatePhoneNumber_invalid_fails_andSame_noChange() = runBlocking {
        val user = signUp().value!!
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updatePhoneNumber(user, "12345").status)
        assertEquals(UpdateReturnStatus.NoChange, userDatabaseSystem.updatePhoneNumber(user, "082 111 1111").status)
    }

    @Test
    fun updatePassword_newPasswordWorks_oldOneDoesNot() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updatePassword(user, "newpassword456")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        userDatabaseSystem.logout()
        assertFalse(userDatabaseSystem.login("david@example.com", "password123").wasSuccessful)
        assertTrue(userDatabaseSystem.login("david@example.com", "newpassword456").wasSuccessful)
    }

    @Test
    fun updatePassword_weakPassword_fails() = runBlocking {
        val user = signUp().value!!
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updatePassword(user, "123").status)
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updatePassword(user, "").status)
    }

    @Test
    fun updateEmail_isPendingUntilTheLinkIsClicked_soTheUserIsUnchanged() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updateEmail(user, "new@example.com")
        assertEquals(UpdateReturnStatus.PendingVerification, result.status)
        assertEquals("david@example.com", result.value?.email)
        assertEquals("david@example.com", userDatabaseSystem.getCurrentUser()?.email)
    }

    @Test
    fun updateEmail_invalid_fails_andSame_noChange() = runBlocking {
        val user = signUp().value!!
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updateEmail(user, "not-an-email").status)
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updateEmail(user, "").status)
        assertEquals(UpdateReturnStatus.NoChange, userDatabaseSystem.updateEmail(user, user.email).status)
    }

    // ---- profile photo

    @Test
    fun findProfilePhoto_noPhoto_isNull() = runBlocking {
        val user = signUp().value!!
        assertNull(userDatabaseSystem.findProfilePhoto(user))
    }

    @Test
    fun updateProfilePhoto_savesTheBytes() = runBlocking {
        val user = signUp().value!!
        val photo = ByteArray(1000) { it.toByte() }
        val result = userDatabaseSystem.updateProfilePhoto(user, photo)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertArrayEquals(photo, userDatabaseSystem.findProfilePhoto(user))
    }

    @Test
    fun updateProfilePhoto_replacesThePreviousPhoto() = runBlocking {
        val user = signUp().value!!
        userDatabaseSystem.updateProfilePhoto(user, byteArrayOf(1, 2, 3))
        userDatabaseSystem.updateProfilePhoto(user, byteArrayOf(4, 5, 6))
        assertArrayEquals(byteArrayOf(4, 5, 6), userDatabaseSystem.findProfilePhoto(user))
    }

    @Test
    fun updateProfilePhoto_null_removesThePhoto() = runBlocking {
        val user = signUp().value!!
        userDatabaseSystem.updateProfilePhoto(user, byteArrayOf(1, 2, 3))
        val result = userDatabaseSystem.updateProfilePhoto(user, null)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertNull(userDatabaseSystem.findProfilePhoto(user))
    }

    @Test
    fun updateProfilePhoto_emptyOrTooLarge_fails() = runBlocking {
        val user = signUp().value!!
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updateProfilePhoto(user, ByteArray(0)).status)
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updateProfilePhoto(user, ByteArray(500_001)).status)
    }

    // ---- deleteUser

    @Test
    fun deleteUser_wrongPassword_deletesNothing() = runBlocking {
        val user = signUp().value!!
        val status = userDatabaseSystem.deleteUser(user, "wrongpassword")
        assertEquals(DeleteReturnStatus.ReauthenticationFailed, status)
        assertEquals(user, userDatabaseSystem.getCurrentUser())
    }

    @Test
    fun deleteUser_correctPassword_deletesTheAccount() = runBlocking {
        val user = signUp().value!!
        val status = userDatabaseSystem.deleteUser(user, "password123")
        assertEquals(DeleteReturnStatus.Deleted, status)
        assertNull(auth.currentUser)
        assertFalse(userDatabaseSystem.login("david@example.com", "password123").wasSuccessful)
    }

    @Test
    fun deleteUser_freesThePhoneNumberForSomeoneElse() = runBlocking {
        val user = signUp(phoneNumber = "0821111111").value!!
        userDatabaseSystem.deleteUser(user, "password123")
        val result = signUp(email = "other@example.com", phoneNumber = "0821111111")
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun deleteUser_alsoDeletesTheUsersData() = runBlocking {
        val user = signUp().value!!
        val category = createTestCategory(user)
        userDatabaseSystem.updateProfilePhoto(user, byteArrayOf(1, 2, 3))
        userDatabaseSystem.deleteUser(user, "password123")

        // Signing back in as a brand new account, nothing from the old one can be reached: the old uid is gone
        val newUser = signUp(email = "other@example.com", phoneNumber = "0829999999").value!!
        assertTrue(categoryDatabaseSystem.getAllCategoriesForUser(newUser).values!!.isEmpty())
        assertFalse(categoryDatabaseSystem.findCategory(category.id).wasSuccessful)
    }

    @Test
    fun deleteUser_forSomeoneWhoIsNotSignedIn_doesNotExist() = runBlocking {
        val first = signUp(email = "first@example.com", phoneNumber = "0821111111").value!!
        signUp(email = "second@example.com", phoneNumber = "0832222222")
        assertEquals(DeleteReturnStatus.DoesNotExist, userDatabaseSystem.deleteUser(first, "password123"))
    }
}
