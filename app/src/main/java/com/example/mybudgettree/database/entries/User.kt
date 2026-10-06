package com.example.mybudgettree.database.entries

import java.time.LocalDate
import com.google.firebase.firestore.DocumentId

/**
 * A user's profile, stored in Firestore at `users/{uid}`. The account itself (email and
 * password) is owned by Firebase Authentication, so no password is stored here
 *
 * Every property has a default value so Firestore can rebuild the object from a document
 *
 * @property uid The Firebase Authentication user ID, filled in from the document ID rather than stored as a field
 * @property email The account email address, a display copy of the one held by Firebase Authentication
 * @property phoneNumber The account phone number
 * @property displayName The name shown for the user
 * @property dateOfBirth The user's date of birth as an ISO-8601 string (e.g. "2000-01-01"), since Firestore can't store a [LocalDate]; use [dateOfBirthAsLocalDate] to read it as a date
 * @property currency The user's preferred currency
 * @property profilePhoto The local path to the user's profile photo, or null if none is set; the image itself stays on the device
 */
data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val dateOfBirth: String = "",
    val currency: String = "ZAR",
    val profilePhoto: String? = null
) {
    /**
     * Parses [dateOfBirth] into a [LocalDate]
     *
     * This is a function rather than a property so Firestore's serializer doesn't write it to the database as an extra field
     *
     * @return The user's date of birth
     */
    fun dateOfBirthAsLocalDate(): LocalDate = LocalDate.parse(dateOfBirth)
}

/**
 * Marks a phone number as registered so no two accounts can share one, stored in Firestore at
 * `phoneNumbers/{phoneNumber}`. The document ID is the normalized phone number (digits and an optional
 * leading "+"), which also guarantees phone numbers are unique
 *
 * @property uid The Firebase Authentication user ID of the account that owns the phone number
 */
data class PhoneNumberLookup(val uid: String = "")
