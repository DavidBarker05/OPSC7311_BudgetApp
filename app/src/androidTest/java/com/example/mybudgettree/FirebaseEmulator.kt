package com.example.mybudgettree

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Connects the tests to the Firebase Emulator Suite instead of the real project. `firebase emulators:start` must be
 * running on the computer before the tests start. The emulators are reached from the Android emulator through
 * 10.0.2.2, the alias for the computer's localhost
 *
 * The emulators are wiped once at the start and once at the end of a whole test run (see [WipeEmulatorsListener]), not
 * between tests. Tests stay independent of each other by never sharing data: each one makes its own user with an email
 * and phone number from [uniqueEmail] and [nextPhoneNumber]
 */
object FirebaseEmulator {
    private const val HOST = "10.0.2.2"
    private const val AUTH_PORT = 9099
    private const val FIRESTORE_PORT = 8080

    const val PASSWORD_RESET = "PASSWORD_RESET"

    private val phoneCounter = AtomicInteger()

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
     * Deletes every account and every document in the emulators
     */
    fun wipe() {
        connect()
        val projectId = FirebaseApp.getInstance().options.projectId
        delete("http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts")
        delete("http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/(default)/documents")
    }

    /**
     * Makes an email address nobody else in the run has used
     *
     * @param prefix A readable start for the address, to make the emulator dashboard easier to follow
     */
    fun uniqueEmail(prefix: String = "user"): String = "$prefix-${UUID.randomUUID()}@example.com"

    /**
     * Makes a valid 10 digit phone number nobody else in the run has used. The counter restarts with every run, which
     * is fine because the emulators are wiped at the start of a run
     */
    fun nextPhoneNumber(): String = "08212%05d".format(phoneCounter.incrementAndGet())

    /**
     * A one-time code the Auth emulator recorded instead of sending an email
     *
     * @property email The address the code was requested for
     * @property requestType What it is for, e.g. `PASSWORD_RESET`
     * @property oobCode The code itself, which is what the link in a real email carries
     */
    data class RecordedCode(val email: String, val requestType: String, val oobCode: String)

    /**
     * Gets the codes the Auth emulator recorded for an address. The emulator sends no emails, so this is how a test can
     * tell that one would have been sent. Codes are recorded in the order they were requested
     *
     * @param email The address to look for
     * @param requestType Only codes of this kind, [PASSWORD_RESET] by default
     */
    fun recordedCodes(email: String, requestType: String = PASSWORD_RESET): List<RecordedCode> {
        connect()
        val projectId = FirebaseApp.getInstance().options.projectId
        val connection = URL("http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/oobCodes").openConnection() as HttpURLConnection
        try {
            check(connection.responseCode in 200..299) { "Could not read the recorded codes: ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val codes = JSONObject(body).optJSONArray("oobCodes") ?: return emptyList()
            return (0 until codes.length()).map { codes.getJSONObject(it) }
                .map { RecordedCode(it.optString("email"), it.optString("requestType"), it.optString("oobCode")) }
                .filter { it.email.equals(email, ignoreCase = true) && it.requestType == requestType }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Does what opening the link in a reset email and choosing a new password does, by sending the recorded code to the
     * Auth emulator along with the new password
     *
     * @param oobCode A code from [recordedCodes]
     * @param newPassword The password to set
     */
    fun resetPassword(oobCode: String, newPassword: String) {
        connect()
        val url = URL("http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:resetPassword?key=fake-api-key")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject().put("oobCode", oobCode).put("newPassword", newPassword).toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            check(connection.responseCode in 200..299) { "Could not reset the password: ${connection.responseCode}" }
        } finally {
            connection.disconnect()
        }
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
