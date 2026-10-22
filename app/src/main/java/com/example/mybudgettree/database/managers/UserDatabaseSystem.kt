package com.example.mybudgettree.database.managers

import android.util.Log
import android.util.Patterns
import com.example.mybudgettree.database.entries.PhoneNumberLookup
import com.example.mybudgettree.database.entries.User
import com.example.mybudgettree.database.entries.UserPhoto
import com.example.mybudgettree.database.entries.UserTree
import com.example.mybudgettree.database.managers.shared.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.toObject
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * This system manages user state, credentials validation, account discovery, updates, and removals
 *
 * Accounts (email and password) live in Firebase Authentication, and each user's profile lives in Firestore at
 * `users/{uid}`. Anything bigger that belongs to the user is stored beneath it, such as the profile photo at
 * `users/{uid}/profilePhoto/photo` and the money tree at `users/{uid}/userTree/tree`. A lookup collection,
 * `phoneNumbers/{phoneNumber}`, makes phone numbers unique. The security rules only let a signed-in user read and
 * write their own data, so these functions can only act on the user who is currently signed in
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

        // Firestore limits a document to 1 MiB, so the photo must be shrunk and compressed before it's saved
        private const val MAX_PHOTO_BYTES = 500_000

        // Everything stored under `users/{uid}`, so deleting an account can clear it. Firestore does not delete
        // subcollections when their parent is deleted, so any new subcollection must be added here
        private val USER_SUBCOLLECTIONS = listOf(
            "userTree", UserPhoto.COLLECTION, "categories", "expenses", "incomes", "savingsGoals", "savingsContributions", "monthlyGoals"
        )

        private fun normalizePhoneNumber(phoneNumber: String): String {
            val hasLeadingPlus = phoneNumber.trim().startsWith("+")
            val digitsOnly = phoneNumber.filter { it.isDigit() }
            return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
        }
    }

    /**
     * The result of asking for a password recovery email
     *
     * @property wasSuccessful Whether the request was accepted. This does NOT mean an account exists for the email,
     * see [sendPasswordRecoveryEmail]
     * @property errMsg Why the request failed, only set when it didn't succeed
     */
    data class PasswordRecoveryReturnInfo(
        val wasSuccessful: Boolean,
        val errMsg: String? = null
    )

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
     * @return A [CreateReturnInfo] indicating what happened with the creation
     */
    suspend fun createUser(
        email: String,
        password: String,
        phoneNumber: String,
        displayName: String,
        dateOfBirth: LocalDate,
        currency: String
    ): CreateReturnInfo<User> {
        val result = tryCreateUser(email, password, phoneNumber, displayName, dateOfBirth, currency)
        logOutcome(
            tag = TAG,
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
    ): CreateReturnInfo<User> {
        if (email.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        if (password.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Password is empty")
        if (phoneNumber.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Phone number is empty")
        if (displayName.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Display name is empty")
        if (currency.isBlank()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Currency is empty")
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return CreateReturnInfo(wasSuccessful = false, errMsg = "Email is not a valid email address")
        val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
        if (!PHONE_NUMBER_REGEX.matches(normalizedPhoneNumber)) return CreateReturnInfo(wasSuccessful = false, errMsg = "Phone number is not a valid phone number")
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
            if (isPhoneNumberTaken) {
                CreateReturnInfo(wasSuccessful = false, errMsg = "Phone number is already in use")
            } else {
                wasCreated = true
                // The uid is not stored as a field, so add it back for the caller
                CreateReturnInfo(wasSuccessful = true, value = profile.copy(uid = uid))
            }
        } catch (_: FirebaseAuthUserCollisionException) {
            CreateReturnInfo(wasSuccessful = false, errMsg = "Email is already in use")
        } catch (_: FirebaseAuthWeakPasswordException) {
            CreateReturnInfo(wasSuccessful = false, errMsg = "Password is too weak")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            CreateReturnInfo(wasSuccessful = false, errMsg = "Email is not a valid email address")
        } catch (_: FirebaseNetworkException) {
            CreateReturnInfo(wasSuccessful = false, errMsg = "No internet connection")
        } catch (_: FirebaseTooManyRequestsException) {
            CreateReturnInfo(wasSuccessful = false, errMsg = "Too many attempts, please try again later")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            CreateReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not create account")
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
     * @return A [FindReturnInfo] with the matching user if the credentials are valid
     */
    suspend fun login(email: String, password: String): FindReturnInfo<User> {
        val result = tryLogin(email, password)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "login",
            verbOnFailure = "login attempt",
            messageDetails = "for '$email'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryLogin(email: String, password: String): FindReturnInfo<User> {
        if (email.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        if (password.isBlank()) return FindReturnInfo(wasSuccessful = false, errMsg = "Password is empty")
        return try {
            val authUser = auth.signInWithEmailAndPassword(email.trim(), password).await().user!!
            val profile = users.document(authUser.uid).get().await().toObject<User>()
            if (profile == null) {
                auth.signOut() // an account without a profile can't be used
                FindReturnInfo(wasSuccessful = false, errMsg = "Invalid email or password")
            } else {
                FindReturnInfo(wasSuccessful = true, value = syncEmail(authUser, profile))
            }
        } catch (_: FirebaseAuthInvalidUserException) {
            FindReturnInfo(wasSuccessful = false, errMsg = "Invalid email or password")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            FindReturnInfo(wasSuccessful = false, errMsg = "Invalid email or password")
        } catch (_: FirebaseNetworkException) {
            FindReturnInfo(wasSuccessful = false, errMsg = "No internet connection")
        } catch (_: FirebaseTooManyRequestsException) {
            FindReturnInfo(wasSuccessful = false, errMsg = "Too many attempts, please try again later")
        } catch (e: CancellationException) {
            throw e // Rethrow so the generic catch below doesn't swallow the cancellation
                    // Kotlin then ends the coroutine quietly
        } catch (e: Exception) {
            FindReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not log in")
        }
    }

    /**
     * Asks Firebase Authentication to email a password reset link to the address. The user opens the link, chooses a
     * new password on a Firebase-hosted page, then logs in with it. The app never sees the new password
     *
     * Success does not tell you whether an account exists for the email. While Firebase's email enumeration
     * protection is on (the default for new projects), it reports success for unknown addresses too, and nothing is
     * sent. So screens should say that an email is sent "if an account exists" instead of confirming that one was sent,
     * otherwise they would reveal which emails have accounts
     *
     * @param email The address of the account to recover, spaces around it are not trimmed
     * @return A [PasswordRecoveryReturnInfo] that fails for an empty or invalid email, no internet connection, or too
     * many requests in a short time
     */
    suspend fun sendPasswordRecoveryEmail(email: String): PasswordRecoveryReturnInfo {
        val result = trySendPasswordRecoveryEmail(email)
        logOutcome(
            tag = TAG,
            wasSuccessful = result.wasSuccessful,
            verbOnSuccess = "send",
            verbOnFailure = "to send",
            messageDetails = "recovery email to '$email'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun trySendPasswordRecoveryEmail(email: String): PasswordRecoveryReturnInfo {
        if (email.isBlank()) return PasswordRecoveryReturnInfo(wasSuccessful = false, errMsg = "Email is empty")
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return PasswordRecoveryReturnInfo(wasSuccessful = false, errMsg = "Email is not a valid email address")
        return try {
            auth.sendPasswordResetEmail(email).await()
            PasswordRecoveryReturnInfo(wasSuccessful = true)
        } catch (_: FirebaseNetworkException) {
            PasswordRecoveryReturnInfo(wasSuccessful = false, errMsg = "No internet connection")
        } catch (_: FirebaseTooManyRequestsException) {
            PasswordRecoveryReturnInfo(wasSuccessful = false, errMsg = "Too many attempts, please try again later")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PasswordRecoveryReturnInfo(wasSuccessful = false, errMsg = e.message ?: "Could not send recovery email")
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
            users.document(uid).get().await().toObject<User>()
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
     * Updates the email for the user
     *
     * Firebase does not change the email straight away. It sends a verification link to the new address, and the
     * email only changes once the user clicks it, so this returns [UpdateReturnStatus.PendingVerification]
     * with the unchanged user. The user stays signed in meanwhile, and once they open the link Firebase signs them out
     * everywhere, so they log in with the new email. The profile is brought up to date on that [login]
     *
     * Changing the email is sensitive, and Firebase refuses it when the last sign in was a while ago. Passing the
     * user's [password] confirms it is really them first, which also avoids that refusal
     *
     * @param user The [User] being updated
     * @param newEmail The new email
     * @param password The user's current password, to confirm it is really them. When null no check is made, and a
     * session that is too old fails with a message to log in again
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateEmail(user: User, newEmail: String, password: String? = null): UpdateReturnInfo<User> {
        val result = tryUpdateEmail(user, newEmail, password)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "email for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateEmail(user: User, newEmail: String, password: String?): UpdateReturnInfo<User> {
        if (newEmail.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New email is empty")
        if (user.email == newEmail) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = user)
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Email is not a valid email address")
        val authUser = auth.currentUser
        if (authUser == null || authUser.uid != user.uid) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            if (password != null && !reauthenticate(authUser, password)) {
                return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Password is incorrect")
            }
            authUser.verifyBeforeUpdateEmail(newEmail).await()
            UpdateReturnInfo(status = UpdateReturnStatus.PendingVerification, value = user)
        } catch (_: FirebaseAuthUserCollisionException) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Email is already in use")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Email is not a valid email address")
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Please log in again before changing your email")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update email")
        }
    }

    /**
     * Updates the password for the user
     *
     * Firebase asks for a recent sign in before it will change a password. Passing the user's [currentPassword]
     * confirms it is really them first, which also avoids that. Without it, a session that is too old fails with a
     * message to log in again. Firebase never reveals the current password, so there is no "no change" result unless
     * the new password is the same as [currentPassword]
     *
     * @param user The [User] being updated
     * @param newPassword The new password
     * @param currentPassword The user's current password, to confirm it is really them. When null no check is made
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updatePassword(user: User, newPassword: String, currentPassword: String? = null): UpdateReturnInfo<User> {
        val result = tryUpdatePassword(user, newPassword, currentPassword)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "password for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdatePassword(user: User, newPassword: String, currentPassword: String?): UpdateReturnInfo<User> {
        if (newPassword.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New password is empty")
        if (currentPassword != null && currentPassword == newPassword) {
            return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New password must be different from the current one")
        }
        val authUser = auth.currentUser
        if (authUser == null || authUser.uid != user.uid) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            if (currentPassword != null && !reauthenticate(authUser, currentPassword)) {
                return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Current password is incorrect")
            }
            authUser.updatePassword(newPassword).await()
            UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = user)
        } catch (_: FirebaseAuthWeakPasswordException) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Password is too weak")
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Please log in again before changing your password")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update password")
        }
    }

    /**
     * Updates the phone number for the user
     *
     * Phone numbers are unique, so the number is only changed if nobody else has it. The old number is freed, the
     * new one claimed and the profile updated in one transaction, so a failure can't leave a number claimed twice
     * or not at all
     *
     * @param user The [User] being updated
     * @param newPhoneNumber The new phone number
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updatePhoneNumber(user: User, newPhoneNumber: String): UpdateReturnInfo<User> {
        val result = tryUpdatePhoneNumber(user, newPhoneNumber)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "phone number for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdatePhoneNumber(user: User, newPhoneNumber: String): UpdateReturnInfo<User> {
        if (newPhoneNumber.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New phone number is empty")
        val normalizedNewPhoneNumber = normalizePhoneNumber(newPhoneNumber)
        if (user.phoneNumber == normalizedNewPhoneNumber) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = user)
        if (!PHONE_NUMBER_REGEX.matches(normalizedNewPhoneNumber)) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Phone number is not a valid phone number")
        if (auth.currentUser?.uid != user.uid) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "User does not exist")
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
            if (isTaken) UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "Phone number is already in use")
            else UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = user.copy(phoneNumber = normalizedNewPhoneNumber))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update phone number")
        }
    }

    /**
     * Updates the display name for the user
     *
     * @param user The [User] being updated
     * @param newDisplayName The new display name
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateDisplayName(user: User, newDisplayName: String): UpdateReturnInfo<User> {
        val result = tryUpdateDisplayName(user, newDisplayName)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "display name for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateDisplayName(user: User, newDisplayName: String): UpdateReturnInfo<User> {
        if (newDisplayName.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New display name is empty")
        if (user.displayName == newDisplayName) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = user)
        return updateProfileField(user, "displayName", newDisplayName, user.copy(displayName = newDisplayName))
    }

    /**
     * Updates the date of birth for the user
     *
     * @param user The [User] being updated
     * @param newDateOfBirth The new date of birth
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateDateOfBirth(user: User, newDateOfBirth: LocalDate): UpdateReturnInfo<User> {
        val result = tryUpdateDateOfBirth(user, newDateOfBirth)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "date of birth for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateDateOfBirth(user: User, newDateOfBirth: LocalDate): UpdateReturnInfo<User> {
        if (user.dateOfBirth == newDateOfBirth.toString()) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = user)
        return updateProfileField(user, "dateOfBirth", newDateOfBirth.toString(), user.copy(dateOfBirth = newDateOfBirth.toString()))
    }

    /**
     * Updates the preferred currency for the user
     *
     * @param user The [User] being updated
     * @param newCurrency The new currency
     * @return An [UpdateReturnInfo] indicating what happened with the update
     */
    suspend fun updateCurrency(user: User, newCurrency: String): UpdateReturnInfo<User> {
        val result = tryUpdateCurrency(user, newCurrency)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "currency for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateCurrency(user: User, newCurrency: String): UpdateReturnInfo<User> {
        if (newCurrency.isBlank()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New currency is empty")
        if (user.currency == newCurrency) return UpdateReturnInfo(status = UpdateReturnStatus.NoChange, value = user)
        return updateProfileField(user, "currency", newCurrency, user.copy(currency = newCurrency))
    }

    /**
     * Saves or removes the user's profile photo, which is stored in its own document at
     * `users/{uid}/profilePhoto/photo` so the image isn't downloaded every time the profile is read
     *
     * @param user The [User] being updated
     * @param newPhoto The new photo as compressed image bytes (at most 500 KB), or null to remove it
     * @return An [UpdateReturnInfo] indicating what happened with the update. The [User] itself never changes
     */
    suspend fun updateProfilePhoto(user: User, newPhoto: ByteArray?): UpdateReturnInfo<User> {
        val result = tryUpdateProfilePhoto(user, newPhoto)
        logOutcome(
            tag = TAG,
            status = result.status,
            messageDetails = "profile photo for user '${user.uid}'",
            errMsg = result.errMsg
        )
        return result
    }

    private suspend fun tryUpdateProfilePhoto(user: User, newPhoto: ByteArray?): UpdateReturnInfo<User> {
        if (newPhoto != null && newPhoto.isEmpty()) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New profile photo is empty")
        if (newPhoto != null && newPhoto.size > MAX_PHOTO_BYTES) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "New profile photo is too large")
        if (auth.currentUser?.uid != user.uid) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            val photoRef = users.document(user.uid).collection(UserPhoto.COLLECTION).document(UserPhoto.DOCUMENT_ID)
            if (newPhoto == null) photoRef.delete().await()
            else photoRef.set(UserPhoto(image = Blob.fromBytes(newPhoto))).await()
            UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update profile photo")
        }
    }

    /**
     * Finds the user's profile photo if they have one
     *
     * @param user The [User] to find the photo for
     * @return The compressed image bytes, or null if the user has no photo. Network failures propagate to the caller
     */
    suspend fun findProfilePhoto(user: User): ByteArray? =
        users.document(user.uid).collection(UserPhoto.COLLECTION).document(UserPhoto.DOCUMENT_ID)
            .get().await().toObject<UserPhoto>()?.image?.toBytes()

    /**
     * Deletes the user's account and all of their data
     *
     * Deleting an account is sensitive, so the password is checked again first. The data is deleted before the
     * account, because once the account is gone the user is signed out and the security rules would no longer let
     * their data be removed. Firestore does not remove a document's subcollections along with it, so each one in
     * [USER_SUBCOLLECTIONS] is cleared explicitly. The phone number lookup is removed too, so the number can be
     * used by someone else
     *
     * @param user The [User] to delete
     * @param password The user's current password, to confirm it is really them
     * @return A status reflection from [DeleteReturnStatus]
     */
    suspend fun deleteUser(user: User, password: String): DeleteReturnStatus {
        val result = tryDeleteUser(user, password)
        val wasSuccessful = result == DeleteReturnStatus.Deleted
        val errMsg: String? =
            if (wasSuccessful) null
            else when (result) {
                DeleteReturnStatus.DoesNotExist -> "User does not exist"
                DeleteReturnStatus.ReauthenticationFailed -> "Could not confirm the password"
                else -> "Unknown reason"
            }
        logOutcome(
            tag = TAG,
            wasSuccessful = wasSuccessful,
            verbOnSuccess = "deleted",
            verbOnFailure = "to delete",
            messageDetails = "user '${user.uid}'",
            errMsg = errMsg
        )
        return result
    }

    private suspend fun tryDeleteUser(user: User, password: String): DeleteReturnStatus {
        val authUser = auth.currentUser
        val email = authUser?.email
        if (authUser == null || email == null || authUser.uid != user.uid) return DeleteReturnStatus.DoesNotExist
        try {
            authUser.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return DeleteReturnStatus.ReauthenticationFailed
        }
        for (name in USER_SUBCOLLECTIONS) deleteCollection(users.document(user.uid).collection(name))
        db.runBatch { batch ->
            batch.delete(phoneNumbers.document(user.phoneNumber))
            batch.delete(users.document(user.uid))
        }.await()
        authUser.delete().await()
        return DeleteReturnStatus.Deleted
    }

    /**
     * Confirms the signed-in user's password, which also counts as a fresh sign in for the changes Firebase only
     * allows after one
     *
     * @param authUser The signed-in Firebase user
     * @param password The password to check
     * @return True if the password is right, false if it is wrong. Other failures, like having no connection, are thrown
     */
    private suspend fun reauthenticate(authUser: FirebaseUser, password: String): Boolean {
        val email = authUser.email ?: return false
        return try {
            authUser.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
            true
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            false
        } catch (_: FirebaseAuthInvalidUserException) {
            false
        }
    }

    private suspend fun updateProfileField(user: User, field: String, value: Any?, updatedUser: User): UpdateReturnInfo<User> {
        if (auth.currentUser?.uid != user.uid) return UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "User does not exist")
        return try {
            users.document(user.uid).update(field, value).await()
            UpdateReturnInfo(status = UpdateReturnStatus.Succeeded, value = updatedUser)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) UpdateReturnInfo(status = UpdateReturnStatus.Failed, errMsg = "User does not exist")
            else UpdateReturnInfo(UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update $field")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateReturnInfo(UpdateReturnStatus.Failed, errMsg = e.message ?: "Could not update $field")
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
