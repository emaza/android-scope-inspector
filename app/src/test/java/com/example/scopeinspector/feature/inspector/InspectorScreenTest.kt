package com.example.scopeinspector.feature.inspector

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.scopeinspector.R
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Pantalla del inspector con una fuente falsa: los valores que se ven son los de [fakeSnapshot]. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h6000dp")
class InspectorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var snapshots = mutableListOf(fakeSnapshot())
    private var calls = 0
    private val source = DeviceInfoSource {
        calls++
        snapshots.removeAt(0).also { if (snapshots.isEmpty()) snapshots += it }
    }

    private fun string(id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun show(onBack: () -> Unit = {}) {
        composeRule.setContent { ScopeInspectorTheme { InspectorScreen(onBack = onBack, source = source) } }
        composeRule.waitForIdle()
    }

    @Test
    fun showsEverySectionWithItsExplanation() {
        show()

        InspectorReport.sections(fakeSnapshot()).forEach { section ->
            composeRule.onNodeWithText(string(section.titleRes)).assertIsDisplayed()
            composeRule.onNodeWithText(string(section.whyRes)).assertIsDisplayed()
        }
        composeRule.onNodeWithText(string(R.string.inspector_sensors_live_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.inspector_clipboard_title)).assertIsDisplayed()
    }

    @Test
    fun showsTheRealValuesOfTheSnapshot() {
        show()

        composeRule.onNodeWithText("motorola edge 40 neo").assertIsDisplayed()
        composeRule.onNodeWithText("Europe/Madrid").assertIsDisplayed()
        composeRule.onNodeWithText(InspectorReport.fingerprint(fakeSnapshot())).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.transport_wifi)).assertIsDisplayed()
    }

    @Test
    fun refreshReadsTheDeviceAgain() {
        val base = fakeSnapshot()
        snapshots = mutableListOf(base, base.copy(region = base.region.copy(timeZoneId = "Atlantic/Canary")))
        show()

        composeRule.onNodeWithText(string(R.string.action_refresh)).performClick()
        composeRule.waitForIdle()

        assertEquals(2, calls)
        composeRule.onNodeWithText("Atlantic/Canary").assertIsDisplayed()
    }

    @Test
    fun theBackButtonCallsOnBack() {
        var back = 0
        show(onBack = { back++ })

        composeRule.onNodeWithText(string(R.string.action_back)).performClick()

        assertEquals(1, back)
    }

    @Test
    fun theClipboardIsOnlyReadWhenTheUserAsks() {
        composeRule.activity.getSystemService<ClipboardManager>()!!
            .setPrimaryClip(ClipData.newPlainText("test", "texto copiado"))
        show()

        composeRule.onNodeWithText(string(R.string.inspector_clipboard_types, "text/plain")).assertIsDisplayed()
        composeRule.onNodeWithText("texto copiado").assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.action_read_clipboard)).performClick()
        composeRule.onNodeWithText("texto copiado").assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.action_hide)).performClick()
        composeRule.onNodeWithText("texto copiado").assertDoesNotExist()
    }
}
