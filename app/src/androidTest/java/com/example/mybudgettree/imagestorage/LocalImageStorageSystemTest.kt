package com.example.mybudgettree.imagestorage

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocalImageStorageSystemTest {

    private lateinit var imageStorageSystem: ImageStorageSystem

    @Before
    fun setUp() {
        imageStorageSystem = LocalImageStorageSystem(ApplicationProvider.getApplicationContext())
    }

    private fun samplePngBytes(width: Int = 4, height: Int = 4): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @Test
    fun saveThenLoad_returnsMatchingImage() = runBlocking {
        val bytes = samplePngBytes(4, 4)
        val path = imageStorageSystem.saveImage(bytes, "${UUID.randomUUID()}.png")
        assertNotNull(path)
        val loaded = imageStorageSystem.loadImage(path)
        assertNotNull(loaded)
        assertEquals(4, loaded?.width)
        assertEquals(4, loaded?.height)
    }

    @Test
    fun saveImage_nullBytes_returnsNull() = runBlocking {
        val path = imageStorageSystem.saveImage(null, "${UUID.randomUUID()}.png")
        assertNull(path)
    }

    @Test
    fun loadImage_nullPath_returnsNull() = runBlocking {
        val loaded = imageStorageSystem.loadImage(null)
        assertNull(loaded)
    }

    @Test
    fun deleteImage_removesFile_andLoadImageThenReturnsNull() = runBlocking {
        val path = imageStorageSystem.saveImage(samplePngBytes(), "${UUID.randomUUID()}.png")
        val deleted = imageStorageSystem.deleteImage(path)
        assertTrue(deleted)
        val loaded = imageStorageSystem.loadImage(path)
        assertNull(loaded)
    }

    @Test
    fun deleteImage_nullPath_returnsFalse() = runBlocking {
        val deleted = imageStorageSystem.deleteImage(null)
        assertFalse(deleted)
    }
}
