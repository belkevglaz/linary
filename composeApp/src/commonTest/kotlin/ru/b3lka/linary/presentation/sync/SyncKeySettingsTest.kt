package ru.b3lka.linary.presentation.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncKeySettingsTest {

    private class MemoryPreferences(override var syncKey: String? = null) : SyncKeyPreferences

    private val valid = "k".repeat(SyncKeySettings.LENGTH)

    @Test
    fun saveAcceptsOnlyWellFormedKeys() {
        val prefs = MemoryPreferences()
        val keys = SyncKeySettings(prefs)
        assertFalse(keys.isSet)

        assertFalse(keys.save("short"))
        assertFalse(keys.save("ключ".repeat(8)))
        assertNull(prefs.syncKey)

        assertTrue(keys.save("  $valid \n"))
        assertTrue(keys.isSet)
        assertEquals(valid, keys.key)
    }

    @Test
    fun clearRemovesKey() {
        val prefs = MemoryPreferences(valid)
        val keys = SyncKeySettings(prefs)
        assertTrue(keys.isSet)
        keys.clear()
        assertFalse(keys.isSet)
        assertNull(prefs.syncKey)
        assertNull(keys.key)
    }

    @Test
    fun malformedStoredKeyIsIgnored() {
        val keys = SyncKeySettings(MemoryPreferences("broken"))
        assertFalse(keys.isSet)
        assertNull(keys.key)
    }
}
