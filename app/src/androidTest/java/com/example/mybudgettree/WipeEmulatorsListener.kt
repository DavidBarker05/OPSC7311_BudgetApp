package com.example.mybudgettree

import android.util.Log
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.RunListener

/**
 * Wipes both Firebase emulators once when a test run starts and once when it ends, instead of around every test. The
 * start wipe means a run that was stopped half-way (or crashed) can't leave data behind for the next one, and the end
 * wipe keeps the emulator dashboard clean afterwards
 *
 * Registered through the `listener` instrumentation argument in `app/build.gradle.kts`, so it applies to every run
 * whether it started from Android Studio, Gradle or CI
 */
class WipeEmulatorsListener : RunListener() {

    override fun testRunStarted(description: Description) {
        wipe("start")
    }

    override fun testRunFinished(result: Result) {
        wipe("end")
    }

    // A failed wipe is only logged. If the emulators aren't running, the tests themselves fail with a clearer error
    private fun wipe(moment: String) {
        try {
            FirebaseEmulator.wipe()
            Log.i(TAG, "Wiped the Firebase emulators at the $moment of the run")
        } catch (e: Exception) {
            Log.w(TAG, "Could not wipe the Firebase emulators at the $moment of the run: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "WipeEmulatorsListener"
    }
}
