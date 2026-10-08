package com.example.mybudgettree.database.managers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mybudgettree.database.DatabaseTestBase
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.managers.shared.UpdateReturnStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val deviceA = "device-a"
    private val deviceB = "device-b"

    private fun samplePngBytes(width: Int = 4, height: Int = 4): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun saveSampleImage(size: Int = 4): String =
        runBlocking { imageStorageSystem.saveImage(samplePngBytes(size, size), "${UUID.randomUUID()}.png") }

    private suspend fun createExpenseWithImage(imagePath: String?, deviceId: String?) =
        expenseDatabaseSystem.createExpense(
            category = createTestCategory(createTestUser()),
            description = "Receipt",
            amount = 100.0,
            date = LocalDate.of(2026, 1, 1),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(9, 5),
            imagePath = imagePath,
            deviceId = deviceId
        )

    private suspend fun reload(expense: Expense): Expense =
        userDatabaseSystem.getCurrentUser()!!.let { expenseDatabaseSystem.retrieveAllExpenses(it).values!!.single { e -> e.id == expense.id } }

    @Test
    fun createExpense_withImage_canBeLoadedBackThroughImageStorage() = runBlocking {
        val imagePath = saveSampleImage()
        val result = createExpenseWithImage(imagePath, deviceA)

        assertTrue(result.wasSuccessful)
        assertEquals(imagePath, result.value?.imagePathFor(deviceA))
        assertTrue(result.value!!.hasImage())
        val loadedImage = imageStorageSystem.loadImage(result.value?.imagePathFor(deviceA))
        assertNotNull(loadedImage)
        assertEquals(4, loadedImage?.width)
    }

    @Test
    fun createExpense_withImage_pathIsSavedInTheDatabase() = runBlocking {
        val imagePath = saveSampleImage()
        val created = createExpenseWithImage(imagePath, deviceA).value!!
        assertEquals(mapOf(deviceA to imagePath), reload(created).imagePaths)
    }

    @Test
    fun createExpense_withoutImage_hasNoImage() = runBlocking {
        val result = createExpenseWithImage(null, null)

        assertTrue(result.wasSuccessful)
        assertFalse(result.value!!.hasImage())
        assertNull(result.value?.imagePathFor(deviceA))
        assertNull(imageStorageSystem.loadImage(result.value?.imagePathFor(deviceA)))
    }

    @Test
    fun createExpense_withImageButNoDeviceId_fails() = runBlocking {
        val result = createExpenseWithImage(saveSampleImage(), null)
        assertFalse(result.wasSuccessful)
    }

    @Test
    fun updateExpenseImage_replacesImageForSameDevice_oldOneNoLongerLoadable() = runBlocking {
        val originalPath = saveSampleImage(4)
        val created = createExpenseWithImage(originalPath, deviceA).value!!

        val newPath = saveSampleImage(8)
        val updateResult = expenseDatabaseSystem.updateExpenseImage(created, deviceA, newPath)
        imageStorageSystem.deleteImage(originalPath)

        assertEquals(UpdateReturnStatus.Succeeded, updateResult.status)
        assertNull(imageStorageSystem.loadImage(originalPath))
        val reloadedNewImage = imageStorageSystem.loadImage(updateResult.value?.imagePathFor(deviceA))
        assertNotNull(reloadedNewImage)
        assertEquals(8, reloadedNewImage?.width)
        assertEquals(newPath, reload(created).imagePathFor(deviceA))
    }

    @Test
    fun updateExpenseImage_sameImage_noChange() = runBlocking {
        val path = saveSampleImage()
        val created = createExpenseWithImage(path, deviceA).value!!
        val result = expenseDatabaseSystem.updateExpenseImage(created, deviceA, path)
        assertEquals(UpdateReturnStatus.NoChange, result.status)
    }

    @Test
    fun updateExpenseImage_blankPath_fails() = runBlocking {
        val created = createExpenseWithImage(null, null).value!!
        val result = expenseDatabaseSystem.updateExpenseImage(created, deviceA, " ")
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateExpenseImage_blankDeviceId_fails() = runBlocking {
        val created = createExpenseWithImage(null, null).value!!
        val result = expenseDatabaseSystem.updateExpenseImage(created, "", saveSampleImage())
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun updateExpenseImage_twoDevices_keepSeparatePaths() = runBlocking {
        val pathA = saveSampleImage(4)
        val pathB = saveSampleImage(8)
        val created = createExpenseWithImage(pathA, deviceA).value!!

        val withBoth = expenseDatabaseSystem.updateExpenseImage(created, deviceB, pathB).value!!

        assertEquals(mapOf(deviceA to pathA, deviceB to pathB), withBoth.imagePaths)
        val saved = reload(created)
        assertEquals(pathA, saved.imagePathFor(deviceA))
        assertEquals(pathB, saved.imagePathFor(deviceB))
    }

    @Test
    fun updateExpenseImage_changingOneDevice_leavesTheOtherAlone() = runBlocking {
        val pathA = saveSampleImage(4)
        val pathB = saveSampleImage(8)
        val created = createExpenseWithImage(pathA, deviceA).value!!
        val withBoth = expenseDatabaseSystem.updateExpenseImage(created, deviceB, pathB).value!!

        // Device B comes along with a stale copy that only knew about A: the write must still not touch A
        val newPathB = saveSampleImage(16)
        expenseDatabaseSystem.updateExpenseImage(created, deviceB, newPathB)

        val saved = reload(withBoth)
        assertEquals(pathA, saved.imagePathFor(deviceA))
        assertEquals(newPathB, saved.imagePathFor(deviceB))
    }

    @Test
    fun updateExpenseImage_removingOneDevice_keepsTheOther_andHasImageStaysTrue() = runBlocking {
        val pathA = saveSampleImage(4)
        val pathB = saveSampleImage(8)
        val created = createExpenseWithImage(pathA, deviceA).value!!
        val withBoth = expenseDatabaseSystem.updateExpenseImage(created, deviceB, pathB).value!!

        val afterRemove = expenseDatabaseSystem.updateExpenseImage(withBoth, deviceB, null)

        assertEquals(UpdateReturnStatus.Succeeded, afterRemove.status)
        assertEquals(mapOf(deviceA to pathA), afterRemove.value?.imagePaths)
        assertTrue(reload(created).hasImage())
    }

    @Test
    fun updateExpenseImage_removingTheLastImage_meansNoDeviceHasOne() = runBlocking {
        val created = createExpenseWithImage(saveSampleImage(), deviceA).value!!
        val result = expenseDatabaseSystem.updateExpenseImage(created, deviceA, null)
        assertEquals(UpdateReturnStatus.Succeeded, result.status)
        assertFalse(reload(created).hasImage())
    }

    @Test
    fun updateExpenseImage_expenseDeleted_fails() = runBlocking {
        val created = createExpenseWithImage(null, null).value!!
        expenseDatabaseSystem.deleteExpense(created)
        val result = expenseDatabaseSystem.updateExpenseImage(created, deviceA, saveSampleImage())
        assertEquals(UpdateReturnStatus.Failed, result.status)
    }

    @Test
    fun expenseImageFile_deletedExternally_loadImageReturnsNullButExpenseStillExists() = runBlocking {
        val imagePath = saveSampleImage()
        val expense = createExpenseWithImage(imagePath, deviceA).value!!

        imageStorageSystem.deleteImage(imagePath)

        assertNull(imageStorageSystem.loadImage(expense.imagePathFor(deviceA)))
        assertEquals(expense.id, reload(expense).id)
    }
}
