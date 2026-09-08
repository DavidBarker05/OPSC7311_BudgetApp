package com.example.mybudgettree.database.managers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class IncomeImageIntegrationTest : DatabaseTestBase() {

    private fun samplePngBytes(width: Int = 4, height: Int = 4): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @Test
    fun createIncome_withImage_canBeLoadedBackThroughImageStorage() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")
        val imagePath = imageStorageSystem.saveImage(samplePngBytes(), "${UUID.randomUUID()}.png")

        val result = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Payslip",
            currencyAtTime = "ZAR",
            amount = 15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = imagePath
        )

        assertTrue(result.wasSuccessful)
        val loadedImage = imageStorageSystem.loadImage(result.income?.imagePath)
        assertNotNull(loadedImage)
        assertEquals(4, loadedImage?.width)
    }

    @Test
    fun createIncome_withoutImage_loadImageReturnsNull() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")

        val result = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Payslip",
            currencyAtTime = "ZAR",
            amount = 15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )

        assertTrue(result.wasSuccessful)
        assertNull(result.income?.imagePath)
        val loadedImage = imageStorageSystem.loadImage(result.income?.imagePath)
        assertNull(loadedImage)
    }

    @Test
    fun updateIncomeImage_replacesImage_oldOneNoLongerLoadable() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")
        val originalPath = imageStorageSystem.saveImage(samplePngBytes(4, 4), "${UUID.randomUUID()}.png")
        val created = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Payslip",
            currencyAtTime = "ZAR",
            amount = 15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = originalPath
        ).income!!

        val newPath = imageStorageSystem.saveImage(samplePngBytes(8, 8), "${UUID.randomUUID()}.png")
        val updateResult = incomeDatabaseSystem.updateIncomeImage(created, newPath)
        imageStorageSystem.deleteImage(originalPath)

        assertEquals(IncomeDatabaseSystem.UpdateIncomeReturnStatus.Succeeded, updateResult.status)
        assertNull(imageStorageSystem.loadImage(originalPath))
        val reloadedNewImage = imageStorageSystem.loadImage(updateResult.income?.imagePath)
        assertNotNull(reloadedNewImage)
        assertEquals(8, reloadedNewImage?.width)
    }

    @Test
    fun incomeImagePath_deletedExternally_loadImageReturnsNullButIncomeStillValid() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user, "Salary")
        val imagePath = imageStorageSystem.saveImage(samplePngBytes(), "${UUID.randomUUID()}.png")
        val income = incomeDatabaseSystem.createIncome(
            category = category,
            description = "Payslip",
            currencyAtTime = "ZAR",
            amount = 15000.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = imagePath
        ).income!!

        imageStorageSystem.deleteImage(imagePath)

        assertNull(imageStorageSystem.loadImage(income.imagePath))
        assertTrue(incomeDatabaseSystem.isIncomeStillValid(income))
    }
}
