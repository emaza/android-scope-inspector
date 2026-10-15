package com.example.scopeinspector.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.scopeinspector.R
import com.example.scopeinspector.permissions.AppPermission
import com.example.scopeinspector.permissions.PermissionState
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionStatusListTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun row(permission: AppPermission, stateLabel: Int): String =
        string(R.string.permission_row, string(permission.labelRes), string(stateLabel))

    private fun show(states: Map<AppPermission, PermissionState>) {
        composeRule.setContent { ScopeInspectorTheme { PermissionStatusList(states = states) } }
    }

    @Test
    fun aGrantedRuntimePermissionIsShownAsGrantedWithoutExplanation() {
        show(mapOf(AppPermission.Camera to PermissionState.Granted))

        composeRule.onNodeWithText(row(AppPermission.Camera, R.string.state_granted)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.install_permission_explanation)).assertDoesNotExist()
    }

    @Test
    fun aGrantedInstallTimePermissionIsShownAsGrantedAtInstallWithTheExplanation() {
        show(mapOf(AppPermission.UseBiometric to PermissionState.Granted))

        composeRule.onNodeWithText(row(AppPermission.UseBiometric, R.string.state_granted_at_install)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.install_permission_explanation)).assertIsDisplayed()
    }

    @Test
    fun aNotApplicableInstallTimePermissionHasNoExplanation() {
        show(mapOf(AppPermission.UseBiometric to PermissionState.NotApplicable))

        composeRule.onNodeWithText(row(AppPermission.UseBiometric, R.string.state_not_applicable)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.install_permission_explanation)).assertDoesNotExist()
    }

    @Test
    fun deniedAndPermanentlyDeniedPermissionsHaveTheirOwnText() {
        show(
            mapOf(
                AppPermission.Camera to PermissionState.Denied,
                AppPermission.RecordAudio to PermissionState.PermanentlyDenied,
            ),
        )

        composeRule.onNodeWithText(row(AppPermission.Camera, R.string.state_denied)).assertIsDisplayed()
        composeRule.onNodeWithText(row(AppPermission.RecordAudio, R.string.state_permanently_denied)).assertIsDisplayed()
    }

    @Test
    fun anEmptyListShowsNoRowsAndNoExplanation() {
        show(emptyMap())

        composeRule.onNodeWithText(string(R.string.install_permission_explanation)).assertDoesNotExist()
    }
}
