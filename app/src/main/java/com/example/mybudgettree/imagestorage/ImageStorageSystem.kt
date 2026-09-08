package com.example.mybudgettree.imagestorage

import android.graphics.Bitmap

interface ImageStorageSystem {
    suspend fun saveImage(imageBytes: ByteArray?, fileName: String): String?
    suspend fun loadImage(imagePath: String?): Bitmap?
    suspend fun deleteImage(imagePath: String?): Boolean
}
