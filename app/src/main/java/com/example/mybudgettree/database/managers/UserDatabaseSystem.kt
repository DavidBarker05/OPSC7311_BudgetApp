package com.example.mybudgettree.database.managers

import com.example.mybudgettree.database.daos.UserDao
import com.example.mybudgettree.database.entries.User

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
     * Creates a new user
     *
     * @param username The intended username string (Must not be blank)
     * @param password The intended password string (Must not be blank)
     * @return A [CreateUserReturnInfo] indicating what happened with the creation
     */
    suspend fun createUser(username: String, password: String): CreateUserReturnInfo {
        if (username.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Username is empty")
        if (password.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Password is empty")
        // TODO: Regex for invalid characters
        val user = User(username = username, password = password)
        userDao.insertUser(user) ?: return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Username is already in use")
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
     * Check if the user is in the database
     *
     * @param username The username to search for
     * @return True if the profile exists, false otherwise
     */
    suspend fun doesUserExist(username: String): Boolean = findUser(username).wasSuccessful

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
        val userNewUsername = user.copy(username = newUsername)
        userDao.updateUsername(user.username, newUsername)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = userNewUsername)
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
        val userNewPassword = user.copy(password = newPassword)
        userDao.updatePassword(user.username, newPassword)
        return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = userNewPassword)
    }

    /**
     * Deletes the user from the database
     *
     * @param user The [User] to delete
     * @return A status reflection from [UserDeleteReturnStatus]
     */
    suspend fun deleteUser(user: User): UserDeleteReturnStatus = if (userDao.deleteUser(user) == 1) UserDeleteReturnStatus.Deleted else UserDeleteReturnStatus.DoesNotExist
}