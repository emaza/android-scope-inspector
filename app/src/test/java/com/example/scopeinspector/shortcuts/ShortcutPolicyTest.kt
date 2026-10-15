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

    private val routes = listOf("location", "camera")

    @Test
    fun moduleRouteResolvesAKnownModuleLink() {
        assertEquals("camera", ShortcutPolicy.moduleRoute("scopeinspector://module/camera", routes))
    }

    @Test
    fun moduleRouteIsTheInverseOfModuleUri() {
        DemoModule.entries.forEach { module ->
            assertEquals(
                module.route,
                ShortcutPolicy.moduleRoute(ShortcutPolicy.moduleUri(module.route), DemoModule.entries.map { it.route }),
            )
        }
    }

    @Test
    fun moduleRouteRejectsUnknownModules() {
        assertEquals(null, ShortcutPolicy.moduleRoute("scopeinspector://module/secret", routes))
    }

    @Test
    fun moduleRouteRejectsOtherSchemesAndHosts() {
        assertEquals(null, ShortcutPolicy.moduleRoute("https://module/camera", routes))
        assertEquals(null, ShortcutPolicy.moduleRoute("scopeinspector://other/camera", routes))
    }

    @Test
    fun moduleRouteRejectsExtraPathSegmentsAndQueries() {
        assertEquals(null, ShortcutPolicy.moduleRoute("scopeinspector://module/camera/extra", routes))
        assertEquals(null, ShortcutPolicy.moduleRoute("scopeinspector://module/camera?x=1", routes))
        assertEquals(null, ShortcutPolicy.moduleRoute("scopeinspector://module/", routes))
    }

    @Test
    fun moduleRouteRejectsMissingLinks() {
        assertEquals(null, ShortcutPolicy.moduleRoute(null, routes))
        assertEquals(null, ShortcutPolicy.moduleRoute("", routes))
    }
}
