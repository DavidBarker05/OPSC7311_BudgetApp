package com.example.mybudgettree.database.managers

import android.util.Log
import android.util.Patterns
import com.example.mybudgettree.database.entries.PhoneNumberLookup
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserTree
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import java.time.LocalDate
import java.time.YearMonth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * This system manages user state, credentials validation, account discovery, updates, and removals
 *
 * Accounts (email and password) live in Firebase Authentication, and each user's profile lives in Firestore at
 * `users/{uid}`. A lookup collection, `phoneNumbers/{phoneNumber}`, makes phone numbers unique. The security
 * rules only let a signed-in user read and write their own profile, so these functions can only act on the user
 * who is currently signed in
 *
 * @property auth The Firebase Authentication instance that owns the accounts
 * @property db The Firestore instance that holds the profiles and lookups
 */
class UserDatabaseSystem(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val users get() = db.collection("users")
    private val phoneNumbers get() = db.collection("phoneNumbers")

    companion object {
        private const val TAG = "UserDatabaseSystem"
        private val PHONE_NUMBER_REGEX = Regex("^\\+?[0-9]{7,15}$")
        private const val BATCH_SIZE = 400L

        // Everything stored under `users/{uid}`, so deleting an account can clear it. Firestore does not delete
        // subcollections when their parent is deleted, so any new subcollection must be added here
        private val USER_SUBCOLLECTIONS = listOf(
            "userTree", "categories", "expenses", "incomes", "savingsGoals", "savingsContributions", "monthlyGoals"
        )

        private fun normalizePhoneNumber(phoneNumber: String): String {
            val hasLeadingPlus = phoneNumber.trim().startsWith("+")
            val digitsOnly = phoneNumber.filter { it.isDigit() }
            return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
        }

        private fun logOutcome(
            wasSuccessful: Boolean,
            verbOnSuccess: String,
            verbOnFailure: String,
            messageDetails: String,
            errMsg: String?
        ) {
            if (wasSuccessful)
                Log.i(TAG, "Successfully $verbOnSuccess $messageDetails")
            else
                Log.w(TAG, "Failed $verbOnFailure $messageDetails: $errMsg")
        }

        private fun logOutcome(
            status: UpdateUserReturnStatus,
            messageDetails: String,
            errMsg: String?
        ) {
            when (status) {
                UpdateUserReturnStatus.Succeeded, UpdateUserReturnStatus.Failed -> {
                    logOutcome(
                        wasSuccessful = status == UpdateUserReturnStatus.Succeeded,
                        verbOnSuccess = "updated",
                        verbOnFailure = "to update",
                        messageDetails = messageDetails,
                        errMsg = errMsg
                    )
                }
                UpdateUserReturnStatus.NoChange -> Log.d(TAG, "No change, $messageDetails is already up to date")
                UpdateUserReturnStatus.PendingVerification -> Log.i(TAG, "Started update for $messageDetails, waiting for the user to verify it")
            }
        }
    }

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
        Succeeded,
        /**
         * The update was started but only takes effect once the user confirms it from a link sent to them
         */
        PendingVerification
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
        Deleted,
        /**
         * The password was wrong, or the session could not be confirmed, so nothing was deleted
         */
        ReauthenticationFailed
    }

    /**
     * Creates a new Firebase Authentication account and its Firestore profile after validating the fields and
     * confirming the phone number is not already in use
     *
     * The profile, phone number lookup and money tree are all written in one transaction, so either all of them
     * exist or none do. If anything fails after the account was created, the account is deleted again so no
     * half-created user is left behind
     *
     * @param email The account email address, must be unique (enforced by Firebase Authentication)
     * @param password The account password, must satisfy Firebase's password policy
     * @param phoneNumber The account phone number, must be unique
     * @param displayName The name shown for the user
     * @param dateOfBirth The user's date of birth
     * @param currency The user's preferred currency
     * @return A [CreateUserReturnInfo] indicating what happened with the creation
     */
    suspend fun createUser(
        email: String,
        password: String,
        phoneNumber: String,
        displayName: String,
        dateOfBirth: LocalDate,
        currency: String
    ): CreateUserReturnInfo {
        val result = tryCreateUser(email, password, phoneNumber, displayName, dateOfBirth, currency)
        logOutcome(
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "created",
            verbOnFailure = "to create",
            messageDetails = "'$email'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryCreateUser(
        email: String,
        password: String,
        phoneNumber: String,
        displayName: String,
        dateOfBirth: LocalDate,
        currency: String
    ): CreateUserReturnInfo {
        if (email.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        if (password.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Password is empty")
        if (phoneNumber.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Phone number is empty")
        if (displayName.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Display name is empty")
        if (currency.isBlank()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Currency is empty")
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Email is not a valid email address")
        val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
        if (!PHONE_NUMBER_REGEX.matches(normalizedPhoneNumber)) return CreateUserReturnInfo(wasSuccessful = false, errMsg = "Phone number is not a valid phone number")
        var createdAuthUser: FirebaseUser? = null
        var wasCreated = false
        return try {
            val authUser = auth.createUserWithEmailAndPassword(email, password).await().user!!
            createdAuthUser = authUser
            val uid = authUser.uid
            val userRef = users.document(uid)
            val phoneRef = phoneNumbers.document(normalizedPhoneNumber)
            val profile = User(
                email = email,
                phoneNumber = normalizedPhoneNumber,
                displayName = displayName,
                dateOfBirth = dateOfBirth.toString(),
                currency = currency
            )
            // A transaction needs every read before any write, so the phone number is checked first
            val isPhoneNumberTaken = db.runTransaction { tx ->
                if (tx.get(phoneRef).exists()) {
                    true
                } else {
                    tx.set(phoneRef, PhoneNumberLookup(uid = uid))
                    tx.set(userRef, profile)
                    tx.set(
                        userRef.collection("userTree").document(UserTree.DOCUMENT_ID),
                        UserTree(yearMonth = YearMonth.now().toString())
                    )
                    false
                }
            }.await()
            if (isPhoneNumberTaken) CreateUserReturnInfo(wasSuccessful = false, errMsg = "Phone number is already in use")
            wasCreated = true
            // The uid is not stored as a field, so add it back for the caller
            CreateUserReturnInfo(wasSuccessful = true, user = profile.copy(uid = uid))
        } catch (_: FirebaseAuthUserCollisionException) {
            CreateUserReturnInfo(wasSuccessful = false, errMsg = "Email is already in use")
        } catch (_: FirebaseAuthWeakPasswordException) {
            CreateUserReturnInfo(wasSuccessful = false, errMsg = "Password is too weak")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            CreateUserReturnInfo(wasSuccessful = false, errMsg = "Email is not a valid email address")
        } catch (_: FirebaseNetworkException) {
            CreateUserReturnInfo(wasSuccessful = false, errMsg = "No internet connection")
        } catch (_: FirebaseTooManyRequestsException) {
            CreateUserReturnInfo(wasSuccessful = false, errMsg = "Too many attempts, please try again later")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            CreateUserReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create account")
        } finally {
            // Roll back the half-created account. NonCancellable so this still runs if the screen was closed
            // mid-signup, and runCatching so a failed delete can't hide the original error
            if (!wasCreated) {
                createdAuthUser?.let { orphan ->
                    withContext(NonCancellable) { runCatching { orphan.delete().await() } }
                }
            }
        }
    }

    /**
     * Signs in with an email and checks the password
     *
     * Every credential failure gives the same message so the screen never reveals whether an email has an account
     *
     * @param email The email entered on the login screen
     * @param password The password to check
     * @return A [FindUserReturnInfo] with the matching user if the credentials are valid
     */
    suspend fun login(email: String, password: String): FindUserReturnInfo {
        val result = tryLogin(email, password)
        logOutcome(
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "login",
            verbOnFailure = "login attempt",
            messageDetails = "for '$email'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryLogin(email: String, password: String): FindUserReturnInfo {
        if (email.isBlank()) return FindUserReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        if (password.isBlank()) return FindUserReturnInfo(wasSuccessful = false, errMsg = "Password is empty")
        return try {
            val authUser = auth.signInWithEmailAndPassword(email.trim(), password).await().user!!
            val profile = users.document(authUser.uid).get().await().toObject(User::class.java)
            if (profile == null) {
                auth.signOut() // an account without a profile can't be used
                FindUserReturnInfo(wasSuccessful = false, errMsg = "Invalid email or password")
            } else {
                FindUserReturnInfo(wasSuccessful = true, user = syncEmail(authUser, profile))
            }
        } catch (_: FirebaseAuthInvalidUserException) {
            FindUserReturnInfo(wasSuccessful = false, errMsg = "Invalid email or password")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            FindUserReturnInfo(wasSuccessful = false, errMsg = "Invalid email or password")
        } catch (_: FirebaseNetworkException) {
            FindUserReturnInfo(wasSuccessful = false, errMsg = "No internet connection")
        } catch (_: FirebaseTooManyRequestsException) {
            FindUserReturnInfo(wasSuccessful = false, errMsg = "Too many attempts, please try again later")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            FindUserReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not log in")
        }
    }

    /**
     * Gets the profile of whoever is currently signed in, which is how a login is remembered after the app restarts
     *
     * @return The signed-in [User], or null if nobody is signed in or their profile is missing
     */
    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            users.document(uid).get().await().toObject(User::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load the current user: ${e.message}")
            null
        }
    }

    /**
     * Signs the current user out
     */
    fun logout() {
        auth.signOut()
        Log.i(TAG, "Signed out")
    }

    /**
     * Check if the phone number is already in use by a user
     *
     * Only works while a user is signed in, since the security rules don't let signed-out users read phone numbers
     *
     * @param phoneNumber The phone number to search for
     * @return True if the phone number is in use, false otherwise
     */
    suspend fun isPhoneNumberInUse(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        return try {
            phoneNumbers.document(normalizePhoneNumber(phoneNumber)).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the user still exists in the database
     *
     * @param user The [User] to validate
     * @return True if the user still exists, false otherwise
     */
    suspend fun isUserValid(user: User): Boolean {
        if (auth.currentUser?.uid != user.uid) return false
        return try {
            users.document(user.uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates the email for the user
     *
     * Firebase does not change the email straight away. It sends a verification link to the new address, and the
     * email only changes once the user clicks it, so this returns [UpdateUserReturnStatus.PendingVerification]
     * with the unchanged user. The profile is brought up to date on the next [login]
     *
     * @param user The [User] being updated
     * @param newEmail The new email
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateEmail(user: User, newEmail: String): UpdateUserReturnInfo {
        val result = tryUpdateEmail(user, newEmail)
        logOutcome(
            status = result.status,
            messageDetails = "email for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateEmail(user: User, newEmail: String): UpdateUserReturnInfo {
        if (newEmail.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New email is empty")
        if (user.email == newEmail) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Email is not a valid email address")
        val authUser = auth.currentUser
        if (authUser == null || authUser.uid != user.uid) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            authUser.verifyBeforeUpdateEmail(newEmail).await()
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.PendingVerification, user = user)
        } catch (_: FirebaseAuthUserCollisionException) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Email is already in use")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Email is not a valid email address")
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Please log in again before changing your email")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = e.message ?: "Could not update email")
        }
    }

    /**
     * Updates the password for the user
     *
     * Firebase asks for a recent sign in before it will change a password, so this fails with a message to log in
     * again if the session is too old. Firebase never reveals the current password, so unlike before there is no
     * "no change" result
     *
     * @param user The [User] being updated
     * @param newPassword The new password
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updatePassword(user: User, newPassword: String): UpdateUserReturnInfo {
        val result = tryUpdatePassword(user, newPassword)
        logOutcome(
            status = result.status,
            messageDetails = "password for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdatePassword(user: User, newPassword: String): UpdateUserReturnInfo {
        if (newPassword.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New password is empty")
        val authUser = auth.currentUser
        if (authUser == null || authUser.uid != user.uid) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            authUser.updatePassword(newPassword).await()
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user)
        } catch (_: FirebaseAuthWeakPasswordException) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Password is too weak")
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Please log in again before changing your password")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = e.message ?: "Could not update password")
        }
    }

    /**
     * Updates the phone number for the user
     *
     * @param user The [User] being updated
     * @param newPhoneNumber The new phone number
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updatePhoneNumber(user: User, newPhoneNumber: String): UpdateUserReturnInfo {
        val result = tryUpdatePhoneNumber(user, newPhoneNumber)
        logOutcome(
            status = result.status,
            messageDetails = "phone number for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdatePhoneNumber(user: User, newPhoneNumber: String): UpdateUserReturnInfo {
        if (newPhoneNumber.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New phone number is empty")
        val normalizedNewPhoneNumber = normalizePhoneNumber(newPhoneNumber)
        if (user.phoneNumber == normalizedNewPhoneNumber) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        if (!PHONE_NUMBER_REGEX.matches(normalizedNewPhoneNumber)) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Phone number is not a valid phone number")
        if (auth.currentUser?.uid != user.uid) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            val oldPhoneRef = phoneNumbers.document(user.phoneNumber)
            val newPhoneRef = phoneNumbers.document(normalizedNewPhoneNumber)
            val userRef = users.document(user.uid)
            val isTaken = db.runTransaction { tx ->
                if (tx.get(newPhoneRef).exists()) {
                    true
                } else {
                    tx.delete(oldPhoneRef)
                    tx.set(newPhoneRef, PhoneNumberLookup(uid = user.uid))
                    tx.update(userRef, "phoneNumber", normalizedNewPhoneNumber)
                    false
                }
            }.await()
            if (isTaken) UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "Phone number is already in use")
            else UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = user.copy(phoneNumber = normalizedNewPhoneNumber))
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = e.message ?: "Could not update phone number")
        }
    }

    /**
     * Updates the display name for the user
     *
     * @param user The [User] being updated
     * @param newDisplayName The new display name
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateDisplayName(user: User, newDisplayName: String): UpdateUserReturnInfo {
        val result = tryUpdateDisplayName(user, newDisplayName)
        logOutcome(
            status = result.status,
            messageDetails = "display name for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateDisplayName(user: User, newDisplayName: String): UpdateUserReturnInfo {
        if (newDisplayName.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New display name is empty")
        if (user.displayName == newDisplayName) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        return updateProfileField(user, "displayName", newDisplayName, user.copy(displayName = newDisplayName))
    }

    /**
     * Updates the date of birth for the user
     *
     * @param user The [User] being updated
     * @param newDateOfBirth The new date of birth
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateDateOfBirth(user: User, newDateOfBirth: LocalDate): UpdateUserReturnInfo {
        val result = tryUpdateDateOfBirth(user, newDateOfBirth)
        logOutcome(
            status = result.status,
            messageDetails = "date of birth for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateDateOfBirth(user: User, newDateOfBirth: LocalDate): UpdateUserReturnInfo {
        if (user.dateOfBirth == newDateOfBirth.toString()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        return updateProfileField(user, "dateOfBirth", newDateOfBirth.toString(), user.copy(dateOfBirth = newDateOfBirth.toString()))
    }

    /**
     * Updates the preferred currency for the user
     *
     * @param user The [User] being updated
     * @param newCurrency The new currency
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateCurrency(user: User, newCurrency: String): UpdateUserReturnInfo {
        val result = tryUpdateCurrency(user, newCurrency)
        logOutcome(
            status = result.status,
            messageDetails = "currency for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateCurrency(user: User, newCurrency: String): UpdateUserReturnInfo {
        if (newCurrency.isBlank()) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New currency is empty")
        if (user.currency == newCurrency) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        return updateProfileField(user, "currency", newCurrency, user.copy(currency = newCurrency))
    }

    /**
     * Updates the profile photo path for the user
     *
     * @param user The [User] being updated
     * @param newProfilePhotoPath The new profile photo path, or null to remove it
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    suspend fun updateProfilePhoto(user: User, newProfilePhotoPath: String?): UpdateUserReturnInfo {
        val result = tryUpdateProfilePhoto(user, newProfilePhotoPath)
        logOutcome(
            status = result.status,
            messageDetails = "profile photo for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateProfilePhoto(user: User, newProfilePhotoPath: String?): UpdateUserReturnInfo {
        if (newProfilePhotoPath?.isBlank() ?: false) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "New profile photo path is empty")
        if (user.profilePhoto == newProfilePhotoPath) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.NoChange, user = user)
        return updateProfileField(user, "profilePhoto", newProfilePhotoPath, user.copy(profilePhoto = newProfilePhotoPath))
    }

    /**
     * Deletes the user's account and all of their data
     *
     * Deleting an account is sensitive, so the password is checked again first. The data is deleted before the
     * account, because once the account is gone the user is signed out and the security rules would no longer let
     * their data be removed. Firestore does not remove a document's subcollections along with it, so each one in
     * [USER_SUBCOLLECTIONS] is cleared explicitly
     *
     * @param user The [User] to delete
     * @param password The user's current password, to confirm it is really them
     * @return A status reflection from [UserDeleteReturnStatus]
     */
    suspend fun deleteUser(user: User, password: String): UserDeleteReturnStatus {
        val result = tryDeleteUser(user, password)
        val wasSuccessful = result == UserDeleteReturnStatus.Deleted
        val errMsg: String? =
            if (wasSuccessful) null
            else when (result) {
                UserDeleteReturnStatus.DoesNotExist -> "user does not exist"
                UserDeleteReturnStatus.ReauthenticationFailed -> "could not confirm the password"
                else -> "unknown reason"
            }
        logOutcome(
            wasSuccessful = wasSuccessful,
            verbOnSuccess = "deleted",
            verbOnFailure = "to delete",
            messageDetails = "user '${user.uid}'",
            errMsg = errMsg
        )
        return result
    }

    private suspend fun tryDeleteUser(user: User, password: String): UserDeleteReturnStatus {
        val authUser = auth.currentUser
        val email = authUser?.email
        if (authUser == null || email == null || authUser.uid != user.uid) return UserDeleteReturnStatus.DoesNotExist
        try {
            authUser.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (_: Exception) {
            return UserDeleteReturnStatus.ReauthenticationFailed
        }
        for (name in USER_SUBCOLLECTIONS) deleteCollection(users.document(user.uid).collection(name))
        db.runBatch { batch ->
            batch.delete(phoneNumbers.document(user.phoneNumber))
            batch.delete(users.document(user.uid))
        }.await()
        authUser.delete().await()
        return UserDeleteReturnStatus.Deleted
    }

    /**
     * Writes one field of the signed-in user's profile
     *
     * @param user The [User] being updated
     * @param field The name of the profile field to write
     * @param value The new value for the field
     * @param updatedUser The [User] to return if the write succeeds
     * @return An [UpdateUserReturnInfo] indicating what happened with the update
     */
    private suspend fun updateProfileField(user: User, field: String, value: Any?, updatedUser: User): UpdateUserReturnInfo {
        if (auth.currentUser?.uid != user.uid) return UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            users.document(user.uid).update(field, value).await()
            UpdateUserReturnInfo(status = UpdateUserReturnStatus.Succeeded, user = updatedUser)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) UpdateUserReturnInfo(status = UpdateUserReturnStatus.Failed, errMsg = "User does not exist")
            else UpdateUserReturnInfo(UpdateUserReturnStatus.Failed, errMsg = e.message ?: "Could not update $field")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            UpdateUserReturnInfo(UpdateUserReturnStatus.Failed, errMsg = e.message ?: "Could not update $field")
        }
    }

    /**
     * Copies the email Firebase Authentication holds into the profile if they differ, which happens after the user
     * confirms an email change. Doing it on login is the only moment the app knows about it. Failing here is not
     * fatal, so the profile is returned unchanged
     */
    private suspend fun syncEmail(authUser: FirebaseUser, profile: User): User {
        val authEmail = authUser.email ?: return profile
        if (authEmail == profile.email) return profile
        return try {
            users.document(profile.uid).update("email", authEmail).await()
            profile.copy(email = authEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync the email for user '${profile.uid}': ${e.message}")
            profile
        }
    }

    /**
     * Deletes every document in a collection, a batch at a time, since Firestore can't delete a collection in one call
     */
    private suspend fun deleteCollection(collection: CollectionReference) {
        while (true) {
            val documents = collection.limit(BATCH_SIZE).get().await().documents
            if (documents.isEmpty()) return
            db.runBatch { batch -> documents.forEach { batch.delete(it.reference) } }.await()
        }
    }
}
