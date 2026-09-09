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
class ExpenseImageIntegrationTest : DatabaseTestBase() {

    private fun samplePngBytes(width: Int = 4, height: Int = 4): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @Test
    fun createExpense_withImage_canBeLoadedBackThroughImageStorage() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val imagePath = imageStorageSystem.saveImage(samplePngBytes(), "${UUID.randomUUID()}.png")

        val result = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Receipt",
            amount = 100.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = imagePath
        )

        assertTrue(result.wasSuccessful)
        val loadedImage = imageStorageSystem.loadImage(result.expense?.imagePath)
        assertNotNull(loadedImage)
        assertEquals(4, loadedImage?.width)
    }

    @Test
    fun createExpense_withoutImage_loadImageReturnsNull() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)

        val result = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Receipt",
            amount = 100.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5)
        )

        assertTrue(result.wasSuccessful)
        assertNull(result.expense?.imagePath)
        val loadedImage = imageStorageSystem.loadImage(result.expense?.imagePath)
        assertNull(loadedImage)
    }

    @Test
    fun updateExpenseImage_replacesImage_oldOneNoLongerLoadable() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val originalPath = imageStorageSystem.saveImage(samplePngBytes(4, 4), "${UUID.randomUUID()}.png")
        val created = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Receipt",
            amount = 100.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = originalPath
        ).expense!!

        val newPath = imageStorageSystem.saveImage(samplePngBytes(8, 8), "${UUID.randomUUID()}.png")
        val updateResult = expenseDatabaseSystem.updateExpenseImage(created, newPath)
        imageStorageSystem.deleteImage(originalPath)

        assertEquals(ExpenseDatabaseSystem.UpdateExpenseReturnStatus.Succeeded, updateResult.status)
        assertNull(imageStorageSystem.loadImage(originalPath))
        val reloadedNewImage = imageStorageSystem.loadImage(updateResult.expense?.imagePath)
        assertNotNull(reloadedNewImage)
        assertEquals(8, reloadedNewImage?.width)
    }

    @Test
    fun expenseImagePath_deletedExternally_loadImageReturnsNullButExpenseStillValid() = runBlocking {
        val user = createTestUser()
        val category = createTestCategory(user)
        val imagePath = imageStorageSystem.saveImage(samplePngBytes(), "${UUID.randomUUID()}.png")
        val expense = expenseDatabaseSystem.createExpense(
            category = category,
            description = "Receipt",
            amount = 100.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = imagePath
        ).expense!!

        imageStorageSystem.deleteImage(imagePath)

        assertNull(imageStorageSystem.loadImage(expense.imagePath))
        assertTrue(expenseDatabaseSystem.isExpenseStillValid(expense))
    }
}
