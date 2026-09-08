package com.example.mybudgettree.imagestorage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalImageStorageSystem(private val context: Context) : ImageStorageSystem {

    private val imagesDir: File get() = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    override suspend fun saveImage(imageBytes: ByteArray?, fileName: String): String? {
        if (imageBytes == null) return null
        return withContext(Dispatchers.IO) {
            val file = File(imagesDir, fileName)
            file.writeBytes(imageBytes)
            file.absolutePath
        }
    }

    override suspend fun loadImage(imagePath: String?): Bitmap? {
        if (imagePath == null) return null
        return withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(imagePath)
        }
    }

    override suspend fun deleteImage(imagePath: String?): Boolean {
        if (imagePath == null) return false
        return withContext(Dispatchers.IO) {
            File(imagePath).delete()
        }
    }
}
