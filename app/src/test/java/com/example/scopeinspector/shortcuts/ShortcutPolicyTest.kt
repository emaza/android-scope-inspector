package com.example.scopeinspector.shortcuts

import com.example.scopeinspector.navigation.DemoModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutPolicyTest {

    @Test
    fun offersThePinOnlyWhenSupportedAndNeverOfferedAndNotPinned() {
        assertTrue(ShortcutPolicy.shouldOfferPin(supported = true, alreadyOffered = false, alreadyPinned = false))
    }

    @Test
    fun doesNotOfferWhenTheLauncherCannotPinShortcuts() {
        assertFalse(ShortcutPolicy.shouldOfferPin(supported = false, alreadyOffered = false, alreadyPinned = false))
    }

    @Test
    fun doesNotOfferAgainAfterItWasOffered() {
        assertFalse(ShortcutPolicy.shouldOfferPin(supported = true, alreadyOffered = true, alreadyPinned = false))
    }

    @Test
    fun doesNotOfferWhenAShortcutIsAlreadyPinned() {
        assertFalse(ShortcutPolicy.shouldOfferPin(supported = true, alreadyOffered = false, alreadyPinned = true))
    }

    @Test
    fun moduleUriUsesTheInternalScheme() {
        assertEquals("scopeinspector://module/location", ShortcutPolicy.moduleUri("location"))
    }

    @Test
    fun everyModuleHasAUniqueUriAndShortcutId() {
        val uris = DemoModule.entries.map { ShortcutPolicy.moduleUri(it.route) }
        val ids = DemoModule.entries.map { ShortcutPolicy.moduleShortcutId(it.route) }
        assertEquals(uris.size, uris.toSet().size)
        assertEquals(ids.size, ids.toSet().size)
        assertFalse(ShortcutPolicy.HOME_SHORTCUT_ID in ids)
    }
}
