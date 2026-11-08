package com.example.scopeinspector.feature.inspector

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** La fuente real sobre el Android simulado de Robolectric: comprueba que lee cada API sin fallar. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es-rES-w400dp-h800dp-port-night-xhdpi")
class AndroidDeviceInfoSourceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun snapshot() = AndroidDeviceInfoSource(context).snapshot()

    @Test
    fun readsTheScreenConfiguration() {
        val display = snapshot().display

        assertEquals(320, display.densityDpi)
        assertEquals(2f, display.density)
        assertTrue(display.portrait)
        assertTrue(display.darkMode)
        assertTrue(display.widthPx > 0 && display.heightPx > display.widthPx)
    }

    @Test
    fun readsTheSystemAndHardware() {
        val snapshot = snapshot()

        assertTrue(snapshot.system.sdkInt >= 26)
        assertTrue(snapshot.hardware.cpuCores >= 1)
        assertEquals(listOf("es-ES"), snapshot.region.locales)
    }

    @Test
    fun readsTheBatteryFromTheStickyBroadcast() {
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(
            Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_LEVEL, 45)
                .putExtra(BatteryManager.EXTRA_SCALE, 100)
                .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
                .putExtra(BatteryManager.EXTRA_TEMPERATURE, 287),
        )

        val hardware = snapshot().hardware

        assertEquals(45, hardware.batteryPercent)
        assertEquals(true, hardware.charging)
        assertEquals(28.7f, hardware.batteryTemperatureC!!, 0.01f)
    }

    @Test
    fun withoutBatteryInformationTheBatteryIsUnknown() {
        val hardware = snapshot().hardware

        assertNull(hardware.batteryPercent)
        assertNull(hardware.charging)
    }

    @Test
    fun readsAndroidIdAndTheDeveloperSettings() {
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "0123456789abcdef")
        Settings.Global.putInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)

        val ids = snapshot().identifiers

        assertEquals("0123456789abcdef", ids.androidId)
        assertTrue(ids.developerOptions)
        assertFalse(ids.usbDebugging)
    }

    @Test
    fun theReportCanBeBuiltFromARealSnapshot() {
        val sections = InspectorReport.sections(snapshot())

        assertEquals(6, sections.size)
    }
}
