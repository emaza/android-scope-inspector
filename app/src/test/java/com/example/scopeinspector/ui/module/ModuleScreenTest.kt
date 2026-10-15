package com.example.scopeinspector.ui.module

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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
import com.example.scopeinspector.testing.markRequested
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
class ModuleScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val activity get() = composeRule.activity

    private fun string(id: Int, vararg args: Any): String = activity.getString(id, *args)

    private fun row(permission: AppPermission, stateLabel: Int): String =
        string(R.string.permission_row, string(permission.labelRes), string(stateLabel))

    private fun show(module: DemoModule, onBack: () -> Unit = {}) {
        composeRule.setContent { ScopeInspectorTheme { ModuleScreen(module = module, onBack = onBack) } }
    }

    private fun tapRequestButton() {
        composeRule.onNodeWithText(string(R.string.action_request_permissions)).performClick()
    }

    // --- qué se ve según el estado de los permisos --------------------------------------------------

    @Test
    fun withMissingPermissionsShowsTheRequestButtonAndTheReducedModeNote() {
        show(DemoModule.Camera)

        composeRule.onNodeWithText(string(R.string.action_request_permissions)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.note_denied)).assertIsDisplayed()
        composeRule.onNodeWithText(row(AppPermission.Camera, R.string.state_denied)).assertIsDisplayed()
    }

    @Test
    fun withEverythingGrantedHidesTheButtonAndShowsTheFullFunctionNote() {
        activity.application.grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        show(DemoModule.Camera)

        composeRule.onNodeWithText(string(R.string.action_request_permissions)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.note_granted)).assertIsDisplayed()
    }

    @Test
    fun theBiometricModuleHasNoRequestButtonAndExplainsTheInstallTimePermission() {
        activity.application.grant(Manifest.permission.USE_BIOMETRIC)
        show(DemoModule.Biometric)

        composeRule.onNodeWithText(string(R.string.action_request_permissions)).assertDoesNotExist()
        composeRule
            .onNodeWithText(row(AppPermission.UseBiometric, R.string.state_granted_at_install))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.install_permission_explanation)).assertIsDisplayed()
    }

    @Test
    fun theModuleShowsItsSummaryAndHowToTestIt() {
        show(DemoModule.Background)

        composeRule.onNodeWithText(string(DemoModule.Background.summaryRes)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DemoModule.Background.howToTestRes)).assertIsDisplayed()
    }

    @Test
    fun theBackButtonReportsTheNavigationBack() {
        var backs = 0
        show(DemoModule.Camera, onBack = { backs++ })

        composeRule.onNodeWithText(string(R.string.action_back)).performClick()

        assertEquals(1, backs)
    }

    // --- cuándo se pide ---------------------------------------------------------------------------

    @Test
    fun openingTheScreenDoesNotRequestAnyPermission() {
        show(DemoModule.Camera)
        composeRule.waitForIdle()

        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun tappingTheButtonRequestsTheModulePermissionsThatAreMissing() {
        show(DemoModule.Camera)

        tapRequestButton()
        composeRule.waitForIdle()

        val requested = shadowOf(activity).lastRequestedPermission
        assertNotNull(requested)
        assertEquals(
            listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            requested!!.requestedPermissions.toList(),
        )
    }

    @Test
    fun onlyThePermissionsThatAreStillMissingAreRequested() {
        activity.application.grant(Manifest.permission.CAMERA)
        show(DemoModule.Camera)

        tapRequestButton()
        composeRule.waitForIdle()

        assertEquals(
            listOf(Manifest.permission.RECORD_AUDIO),
            shadowOf(activity).lastRequestedPermission!!.requestedPermissions.toList(),
        )
    }

    @Test
    fun afterTheUserGrantsThePermissionsTheScreenUpdatesToFullFunction() {
        show(DemoModule.Camera)
        tapRequestButton()
        composeRule.waitForIdle()
        val requested = shadowOf(activity).lastRequestedPermission!!

        // El sistema concede y entrega el resultado a la actividad.
        activity.application.grant(*requested.requestedPermissions)
        composeRule.runOnUiThread {
            activity.onRequestPermissionsResult(
                requested.requestCode,
                requested.requestedPermissions,
                IntArray(requested.requestedPermissions.size) { PackageManager.PERMISSION_GRANTED },
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.action_request_permissions)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.note_granted)).assertIsDisplayed()
    }

    // --- rationale tras una denegación ----------------------------------------------------------------

    @Test
    fun afterADenialTheRationaleIsShownBeforeAskingAgain() {
        shadowOf(activity.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, true)
        show(DemoModule.Camera)

        tapRequestButton()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(DemoModule.Camera.rationaleRes)).assertIsDisplayed()
        assertNull("no se pide hasta que el usuario confirme", shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun confirmingTheRationaleRequestsThePermissions() {
        shadowOf(activity.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, true)
        show(DemoModule.Camera)
        tapRequestButton()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.action_continue)).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            shadowOf(activity).lastRequestedPermission!!.requestedPermissions.toList(),
        )
        composeRule.onNodeWithText(string(DemoModule.Camera.rationaleRes)).assertDoesNotExist()
    }

    @Test
    fun cancellingTheRationaleClosesItWithoutRequestingAnything() {
        shadowOf(activity.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, true)
        show(DemoModule.Camera)
        tapRequestButton()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.action_cancel)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(DemoModule.Camera.rationaleRes)).assertDoesNotExist()
        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    // --- denegación permanente ------------------------------------------------------------------------

    @Test
    fun aPermanentlyDeniedPermissionIsShownAsSuch() {
        activity.markRequested(AppPermission.Camera, AppPermission.RecordAudio)
        show(DemoModule.Camera)

        composeRule.onNodeWithText(row(AppPermission.Camera, R.string.state_permanently_denied)).assertIsDisplayed()
    }

    @Test
    fun whenOnlyPermanentlyDeniedPermissionsAreMissingTheUserIsSentToSettings() {
        activity.markRequested(AppPermission.Camera, AppPermission.RecordAudio)
        show(DemoModule.Camera)

        tapRequestButton()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.settings_dialog_message)).assertIsDisplayed()
        assertNull("no se vuelve a pedir un permiso bloqueado", shadowOf(activity).lastRequestedPermission)

        composeRule.onNodeWithText(string(R.string.action_open_settings)).performClick()
        composeRule.waitForIdle()

        val intent: Intent = shadowOf(activity.application).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${activity.packageName}", intent.dataString)
    }

    @Test
    fun cancellingTheSettingsDialogClosesItWithoutOpeningSettings() {
        activity.markRequested(AppPermission.Camera, AppPermission.RecordAudio)
        show(DemoModule.Camera)
        tapRequestButton()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.action_cancel)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.settings_dialog_message)).assertDoesNotExist()
        assertNull(shadowOf(activity.application).nextStartedActivity)
    }
}
