package com.example.mybudgettree

import com.example.mybudgettree.database.entries.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryGardenTest {

    private fun category(name: String, iconKey: String? = null) =
        Category(username = "user", categoryName = name, iconKey = iconKey)

    @Test
    fun defaultNames_doesNotContainSavings() {
        // Savings is tracked via the Watering Can goals instead — seeding it as a default
        // spending category would be confusing alongside that
        assertEquals(false, CategoryGarden.defaultNames.any { it.equals("Savings", ignoreCase = true) })
    }

    @Test
    fun sort_ordersKnownDefaultsBeforeCustomOnes() {
        val categories = listOf(
            category("Custom Hobby"),
            category("Rent"),
            category("Food")
        )
        val sorted = CategoryGarden.sort(categories).map { it.categoryName }
        assertEquals(listOf("Food", "Rent", "Custom Hobby"), sorted)
    }

    @Test
    fun sort_isCaseInsensitiveAgainstDefaultOrder() {
        val categories = listOf(category("food"), category("TRANSPORT"))
        val sorted = CategoryGarden.sort(categories).map { it.categoryName }
        assertEquals(listOf("food", "TRANSPORT"), sorted)
    }

    @Test
    fun sort_customCategories_fallBackToAlphabeticalOrder() {
        val categories = listOf(category("Zebra"), category("Apple"))
        val sorted = CategoryGarden.sort(categories).map { it.categoryName }
        assertEquals(listOf("Apple", "Zebra"), sorted)
    }

    @Test
    fun iconKeyFor_usesExplicitIconKeyWhenSet() {
        val cat = category("Food", iconKey = "car")
        assertEquals("car", CategoryGarden.iconKeyFor(cat))
    }

    @Test
    fun iconKeyFor_fallsBackToNameBasedDefault() {
        val cat = category("Rent", iconKey = null)
        assertEquals("rent", CategoryGarden.iconKeyFor(cat))
    }

    @Test
    fun iconKeyFor_unknownNameWithNoIconKey_fallsBackToGenericDefault() {
        val cat = category("Custom Hobby", iconKey = null)
        assertEquals(IconCatalog.DEFAULT_KEY, CategoryGarden.iconKeyFor(cat))
    }
}
