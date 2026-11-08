package com.example.scopeinspector.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.scopeinspector.R
import com.example.scopeinspector.shortcuts.ShortcutPolicy
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Navegación y deep links. No se navega a Ubicación: su pantalla carga un mapa real (osmdroid) y
 * Play Services; su ruta se comprueba solo como deep link registrado, sin componerla.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
class AppNavGraphTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    private val currentRoute get() = navController.currentBackStackEntry?.destination?.route
    private val routeBelow get() = navController.previousBackStackEntry?.destination?.route

    private fun string(id: Int): String = composeRule.activity.getString(id)

    private fun show() {
        composeRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ScopeInspectorTheme { AppNavGraph(navController) }
        }
        composeRule.waitForIdle()
    }

    private fun moduleLink(route: String, flags: Int = 0) =
        Intent(Intent.ACTION_VIEW, ShortcutPolicy.moduleUri(route).toUri()).addFlags(flags)

    // Un lanzador abre los atajos con una tarea nueva y limpia.
    private val launcherFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    private fun sendNewIntent(intent: Intent) {
        composeRule.runOnUiThread {
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(composeRule.activity, intent)
        }
        composeRule.waitForIdle()
    }

    private fun handleDeepLink(intent: Intent): Boolean {
        var handled = false
        composeRule.runOnUiThread { handled = navController.handleDeepLink(intent) }
        composeRule.waitForIdle()
        return handled
    }

    // --- navegación normal ---------------------------------------------------------------------------

    @Test
    fun startsAtHome() {
        show()

        assertEquals("home", currentRoute)
    }

    @Test
    fun tappingAModuleCardOpensItsScreen() {
        show()

        composeRule.onNodeWithText(string(DemoModule.Camera.titleRes)).performClick()
        composeRule.waitForIdle()

        assertEquals(DemoModule.Camera.route, currentRoute)
        assertEquals("home", routeBelow)
    }

    @Test
    fun theInspectorCardOpensTheInspectorAndBackReturnsHome() {
        show()

        composeRule.onNodeWithText(string(R.string.inspector_card_title)).performClick()
        composeRule.waitForIdle()
        assertEquals(INSPECTOR_ROUTE, currentRoute)
        assertEquals("home", routeBelow)

        composeRule.onNodeWithText(string(R.string.action_back)).performClick()
        composeRule.waitForIdle()
        assertEquals("home", currentRoute)
    }

    @Test
    fun theBackButtonOfAModuleReturnsToHome() {
        show()
        composeRule.onNodeWithText(string(DemoModule.Biometric.titleRes)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.action_back)).performClick()
        composeRule.waitForIdle()

        assertEquals("home", currentRoute)
    }

    // --- deep links -------------------------------------------------------------------------------------

    @Test
    fun everyModuleHasItsDeepLinkRegistered() {
        show()

        DemoModule.entries.forEach { module ->
            assertTrue(
                "falta el deep link de ${module.route}",
                navController.graph.hasDeepLink(ShortcutPolicy.moduleUri(module.route).toUri()),
            )
        }
    }

    @Test
    fun aLauncherIntentOnColdStartOpensTheModuleWithHomeBelowIt() {
        show()

        assertTrue(handleDeepLink(moduleLink(DemoModule.Biometric.route, launcherFlags)))

        assertEquals(DemoModule.Biometric.route, currentRoute)
        assertEquals("home", routeBelow)
    }

    @Test
    fun backAfterALauncherIntentGoesToHomeNotOutOfTheApp() {
        show()
        handleDeepLink(moduleLink(DemoModule.Background.route, launcherFlags))

        composeRule.onNodeWithText(string(R.string.action_back)).performClick()
        composeRule.waitForIdle()

        assertEquals("home", currentRoute)
    }

    @Test
    fun aDeepLinkWithoutFlagsStillOpensTheModule() {
        show()

        assertTrue(handleDeepLink(moduleLink(DemoModule.Biometric.route)))

        assertEquals(DemoModule.Biometric.route, currentRoute)
    }

    @Test
    fun aDeepLinkToAnUnknownModuleIsIgnored() {
        show()

        assertFalse(handleDeepLink(moduleLink("unknown")))

        assertEquals("home", currentRoute)
    }

    @Test
    fun aLinkWithAnotherSchemeIsIgnored() {
        show()

        assertFalse(handleDeepLink(Intent(Intent.ACTION_VIEW, "https://example.com/module/camera".toUri())))

        assertEquals("home", currentRoute)
    }

    // --- app ya abierta: el atajo llega como nuevo intent (singleTask) ---------------------------------------

    @Test
    fun aNewIntentWhileTheAppIsOpenNavigatesToTheModuleWithHomeBelowIt() {
        show()

        sendNewIntent(moduleLink(DemoModule.Biometric.route, launcherFlags))

        assertEquals(DemoModule.Biometric.route, currentRoute)
        assertEquals("home", routeBelow)
    }

    @Test
    fun theNewIntentWorksWithOrWithoutLauncherFlags() {
        show()

        sendNewIntent(moduleLink(DemoModule.Biometric.route))

        assertEquals(DemoModule.Biometric.route, currentRoute)
        assertEquals("home", routeBelow)
    }

    @Test
    fun aShortcutFromAnotherModuleReplacesItInsteadOfStacking() {
        show()
        composeRule.onNodeWithText(string(DemoModule.Camera.titleRes)).performClick()
        composeRule.waitForIdle()

        sendNewIntent(moduleLink(DemoModule.Biometric.route, launcherFlags))

        assertEquals(DemoModule.Biometric.route, currentRoute)
        assertEquals("home", routeBelow)
        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.waitForIdle()
        assertEquals("tras volver no debe quedar el módulo anterior debajo", "home", currentRoute)
    }

    @Test
    fun theSameShortcutTwiceDoesNotStackTheModuleTwice() {
        show()

        sendNewIntent(moduleLink(DemoModule.Biometric.route, launcherFlags))
        sendNewIntent(moduleLink(DemoModule.Biometric.route, launcherFlags))
        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.waitForIdle()

        assertEquals("home", currentRoute)
    }

    @Test
    fun aNewIntentForAnUnknownModuleIsIgnored() {
        show()

        sendNewIntent(moduleLink("secret", launcherFlags))

        assertEquals("home", currentRoute)
    }

    @Test
    fun aNewIntentWithoutAModuleLinkLeavesNavigationAsItWas() {
        show()
        composeRule.onNodeWithText(string(DemoModule.Camera.titleRes)).performClick()
        composeRule.waitForIdle()

        sendNewIntent(Intent(Intent.ACTION_MAIN))

        assertEquals(DemoModule.Camera.route, currentRoute)
    }
}
