package com.example.scopeinspector.feature.inspector

import com.example.scopeinspector.R
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectorReportTest {

    private val spanish: Locale = Locale.forLanguageTag("es-ES")

    private fun sections(snapshot: DeviceSnapshot = fakeSnapshot()) = InspectorReport.sections(snapshot, spanish)

    private fun section(id: String, snapshot: DeviceSnapshot = fakeSnapshot()) = sections(snapshot).single {
        it.id == id
    }

    private fun value(id: String, label: Int, snapshot: DeviceSnapshot = fakeSnapshot()): RowValue? =
        section(id, snapshot).rows.singleOrNull { it.labelRes == label }?.value

    @Test
    fun hasTheSameSectionsAsBrowserScopeInAFixedOrder() {
        assertEquals(
            listOf("hardware", "system", "display", "network", "region", "identifiers"),
            sections().map { it.id },
        )
    }

    @Test
    fun everySectionExplainsWhyItMattersAndHasRows() {
        sections().forEach { assertTrue(it.id, it.rows.isNotEmpty()) }
        assertEquals(sections().size, sections().map { it.whyRes }.toSet().size)
    }

    @Test
    fun noLabelIsRepeatedWithinASection() {
        sections().forEach { section ->
            val labels = section.rows.map { it.labelRes }
            assertEquals(section.id, labels.size, labels.toSet().size)
        }
    }

    @Test
    fun hardwareShowsTheDeviceMemoryAndBattery() {
        assertEquals(RowValue.Plain("motorola edge 40 neo"), value("hardware", R.string.inspector_device))
        assertEquals(RowValue.Plain("8"), value("hardware", R.string.inspector_cpu_cores))
        assertEquals(
            RowValue.Localized(R.string.inspector_used_of_total, listOf("8,0 GB", "12,0 GB")),
            value("hardware", R.string.inspector_ram),
        )
        assertEquals(
            RowValue.Localized(R.string.inspector_battery_discharging, listOf(87, "31,5 °C")),
            value("hardware", R.string.inspector_battery),
        )
    }

    @Test
    fun aChargingBatteryUsesTheChargingText() {
        val snapshot =
            fakeSnapshot(hardware = fakeSnapshot().hardware.copy(charging = true, batteryTemperatureC = null))

        assertEquals(
            RowValue.Localized(R.string.inspector_battery_charging, listOf(87, InspectorReport.NO_VALUE)),
            value("hardware", R.string.inspector_battery, snapshot),
        )
    }

    @Test
    fun withoutABatteryReadingTheBatteryIsUnknown() {
        val snapshot = fakeSnapshot(hardware = fakeSnapshot().hardware.copy(batteryPercent = null))

        assertEquals(
            RowValue.Localized(R.string.inspector_unknown),
            value("hardware", R.string.inspector_battery, snapshot),
        )
    }

    @Test
    fun theSocRowOnlyAppearsWhenAndroidExposesIt() {
        val older = fakeSnapshot(hardware = fakeSnapshot().hardware.copy(socModel = null))

        assertEquals(RowValue.Plain("Mediatek MT6879"), value("hardware", R.string.inspector_soc))
        assertNull(value("hardware", R.string.inspector_soc, older))
    }

    @Test
    fun displayShowsPixelsDensityAndLogicalSize() {
        assertEquals(RowValue.Plain("1080 × 2400 px"), value("display", R.string.inspector_resolution))
        assertEquals(RowValue.Plain("400 dpi (×2,50)"), value("display", R.string.inspector_density))
        assertEquals(RowValue.Plain("432 × 960 dp"), value("display", R.string.inspector_size_dp))
        assertEquals(RowValue.Plain("144 Hz"), value("display", R.string.inspector_refresh_rate))
        assertEquals(RowValue.Localized(R.string.inspector_yes), value("display", R.string.inspector_dark_mode))
    }

    @Test
    fun aLandscapeScreenSaysSo() {
        val snapshot = fakeSnapshot(display = fakeSnapshot().display.copy(portrait = false))

        assertEquals(
            RowValue.Localized(R.string.inspector_landscape),
            value("display", R.string.inspector_orientation, snapshot),
        )
    }

    @Test
    fun networkShowsTheTransportAndTheSimCountryInCapitals() {
        assertEquals(RowValue.Localized(R.string.transport_wifi), value("network", R.string.inspector_connection))
        assertEquals(RowValue.Plain("↓ 300 / ↑ 50 Mbps"), value("network", R.string.inspector_bandwidth))
        assertEquals(RowValue.Plain("ES"), value("network", R.string.inspector_sim_country))
    }

    @Test
    fun offlineHidesTheRowsThatOnlyMakeSenseWithAConnection() {
        val offline =
            fakeSnapshot(
                network = fakeSnapshot().network.copy(
                    transport = Transport.None,
                    operatorName = "",
                    simCountryIso = "",
                    dataSaver = null,
                ),
            )

        assertEquals(
            RowValue.Localized(R.string.transport_none),
            value("network", R.string.inspector_connection, offline),
        )
        assertNull(value("network", R.string.inspector_vpn, offline))
        assertNull(value("network", R.string.inspector_bandwidth, offline))
        assertEquals(RowValue.Plain(InspectorReport.NO_VALUE), value("network", R.string.inspector_operator, offline))
        assertEquals(
            RowValue.Localized(R.string.inspector_unknown),
            value("network", R.string.inspector_data_saver, offline),
        )
    }

    @Test
    fun regionListsLanguagesInPreferenceOrder() {
        assertEquals(RowValue.Plain("es-ES, en-US"), value("region", R.string.inspector_languages))
        assertEquals(RowValue.Plain("Europe/Madrid"), value("region", R.string.inspector_time_zone))
    }

    @Test
    fun identifiersShowAndroidIdAndTheCombinedFingerprint() {
        assertEquals(RowValue.Plain("a1b2c3d4e5f60789"), value("identifiers", R.string.inspector_android_id))
        assertEquals(
            RowValue.Plain(InspectorReport.fingerprint(fakeSnapshot())),
            value("identifiers", R.string.inspector_fingerprint),
        )
        assertEquals(
            RowValue.Localized(R.string.inspector_sensors_value, listOf(3)),
            value("identifiers", R.string.inspector_sensors),
        )
    }

    // --- huella digital --------------------------------------------------------------------------------

    @Test
    fun theFingerprintIsSixteenHexCharactersAndDeterministic() {
        val fingerprint = InspectorReport.fingerprint(fakeSnapshot())

        assertTrue(fingerprint, fingerprint.matches(Regex("[0-9a-f]{16}")))
        assertEquals(fingerprint, InspectorReport.fingerprint(fakeSnapshot()))
    }

    @Test
    fun theFingerprintIgnoresWhatChangesOrWhatDiffersBetweenApps() {
        val base = fakeSnapshot()
        val later = base.copy(
            hardware = base.hardware.copy(
                availableRamBytes = 1L * GIB,
                totalRamBytes = 12L * GIB - 50_000_000,
                batteryPercent = 12,
                charging = true,
                storageFreeBytes = 1L,
            ),
            system = base.system.copy(uptimeMillis = 99 * HOUR),
            display = base.display.copy(portrait = false, widthPx = 2400, heightPx = 1080, darkMode = false),
            network = base.network.copy(transport = Transport.Cellular),
            identifiers = base.identifiers.copy(
                androidId = "otra-app",
                sensorNames = base.identifiers.sensorNames.reversed(),
            ),
        )

        assertEquals(InspectorReport.fingerprint(base), InspectorReport.fingerprint(later))
    }

    @Test
    fun theFingerprintChangesWithAnotherDeviceOrSetup() {
        val base = fakeSnapshot()
        val fingerprint = InspectorReport.fingerprint(base)

        listOf(
            base.copy(hardware = base.hardware.copy(model = "edge 50")),
            base.copy(display = base.display.copy(densityDpi = 420)),
            base.copy(region = base.region.copy(timeZoneId = "Atlantic/Canary")),
            base.copy(region = base.region.copy(locales = listOf("en-US", "es-ES"))),
            base.copy(identifiers = base.identifiers.copy(sensorNames = listOf("Otro sensor"))),
        ).forEach { assertNotEquals(fingerprint, InspectorReport.fingerprint(it)) }
    }

    // --- formato ------------------------------------------------------------------------------------------

    @Test
    fun ramIsRoundedToTheNearestGigabyte() {
        assertEquals(12, InspectorReport.ramBucketGb(12L * GIB - 300L * 1024 * 1024))
        assertEquals(8, InspectorReport.ramBucketGb(7L * GIB + 600L * 1024 * 1024))
    }

    @Test
    fun bytesUseTheLargestUnit() {
        assertEquals("512 B", InspectorReport.formatBytes(512, spanish))
        assertEquals("2 KB", InspectorReport.formatBytes(2048, spanish))
        assertEquals("300 MB", InspectorReport.formatBytes(300L * 1024 * 1024, spanish))
        assertEquals("1,5 GB", InspectorReport.formatBytes(3L * GIB / 2, spanish))
        assertEquals("0 B", InspectorReport.formatBytes(-5, spanish))
    }

    @Test
    fun durationsShowDaysHoursAndMinutes() {
        assertEquals("0min", InspectorReport.formatDuration(59_000))
        assertEquals("45min", InspectorReport.formatDuration(45 * MINUTE))
        assertEquals("3h 5min", InspectorReport.formatDuration(3 * HOUR + 5 * MINUTE))
        assertEquals("2d 1h 0min", InspectorReport.formatDuration(49 * HOUR))
    }

    @Test
    fun bandwidthToleratesMissingValues() {
        assertEquals(InspectorReport.NO_VALUE, InspectorReport.bandwidth(null, null))
        assertEquals("↓ 20 / ↑ ? Mbps", InspectorReport.bandwidth(20_000, null))
    }

    @Test
    fun everyTransportHasItsOwnText() {
        val texts = Transport.entries.map(InspectorReport::transportRes)
        assertEquals(texts.size, texts.toSet().size)
        assertFalse(texts.contains(0))
    }
}
