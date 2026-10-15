package com.example.scopeinspector.ui.home

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.scopeinspector.R
import com.example.scopeinspector.navigation.DemoModule
import com.example.scopeinspector.permissions.AppPermission
import com.example.scopeinspector.testing.grant
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Pantalla alta para que la LazyColumn componga todas las tarjetas a la vez.
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun row(permission: AppPermission, stateLabel: Int): String =
        string(R.string.permission_row, string(permission.labelRes), string(stateLabel))

    private fun show(onOpenModule: (DemoModule) -> Unit = {}) {
        composeRule.setContent { ScopeInspectorTheme { HomeScreen(onOpenModule = onOpenModule) } }
    }

    @Test
    fun showsTheTitleTheIntroAndOneCardPerModule() {
        show()

        composeRule.onNodeWithText(string(R.string.home_intro)).assertIsDisplayed()
        DemoModule.entries.forEach { module ->
            composeRule.onNodeWithText(string(module.titleRes)).assertIsDisplayed()
        }
    }

    @Test
    fun everyCardShowsWhatItBringsAndAnExample() {
        show()

        DemoModule.entries.forEach { module ->
            composeRule.onNodeWithText(string(module.featureRes)).assertIsDisplayed()
            composeRule.onNodeWithText(string(module.exampleRes)).assertIsDisplayed()
        }
    }

    @Test
    fun showsTheShortcutsHintExplainingWhereTheyAre() {
        show()

        composeRule.onNodeWithText(string(R.string.shortcuts_hint_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.shortcuts_hint_body)).assertIsDisplayed()
    }

    @Test
    fun tappingACardOpensThatModule() {
        val opened = mutableListOf<DemoModule>()
        show(onOpenModule = { opened += it })

        composeRule.onNodeWithText(string(DemoModule.Camera.titleRes)).performClick()

        assertEquals(listOf(DemoModule.Camera), opened)
    }

    @Test
    fun showsTheRealPermissionStateOfEachModule() {
        composeRule.activity.application.grant(Manifest.permission.CAMERA)
        show()

        composeRule.onNodeWithText(row(AppPermission.Camera, R.string.state_granted)).assertIsDisplayed()
        composeRule.onNodeWithText(row(AppPermission.RecordAudio, R.string.state_denied)).assertIsDisplayed()
    }

    @Test
    fun theBiometricCardExplainsThatItsPermissionIsGrantedAtInstall() {
        composeRule.activity.application.grant(Manifest.permission.USE_BIOMETRIC)
        show()

        composeRule
            .onNodeWithText(row(AppPermission.UseBiometric, R.string.state_granted_at_install))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.install_permission_explanation)).assertIsDisplayed()
    }
}
