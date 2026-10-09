package com.example.mybudgettree

import android.util.Log
import com.example.mybudgettree.database.managers.shared.DeleteReturnStatus
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "SafeDelete"

/**
 * Runs a delete from a screen without letting a failure crash the app
 *
 * The delete functions in the database systems clear the document and everything that belongs to it, and they let
 * network errors propagate instead of hiding them. Nothing above them catches those, so a dropped connection part way
 * through a delete would otherwise end the app. This turns every failure into `false`, so the screen can show a message
 * and let the user try again
 *
 * @param delete The delete to run, e.g. `{ app.categoryDatabaseSystem.deleteCategory(category) }`
 * @return True if the item is gone, whether this call deleted it or it was already missing. False if it couldn't be
 * deleted (no connection, nobody signed in, or any other failure)
 */
suspend fun safeDelete(delete: suspend () -> DeleteReturnStatus): Boolean =
    try {
        when (delete()) {
            DeleteReturnStatus.Deleted, DeleteReturnStatus.DoesNotExist -> true
            DeleteReturnStatus.ReauthenticationFailed -> false
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Delete failed: ${e.message}")
        false
    }
