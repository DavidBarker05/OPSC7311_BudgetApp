package com.example.mybudgettree.database.entries

import com.google.firebase.firestore.Blob

/**
 * A user's profile photo, stored in Firestore at `users/{uid}/profilePhoto/photo`. It has a document of its own
 * rather than a field on [User] so the image bytes aren't downloaded every time the profile is read (e.g. on login)
 *
 * Firestore limits a document to 1 MiB, so the image must be shrunk and compressed (a 512px JPEG is roughly
 * 50-100 KB) before it's saved
 *
 * @property image The compressed image bytes
 */
data class UserPhoto(
    val image: Blob = Blob.fromBytes(ByteArray(0))
) {
    companion object {
        /**
         * The name of the collection holding the photo
         */
        const val COLLECTION = "profilePhoto"

        /**
         * The fixed ID of the one photo document every user can have, so a second one can never be created
         */
        const val DOCUMENT_ID = "photo"
    }
}
