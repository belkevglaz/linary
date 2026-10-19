package ru.b3lka.linary.presentation.media

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoStripStateTest {

    private class MemoryPreferences(override var photosExpanded: Boolean) : PhotoStripPreferences

    @Test
    fun toggleAndCollapseAreSaved() {
        val prefs = MemoryPreferences(photosExpanded = true)
        val strip = PhotoStripState(prefs)
        assertTrue(strip.expanded)

        strip.toggle()
        assertFalse(strip.expanded)
        assertFalse(prefs.photosExpanded)

        strip.toggle()
        strip.collapse()
        assertFalse(strip.expanded)
        assertFalse(prefs.photosExpanded)
    }

    @Test
    fun startsWithSavedState() {
        assertFalse(PhotoStripState(MemoryPreferences(photosExpanded = false)).expanded)
    }
}
