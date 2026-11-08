package com.example.scopeinspector.navigation

import com.example.scopeinspector.permissions.AppPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoModuleTest {

    @Test
    fun routesAreUniqueAndDoNotClashWithHome() {
        val routes = DemoModule.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
        assertFalse("home" in routes)
        assertFalse(INSPECTOR_ROUTE in routes)
    }

    @Test
    fun everyModuleDeclaresAtLeastOnePermission() {
        DemoModule.entries.forEach { assertTrue(it.name, it.permissions.isNotEmpty()) }
    }

    @Test
    fun moduleDescriptionTextsAreDistinct() {
        val texts = DemoModule.entries.flatMap {
            listOf(it.summaryRes, it.featureRes, it.exampleRes, it.howToTestRes)
        }
        assertEquals(texts.size, texts.toSet().size)
    }

    @Test
    fun everyAppPermissionBelongsToExactlyOneModule() {
        val declared = DemoModule.entries.flatMap { it.permissions }
        assertEquals(AppPermission.entries.toSet(), declared.toSet())
        assertEquals(declared.size, declared.toSet().size)
    }
}
