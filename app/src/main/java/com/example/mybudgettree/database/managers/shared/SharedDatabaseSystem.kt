package com.example.mybudgettree.database.managers.shared

import android.util.Log

/**
 * The result of creating an entity
 *
 * @property wasSuccessful Whether the entity was created
 * @property value The created entity (with its generated ID filled in), only set when [wasSuccessful]
 * @property errMsg Why the creation failed, only set when it didn't succeed
 */
data class CreateReturnInfo<T>(
    val wasSuccessful: Boolean,
    val value: T? = null,
    val errMsg: String? = null
)

/**
 * The result of looking up a single entity
 *
 * @property wasSuccessful Whether the entity was found
 * @property value The entity that was found, only set when [wasSuccessful]
 * @property errMsg Why the lookup failed or found nothing, only set when it didn't succeed
 */
data class FindReturnInfo<T>(
    val wasSuccessful: Boolean,
    val value: T? = null,
    val errMsg: String? = null
)

/**
 * The result of retrieving a list of entities
 *
 * @property wasSuccessful Whether the retrieval worked. An empty list is still a success
 * @property values The entities that were found, only set when [wasSuccessful]
 * @property errMsg Why the retrieval failed, only set when it didn't succeed
 */
data class FindAllReturnInfo<T>(
    val wasSuccessful: Boolean,
    val values: List<T>? = null,
    val errMsg: String? = null
)

/**
 * How an update ended
 */
enum class UpdateReturnStatus {
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
 * The result of updating an entity
 *
 * @property status How the update ended
 * @property value The entity as it is after the update, set for [UpdateReturnStatus.Succeeded], [UpdateReturnStatus.NoChange] and [UpdateReturnStatus.PendingVerification]
 * @property errMsg Why the update failed, only set when [status] is [UpdateReturnStatus.Failed]
 */
data class UpdateReturnInfo<T>(
    val status: UpdateReturnStatus,
    val value: T? = null,
    val errMsg: String? = null
)

/**
 * How a delete ended
 */
enum class DeleteReturnStatus {
    /**
     * The entity couldn't be deleted because it doesn't exist in the database
     */
    DoesNotExist,
    /**
     * The entity was successfully deleted
     */
    Deleted,
    /**
     * The password was wrong, or the session could not be confirmed, so nothing was deleted
     */
    ReauthenticationFailed
}

/**
 * The result of saving an entity that is created or replaced in one step (e.g. a monthly goal)
 *
 * @property wasSuccessful Whether the entity was saved
 * @property value The saved entity, only set when [wasSuccessful]
 * @property errMsg Why the save failed, only set when it didn't succeed
 */
data class SaveReturnInfo<T>(
    val wasSuccessful: Boolean,
    val value: T? = null,
    val errMsg: String? = null
)

/**
 * Logs whether an operation worked, as "Successfully <verb> <details>" (info) or "Failed <verb> <details>: <error>" (warning)
 *
 * @param tag The log tag, usually the name of the calling system
 * @param wasSuccessful Whether the operation worked
 * @param verbOnSuccess The verb to log on success (e.g. "created")
 * @param verbOnFailure The verb to log on failure (e.g. "to create")
 * @param messageDetails What the operation was done to, and for whom (e.g. "category 'abc' for user 'xyz'")
 * @param errMsg The reason it failed, only logged on failure
 */
fun logOutcome(
    tag: String,
    wasSuccessful: Boolean,
    verbOnSuccess: String,
    verbOnFailure: String,
    messageDetails: String,
    errMsg: String?
) {
    if (wasSuccessful)
        Log.i(tag, "Successfully $verbOnSuccess $messageDetails")
    else
        Log.w(tag, "Failed $verbOnFailure $messageDetails: $errMsg")
}

/**
 * Logs the outcome of an update. [UpdateReturnStatus.Succeeded] and [UpdateReturnStatus.Failed] are logged like any
 * other outcome, while [UpdateReturnStatus.NoChange] and [UpdateReturnStatus.PendingVerification] get messages of
 * their own since they're neither a plain success nor a failure
 *
 * @param tag The log tag, usually the name of the calling system
 * @param status The [UpdateReturnStatus] the update ended with
 * @param messageDetails What was updated, and for whom (e.g. "name for category 'abc'")
 * @param errMsg The reason it failed, only logged when [status] is [UpdateReturnStatus.Failed]
 */
fun logOutcome(
    tag: String,
    status: UpdateReturnStatus,
    messageDetails: String,
    errMsg: String?
) {
    when (status) {
        UpdateReturnStatus.Succeeded, UpdateReturnStatus.Failed -> {
            logOutcome(
                tag,
                wasSuccessful = status == UpdateReturnStatus.Succeeded,
                verbOnSuccess = "updated",
                verbOnFailure = "to update",
                messageDetails = messageDetails,
                errMsg = errMsg
            )
        }
        UpdateReturnStatus.NoChange -> Log.d(tag, "No change, $messageDetails is already up to date")
        UpdateReturnStatus.PendingVerification -> Log.i(tag, "Started update for $messageDetails, waiting for the user to verify it")
    }
}