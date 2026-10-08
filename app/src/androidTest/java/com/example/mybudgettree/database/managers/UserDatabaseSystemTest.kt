package com.example.mybudgettree.database.managers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.FirebaseEmulator
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

    // The emulators are not wiped between tests, so every sign up gets its own email and phone number unless the test
    // passes one in on purpose (e.g. to try a duplicate)
    private suspend fun signUp(
        email: String = FirebaseEmulator.uniqueEmail("david"),
        password: String = "password123",
        phoneNumber: String = FirebaseEmulator.nextPhoneNumber(),
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

    // "0821200001" -> "082 120-0001"
    private fun withSpacesAndDashes(number: String) = "${number.take(3)} ${number.substring(3, 6)}-${number.drop(6)}"

    // "0821200001" -> "(082) 120 0001"
    private fun withParentheses(number: String) = "(${number.take(3)}) ${number.substring(3, 6)} ${number.drop(6)}"

    // "0821200001" -> "+27 82 1200001", which normalizes to "+27821200001"
    private fun withCountryCode(number: String) = "+27 ${number.substring(1, 3)} ${number.drop(3)}"

    // ---- createUser

    @Test
    fun createUser_success() = runBlocking {
        val email = FirebaseEmulator.uniqueEmail("david")
        val result = signUp(email = email)
        assertTrue(result.wasSuccessful)
        assertEquals(email, result.value?.email)
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
        val first = signUp().value!!
        val result = signUp(email = first.email)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_duplicatePhoneNumber_fails() = runBlocking {
        val first = signUp().value!!
        userDatabaseSystem.logout()
        val result = signUp(phoneNumber = first.phoneNumber)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun createUser_duplicatePhoneNumber_rollsBackTheAccount() = runBlocking {
        val first = signUp().value!!
        userDatabaseSystem.logout()
        val otherEmail = FirebaseEmulator.uniqueEmail("other")
        signUp(email = otherEmail, phoneNumber = first.phoneNumber)
        userDatabaseSystem.logout()
        // The half-created account must have been deleted again, so it can't be logged in to
        assertFalse(userDatabaseSystem.login(otherEmail, "password123").wasSuccessful)
    }

    @Test
    fun createUser_duplicatePhoneNumber_unnormalizedFormatAlsoFails() = runBlocking {
        val first = signUp().value!!
        userDatabaseSystem.logout()
        val result = signUp(phoneNumber = withSpacesAndDashes(first.phoneNumber))
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
        val number = FirebaseEmulator.nextPhoneNumber()
        val result = signUp(phoneNumber = withSpacesAndDashes(number))
        assertTrue(result.wasSuccessful)
        assertEquals(number, result.value?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberWithParentheses_isNormalized() = runBlocking {
        val number = FirebaseEmulator.nextPhoneNumber()
        val result = signUp(phoneNumber = withParentheses(number))
        assertTrue(result.wasSuccessful)
        assertEquals(number, result.value?.phoneNumber)
    }

    @Test
    fun createUser_phoneNumberWithCountryCode_isKeptWithPlus() = runBlocking {
        val number = FirebaseEmulator.nextPhoneNumber()
        val result = signUp(phoneNumber = withCountryCode(number))
        assertTrue(result.wasSuccessful)
        assertEquals("+27" + number.drop(1), result.value?.phoneNumber)
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
        val result = userDatabaseSystem.login(created.email, "password123")
        assertTrue(result.wasSuccessful)
        assertEquals(created, result.value)
    }

    @Test
    fun login_wrongPassword_fails() = runBlocking {
        val created = signUp().value!!
        userDatabaseSystem.logout()
        val result = userDatabaseSystem.login(created.email, "wrongpassword")
        assertFalse(result.wasSuccessful)
        assertNull(auth.currentUser)
    }

    @Test
    fun login_unknownEmail_givesTheSameErrorAsAWrongPassword() = runBlocking {
        val created = signUp().value!!
        userDatabaseSystem.logout()
        val wrongPassword = userDatabaseSystem.login(created.email, "wrongpassword")
        val unknownEmail = userDatabaseSystem.login(FirebaseEmulator.uniqueEmail("nobody"), "password123")
        assertFalse(unknownEmail.wasSuccessful)
        assertEquals(wrongPassword.errMsg, unknownEmail.errMsg)
    }

    @Test
    fun login_blankFields_fail() = runBlocking {
        val created = signUp().value!!
        userDatabaseSystem.logout()
        assertFalse(userDatabaseSystem.login("", "password123").wasSuccessful)
        assertFalse(userDatabaseSystem.login(created.email, "").wasSuccessful)
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
        val number = FirebaseEmulator.nextPhoneNumber()
        signUp(phoneNumber = number)
        assertTrue(userDatabaseSystem.isPhoneNumberInUse(number))
        assertTrue(userDatabaseSystem.isPhoneNumberInUse(withSpacesAndDashes(number)))
    }

    @Test
    fun isPhoneNumberInUse_freeNumber_isFalse() = runBlocking {
        signUp()
        assertFalse(userDatabaseSystem.isPhoneNumberInUse(FirebaseEmulator.nextPhoneNumber()))
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
        val newNumber = FirebaseEmulator.nextPhoneNumber()
        val result = userDatabaseSystem.updatePhoneNumber(user, withSpacesAndDashes(newNumber))
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertEquals(newNumber, result.value?.phoneNumber)
        assertEquals(newNumber, userDatabaseSystem.getCurrentUser()?.phoneNumber)
    }

    @Test
    fun updatePhoneNumber_freesTheOldNumber_andClaimsTheNewOne() = runBlocking {
        val user = signUp().value!!
        val newNumber = FirebaseEmulator.nextPhoneNumber()
        userDatabaseSystem.updatePhoneNumber(user, newNumber)
        assertFalse(userDatabaseSystem.isPhoneNumberInUse(user.phoneNumber))
        assertTrue(userDatabaseSystem.isPhoneNumberInUse(newNumber))
    }

    @Test
    fun updatePhoneNumber_numberTakenByAnotherUser_fails() = runBlocking {
        val first = signUp().value!!
        val second = signUp().value!!
        val result = userDatabaseSystem.updatePhoneNumber(second, first.phoneNumber)
        assertEquals(UpdateReturnStatus.Failed, result.status)
        assertEquals(second.phoneNumber, userDatabaseSystem.getCurrentUser()?.phoneNumber)
    }

    @Test
    fun updatePhoneNumber_invalid_fails_andSame_noChange() = runBlocking {
        val user = signUp().value!!
        assertEquals(UpdateReturnStatus.Failed, userDatabaseSystem.updatePhoneNumber(user, "12345").status)
        assertEquals(UpdateReturnStatus.NoChange, userDatabaseSystem.updatePhoneNumber(user, withSpacesAndDashes(user.phoneNumber)).status)
    }

    @Test
    fun updatePassword_newPasswordWorks_oldOneDoesNot() = runBlocking {
        val user = signUp().value!!
        val result = userDatabaseSystem.updatePassword(user, "newpassword456")
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        userDatabaseSystem.logout()
        assertFalse(userDatabaseSystem.login(user.email, "password123").wasSuccessful)
        assertTrue(userDatabaseSystem.login(user.email, "newpassword456").wasSuccessful)
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
        val result = userDatabaseSystem.updateEmail(user, FirebaseEmulator.uniqueEmail("new"))
        assertEquals(UpdateReturnStatus.PendingVerification, result.status)
        assertEquals(user.email, result.value?.email)
        assertEquals(user.email, userDatabaseSystem.getCurrentUser()?.email)
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
        assertFalse(userDatabaseSystem.login(user.email, "password123").wasSuccessful)
    }

    @Test
    fun deleteUser_freesThePhoneNumberForSomeoneElse() = runBlocking {
        val user = signUp().value!!
        userDatabaseSystem.deleteUser(user, "password123")
        val result = signUp(phoneNumber = user.phoneNumber)
        assertTrue(result.wasSuccessful)
    }

    @Test
    fun deleteUser_alsoDeletesTheUsersData() = runBlocking {
        val user = signUp().value!!
        val category = createTestCategory(user)
        userDatabaseSystem.updateProfilePhoto(user, byteArrayOf(1, 2, 3))
        userDatabaseSystem.deleteUser(user, "password123")

        // Signing back in as a brand new account, nothing from the old one can be reached: the old uid is gone
        val newUser = signUp().value!!
        assertTrue(categoryDatabaseSystem.getAllCategoriesForUser(newUser).values!!.isEmpty())
        assertFalse(categoryDatabaseSystem.findCategory(category.id).wasSuccessful)
    }

    @Test
    fun deleteUser_forSomeoneWhoIsNotSignedIn_doesNotExist() = runBlocking {
        val first = signUp().value!!
        signUp()
        assertEquals(DeleteReturnStatus.DoesNotExist, userDatabaseSystem.deleteUser(first, "password123"))
    }
}
