package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.UserDao
import com.example.mybudgettree.database.entries.User
import java.time.LocalDate

/**
 * This system manages user state, credentials validation, account discovery, updates, and removals
 *
 * @property userDao The underlying Data Access Object managing RoomDB operations
 */
class UserDatabaseSystem(private val userDao: UserDao) {

    /**
     * Wraps the update creation return in a detailed form
     *
     * @property wasSuccessful True if the user profile was created without error, false otherwise
     * @property user The newly created [User] record if successful, or null on execution failure
     * @property errMsg The explanatory message detailing why creation failed, or null if successful
     */
    data class CreateUserReturnInfo(
        val wasSuccessful: Boolean,
        val user: User? = null,
        val errMsg: String? = null
    )

    /**
     * Wraps the search request return in a detailed form
     *
     * @property wasSuccessful True if the target record was found, false otherwise
     * @property user The retrieved [User] entity if located, or null if the record doesn't exist
     * @property errMsg The diagnostic message stating the cause of failure, or null if found
     */
    data class FindUserReturnInfo(
        val wasSuccessful: Boolean,
        val user: User? = null,
        val errMsg: String? = null
    )

    /**
     * Identifies exactly what happened when updating a user
     */
    enum class UpdateUserReturnStatus {
        /**
         * The update failed
         */
        Failed,
        /**
         * The update did not change any data, but did not fail
         */
        NoChange,
        /**
         * The update successfully changed data
         */
        Succeeded
    }

    /**
     * Wraps the update request return in a detailed form
     *
     * @property status A [UpdateUserReturnStatus] specifying the operation outcome
     * @property user The modified [User] profile containing updated fields, or null if the task failed
     * @property errMsg The error message, only set if the [status] is [UpdateUserReturnStatus.Failed]
     */
    data class UpdateUserReturnInfo(
        val status: UpdateUserReturnStatus,
        val user: User? = null,
        val errMsg: String? = null
    )

    /**
     * Indicates what happened when trying to delete a user
     */
    enum class UserDeleteReturnStatus {
        /**
         * The user couldn't be deleted because it doesn't exist in the database
         */
        DoesNotExist,
        /**
         * The user was successfully deleted
         */
        Deleted
    }

