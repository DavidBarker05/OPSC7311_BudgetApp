package com.example.mybudgettree.database.managers.shared

import android.util.Log

data class CreateReturnInfo<T>(
    val wasSuccessful: Boolean,
    val value: T? = null,
    val errMsg: String? = null
)

data class FindReturnInfo<T>(
    val wasSuccessful: Boolean,
    val value: T? = null,
    val errMsg: String? = null
)

data class FindAllReturnInfo<T>(
    val wasSuccessful: Boolean,
    val values: List<T>? = null,
    val errMsg: String? = null
)

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

data class UpdateReturnInfo<T>(
    val status: UpdateReturnStatus,
    val value: T? = null,
    val errMsg: String? = null
)

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

data class SaveReturnInfo<T>(
    val wasSuccessful: Boolean,
    val value: T? = null,
    val errMsg: String? = null
)

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