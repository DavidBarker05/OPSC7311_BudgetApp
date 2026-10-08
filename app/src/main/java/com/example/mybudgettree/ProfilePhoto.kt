package com.example.mybudgettree

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.example.mybudgettree.database.entries.User
import java.io.ByteArrayOutputStream
import kotlin.coroutines.cancellation.CancellationException

object ProfilePhoto {
    /**
     * Shows the user's profile photo in the image view, or the default profile icon if they have none
     *
     * @param imageView The view to show the photo in
     * @param photo The compressed image bytes (see UserDatabaseSystem.findProfilePhoto), or null if there is no photo
     */
    fun bind(imageView: ImageView, photo: ByteArray?) {
        val bitmap = photo?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setPadding(0, 0, 0, 0)
        } else {
            val padding = (28 * imageView.resources.displayMetrics.density).toInt()
            imageView.setImageResource(R.drawable.ic_nav_profile)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setPadding(padding, padding, padding, padding)
        }
    }

    /**
     * Loads the user's profile photo from the database
     *
     * @param app The application, used to reach the user database system
     * @param user The [User] whose photo to load
     * @return The compressed image bytes, or null if the user has no photo or it couldn't be loaded
     */
    suspend fun load(app: BudgetTreeApplication, user: User): ByteArray? =
        try {
            app.userDatabaseSystem.findProfilePhoto(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    /**
     * Shrinks and compresses a picked image so it fits in a Firestore document (limited to 1 MiB)
     *
     * @param original The picked image in any format Android can decode
     * @param maxSize The longest side of the result in pixels
     * @return JPEG bytes, or null if the bytes aren't an image
     */
    fun compress(original: ByteArray, maxSize: Int = 512): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSize && bounds.outHeight / (sample * 2) >= maxSize) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(original, 0, original.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scale = maxSize.toFloat() / maxOf(decoded.width, decoded.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        } else decoded
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return out.toByteArray()
    }
}
