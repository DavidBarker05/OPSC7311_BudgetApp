package com.example.mybudgettree

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL

/**
 * Connects the tests to the Firebase Emulator Suite instead of the real project. `firebase emulators:start` must be
 * running on the computer before the tests start. The emulators are reached from the Android emulator through
 * 10.0.2.2, the alias for the computer's localhost
 */
object FirebaseEmulator {
    private const val HOST = "10.0.2.2"
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080

    // Pointing at the emulators has to happen once, before Firestore does anything
    private val connected: Boolean by lazy {
        FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT)
        FirebaseFirestore.getInstance().useEmulator(HOST, FIRESTORE_PORT)
        true
    }

    /**
     * Points the shared Firebase instances at the emulators. Safe to call any number of times, but it must happen
     * before the first read or write
     */
    fun connect() {
        check(connected)
    }

    /**
     * Deletes every account and every document in the emulators, so the next test starts from nothing
     */
    fun wipe() {
        connect()
        val projectId = FirebaseApp.getInstance().options.projectId
        delete("http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts")
        delete("http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/(default)/documents")
    }

    private fun delete(url: String) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            check(connection.responseCode in 200..299) { "Could not wipe the emulator at $url: ${connection.responseCode}" }
        } finally {
            connection.disconnect()
        }
    }
}