    /**
     * Creates a new user profile after validating the fields and confirming none of the unique fields are already in use
     *
     * @param username The desired username, must be unique
     * @param password The account password
     * @param email The account email address, must be unique
     * @param phoneNumber The account phone number, must be unique
     * @param displayName The name shown for the user
     * @param dateOfBirth The user's date of birth
     * @param currency The user's preferred currency
     * @param profilePhotoPath The path to the user's profile photo, or null if none is set
     * @return A [CreateUserReturnInfo] indicating what happened with the creation
     */
    suspend fun createUser(
        username: String,
        password: String,
        email: String,
        phoneNumber: String,
        displayName: String,
        dateOfBirth: LocalDate,
        currency: String,
        profilePhotoPath: String?
    ): CreateUserReturnInfo {
        if (username.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Username is empty")
        if (password.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Password is empty")
        if (email.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        if (phoneNumber.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Phone number is empty")
        if (displayName.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Display name is empty")
        if (currency.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Currency is empty")
        if (profilePhotoPath?.isBlank() ?: false) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Profile photo path is empty")
        // TODO: Regex for invalid characters
        if (userDao.findUser(username) != null) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Username is already in use")
        if (userDao.findUserByEmail(email) != null) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Email is already in use")
        if (userDao.findUserByPhoneNumber(phoneNumber) != null) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Phone number is already in use")
        val user = User(
            username = username,
            password = password,
            email = email,
            phoneNumber = phoneNumber,
            displayName = displayName,
            dateOfBirth = dateOfBirth,
            currency = currency,
            profilePhotoPath = profilePhotoPath
        )
        userDao.insertUser(user) // Already checked all details not in use so safe to not check after insert
        return CreateUserReturnInfo(wasSuccessful = true, user = user)
    }

    /**
     * Find the user in the database if they exist
     *
     * @param username The username to search for
     * @return A [FindUserReturnInfo] indicating what happened with the search
     */
    suspend fun findUser(username: String): FindUserReturnInfo {
        if (username.isBlank()) return FindUserReturnInfo(wasSuccessful = false, errMsg = "Username is empty")
        val foundUser = userDao.findUser(username)
        return if (foundUser != null) FindUserReturnInfo(wasSuccessful = true, user = foundUser)
        else FindUserReturnInfo(wasSuccessful = false, errMsg = "No user with username = \"$username\" found")
    }

    /**
     * Find the user in the database by their email
     *
     * @param email The email to search for
     * @return A [FindUserReturnInfo] indicating what happened with the search
     */
    suspend fun findUserByEmail(email: String): FindUserReturnInfo {
        if (email.isBlank()) return FindUserReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        val foundUser = userDao.findUserByEmail(email)
        return if (foundUser != null) FindUserReturnInfo(wasSuccessful = true, user = foundUser)
        else FindUserReturnInfo(wasSuccessful = false, errMsg = "No user with email = \"$email\" found")
    }

    /**
     * Find the user in the database by their phone number
     *
     * @param phoneNumber The phone number to search for
     * @return A [FindUserReturnInfo] indicating what happened with the search
     */
    suspend fun findUserByPhoneNumber(phoneNumber: String): FindUserReturnInfo {
        if (phoneNumber.isBlank()) return FindUserReturnInfo(wasSuccessful = false, errMsg = "Phone number is empty")
        val foundUser = userDao.findUserByPhoneNumber(phoneNumber)
        return if (foundUser != null) FindUserReturnInfo(wasSuccessful = true, user = foundUser)
        else FindUserReturnInfo(wasSuccessful = false, errMsg = "No user with phone number = \"$phoneNumber\" found")
    }

    /**
     * Check if the user is in the database
     *
     * @param username The username to search for
     * @return True if the profile exists, false otherwise
     */
    suspend fun doesUserExist(username: String): Boolean = findUser(username).wasSuccessful

    /**
     * Check if the email is already in use by a user
     *
     * @param email The email to search for
     * @return True if the email is in use, false otherwise
     */
    suspend fun isEmailInUse(email: String): Boolean = findUserByEmail(email).wasSuccessful

    /**
     * Check if the phone number is already in use by a user
     *
     * @param phoneNumber The phone number to search for
     * @return True if the phone number is in use, false otherwise
     */
    suspend fun isPhoneNumberInUse(phoneNumber: String): Boolean = findUserByPhoneNumber(phoneNumber).wasSuccessful

    /**
     * Check if the user still exists in the database
     *
     * @param user The [User] to validate
     * @return True if the user still exists, false otherwise
     */
    suspend fun isUserValid(user: User): Boolean = findUser(user.username).wasSuccessful

    /**
     * Modifies the username for the user
     *
     * @param user The [User] being updated
     * @param newUsername The new username
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateUsername(user: User, newUsername: String): UpdateUserReturnInfo {
        if (newUsername.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New username is empty")
        if (user.username == newUsername) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        if (doesUserExist(newUsername)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Username is already in use")
        userDao.updateUsername(user.username, newUsername)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(username = newUsername))
    }

    /**
     * Updates the password for the user
     *
     * @param user The [User] being updated
     * @param newPassword The new password
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updatePassword(user: User, newPassword: String): UpdateUserReturnInfo {
        if (newPassword.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New password is empty")
        if (user.password == newPassword) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        userDao.updatePassword(user.username, newPassword)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(password = newPassword))
    }

    /**
     * Updates the email for the user
     *
     * @param user The [User] being updated
     * @param newEmail The new email
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateEmail(user: User, newEmail: String): UpdateUserReturnInfo {
        if (newEmail.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New email is empty")
        if (user.email == newEmail) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        if (isEmailInUse(newEmail)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Email is already in use")
        userDao.updateEmail(user.username, newEmail)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(email = newEmail))
    }

    /**
     * Updates the phone number for the user
     *
     * @param user The [User] being updated
     * @param newPhoneNumber The new phone number
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updatePhoneNumber(user: User, newPhoneNumber: String): UpdateUserReturnInfo {
        if (newPhoneNumber.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New phone number is empty")
        if (user.phoneNumber == newPhoneNumber) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        if (isPhoneNumberInUse(newPhoneNumber)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Phone number is already in use")
        userDao.updatePhoneNumber(user.username, newPhoneNumber)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(phoneNumber = newPhoneNumber))
    }

    /**
     * Updates the display name for the user
     *
     * @param user The [User] being updated
     * @param newDisplayName The new display name
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateDisplayName(user: User, newDisplayName: String): UpdateUserReturnInfo {
        if (newDisplayName.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New display name is empty")
        if (user.password == newDisplayName) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        userDao.updateDisplayName(user.username, newDisplayName)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(displayName = newDisplayName))
    }

    /**
     * Updates the date of birth for the user
     *
     * @param user The [User] being updated
     * @param newDateOfBirth The new date of birth
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateDateOfBirth(user: User, newDateOfBirth: LocalDate): UpdateUserReturnInfo {
        if (user.dateOfBirth == newDateOfBirth) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        userDao.updateDateOfBirth(user.username, newDateOfBirth)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(dateOfBirth = newDateOfBirth))
    }

    /**
     * Updates the preferred currency for the user
     *
     * @param user The [User] being updated
     * @param newCurrency The new currency
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateCurrency(user: User, newCurrency: String): UpdateUserReturnInfo {
        if (newCurrency.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New display name is empty")
        if (user.currency == newCurrency) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        userDao.updateCurrency(user.username, newCurrency)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(currency = newCurrency))
    }

    /**
     * Updates the profile photo path for the user
     *
     * @param user The [User] being updated
     * @param newProfilePhotoPath The new profile photo path, or null to remove it
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateProfilePhoto(user: User, newProfilePhotoPath: String?): UpdateUserReturnInfo {
        if (newProfilePhotoPath?.isBlank() ?: false) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New profile photo path is empty")
        if (user.profilePhotoPath == newProfilePhotoPath) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!doesUserExist(user.username)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        userDao.updateProfilePhoto(user.username, newProfilePhotoPath)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(profilePhotoPath = newProfilePhotoPath))
    }

    /**
     * Deletes the user from the database
     *
     * @param user The [User] to delete
     * @return A status reflection from [UserDeleteReturnStatus]
     */
    suspend fun deleteUser(user: User): UserDeleteReturnStatus = if (userDao.deleteUser(user) == 1) UserDeleteReturnStatus.Deleted else UserDeleteReturnStatus.DoesNotExist
}