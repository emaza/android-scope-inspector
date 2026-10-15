package com.example.scopeinspector.feature.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileUsagePolicyTest {

    @Test
    fun userAgentNamesTheAppAndIncludesVersionAndContact() {
        assertEquals(
            "ScopeInspector/0.1.0 (https://github.com/emaza/android-scope-inspector)",
            TileUsagePolicy.userAgent("0.1.0"),
        )
    }

    @Test
    fun userAgentIsNotAGenericOrTemplateIdentity() {
        val userAgent = TileUsagePolicy.userAgent("1.0")
        assertFalse(userAgent.startsWith("com.example"))
        assertFalse(userAgent.startsWith("osmdroid"))
        assertTrue(userAgent.startsWith(TileUsagePolicy.APP_NAME))
    }

    @Test
    fun contactAndCopyrightUrlsAreHttps() {
        assertTrue(TileUsagePolicy.CONTACT_URL.startsWith("https://"))
        assertTrue(TileUsagePolicy.COPYRIGHT_URL.startsWith("https://"))
    }
}
