package com.example.mybudgettree

import com.example.mybudgettree.database.entries.DeviceImages
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceImagesTest {

    private val here = "this-device"
    private val other = "other-device"

    // ---- isRealPath

    @Test
    fun isRealPath_aPath_isTrue() {
        assertTrue(DeviceImages.isRealPath("/images/a.jpg"))
    }

    @Test
    fun isRealPath_blankOrTheMarker_isFalse() {
        assertFalse(DeviceImages.isRealPath(""))
        assertFalse(DeviceImages.isRealPath("   "))
        assertFalse(DeviceImages.isRealPath(DeviceImages.DECLINED))
    }

    @Test
    fun declinedMarker_isNA() {
        assertEquals("N/A", DeviceImages.DECLINED)
    }

    // ---- hasImage

    @Test
    fun hasImage_noDevices_isFalse() {
        assertFalse(DeviceImages.hasImage(emptyMap()))
    }

    @Test
    fun hasImage_aRealPath_isTrue() {
        assertTrue(DeviceImages.hasImage(mapOf(other to "/images/a.jpg")))
    }

    @Test
    fun hasImage_onlyDevicesThatSaidNo_isFalse() {
        assertFalse(DeviceImages.hasImage(mapOf(here to DeviceImages.DECLINED, other to DeviceImages.DECLINED)))
    }

    @Test
    fun hasImage_oneDeviceSaidNo_butAnotherHasAnImage_isTrue() {
        assertTrue(DeviceImages.hasImage(mapOf(here to DeviceImages.DECLINED, other to "/images/a.jpg")))
    }

    // ---- pathFor

    @Test
    fun pathFor_returnsTheDevicesPath() {
        assertEquals("/images/a.jpg", DeviceImages.pathFor(mapOf(here to "/images/a.jpg"), here))
    }

    @Test
    fun pathFor_unknownDevice_isNull() {
        assertNull(DeviceImages.pathFor(mapOf(other to "/images/a.jpg"), here))
    }

    @Test
    fun pathFor_aDeviceThatSaidNo_isNull_notAnEmptyPath() {
        assertNull(DeviceImages.pathFor(mapOf(here to DeviceImages.DECLINED), here))
    }

    // ---- needsDecision

    @Test
    fun needsDecision_anotherDeviceHasAnImage_andThisOneWasNeverAsked_isTrue() {
        assertTrue(DeviceImages.needsDecision(mapOf(other to "/images/a.jpg"), here))
    }

    @Test
    fun needsDecision_noImageAnywhere_isFalse() {
        assertFalse(DeviceImages.needsDecision(emptyMap(), here))
    }

    @Test
    fun needsDecision_thisDeviceHasItsOwnImage_isFalse() {
        assertFalse(DeviceImages.needsDecision(mapOf(here to "/images/mine.jpg", other to "/images/a.jpg"), here))
    }

    @Test
    fun needsDecision_thisDeviceAlreadySaidNo_isFalse() {
        assertFalse(DeviceImages.needsDecision(mapOf(here to DeviceImages.DECLINED, other to "/images/a.jpg"), here))
    }

    @Test
    fun needsDecision_onlyThisDeviceHasAnImage_isFalse() {
        assertFalse(DeviceImages.needsDecision(mapOf(here to "/images/mine.jpg"), here))
    }

    @Test
    fun needsDecision_theOtherDeviceOnlySaidNo_isFalse() {
        // nobody has an image, so there is nothing to ask about
        assertFalse(DeviceImages.needsDecision(mapOf(other to DeviceImages.DECLINED), here))
    }

    @Test
    fun needsDecision_severalOtherDevices_oneWithAnImage_isTrue() {
        assertTrue(DeviceImages.needsDecision(mapOf("a" to DeviceImages.DECLINED, "b" to "/images/b.jpg"), here))
    }

    // ---- the same rules on the entities

    @Test
    fun expense_usesTheRules() {
        val expense = Expense(imagePaths = mapOf(other to "/images/a.jpg", "third" to DeviceImages.DECLINED))
        assertTrue(expense.hasImage())
        assertTrue(expense.needsImageDecision(here))
        assertNull(expense.imagePathFor(here))
        assertNull(expense.imagePathFor("third"))
        assertEquals("/images/a.jpg", expense.imagePathFor(other))
        assertFalse(expense.needsImageDecision("third"))
    }

    @Test
    fun income_usesTheRules() {
        val income = Income(imagePaths = mapOf(other to "/images/a.jpg", here to DeviceImages.DECLINED))
        assertTrue(income.hasImage())
        assertFalse(income.needsImageDecision(here))
        assertNull(income.imagePathFor(here))
    }

    @Test
    fun expenseAndIncomeWithNoImages_neverNeedADecision() {
        assertFalse(Expense().needsImageDecision(here))
        assertFalse(Income().needsImageDecision(here))
        assertFalse(Expense().hasImage())
    }
}
