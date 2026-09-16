package com.example.mybudgettree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IconCatalogTest {

    @Test
    fun resFor_knownKey_returnsMatchingIcon() {
        assertEquals(R.drawable.ic_cat_rent, IconCatalog.resFor("rent"))
    }

    @Test
    fun resFor_unknownKey_fallsBackToDefault() {
        assertEquals(IconCatalog.resFor(IconCatalog.DEFAULT_KEY), IconCatalog.resFor("does-not-exist"))
    }

    @Test
    fun resFor_nullKey_fallsBackToDefault() {
        assertEquals(IconCatalog.resFor(IconCatalog.DEFAULT_KEY), IconCatalog.resFor(null))
    }

    @Test
    fun icons_haveUniqueKeys() {
        val keys = IconCatalog.icons.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun icons_haveDistinctDrawableResources() {
        // catches the copy/paste mistake of two icon entries pointing at the same drawable
        val resIds = IconCatalog.icons.map { it.res }
        assertEquals(resIds.size, resIds.toSet().size)
    }

    @Test
    fun defaultKey_isPresentInCatalog() {
        assertNotEquals(-1, IconCatalog.icons.indexOfFirst { it.key == IconCatalog.DEFAULT_KEY })
    }
}
