package com.example.scopeinspector.feature.inspector

import androidx.annotation.StringRes
import com.example.scopeinspector.R
import java.security.MessageDigest
import java.util.Locale

/** Valor de una fila: texto tal cual (datos del dispositivo) o un texto traducible de la app. */
sealed interface RowValue {
    data class Plain(val text: String) : RowValue

    data class Localized(@StringRes val res: Int, val args: List<Any> = emptyList()) : RowValue
}

data class InspectorRow(@StringRes val labelRes: Int, val value: RowValue)

data class InspectorSection(
    val id: String,
    @StringRes val titleRes: Int,
    /** Para qué puede usar esto una app (análisis, publicidad, huella digital...). */
    @StringRes val whyRes: Int,
    val rows: List<InspectorRow>,
)

/**
 * Convierte la instantánea en las secciones que se muestran, al estilo de Browser Scope Inspector:
 * hardware, sistema, pantalla, red, región y preferencias, e identificadores y huella digital.
 */
object InspectorReport {

    fun sections(snapshot: DeviceSnapshot, locale: Locale = Locale.getDefault()): List<InspectorSection> = listOf(
        hardware(snapshot.hardware, locale),
        system(snapshot.system),
        display(snapshot.display, locale),
        network(snapshot.network),
        region(snapshot.region, locale),
        identifiers(snapshot, locale),
    )

    private fun hardware(h: HardwareInfo, locale: Locale) = InspectorSection(
        id = "hardware",
        titleRes = R.string.inspector_hardware_title,
        whyRes = R.string.inspector_hardware_why,
        rows = listOfNotNull(
            row(R.string.inspector_device, "${h.manufacturer} ${h.model}".trim()),
            row(R.string.inspector_brand_device, "${h.brand} · ${h.device}"),
            h.socModel?.let { row(R.string.inspector_soc, it) },
            row(R.string.inspector_abis, h.supportedAbis.joinToString(", ").ifEmpty { NO_VALUE }),
            row(R.string.inspector_cpu_cores, h.cpuCores.toString()),
            InspectorRow(
                R.string.inspector_ram,
                RowValue.Localized(
                    R.string.inspector_used_of_total,
                    listOf(
                        formatBytes(h.totalRamBytes - h.availableRamBytes, locale),
                        formatBytes(h.totalRamBytes, locale),
                    ),
                ),
            ),
            InspectorRow(
                R.string.inspector_storage,
                RowValue.Localized(
                    R.string.inspector_free_of_total,
                    listOf(formatBytes(h.storageFreeBytes, locale), formatBytes(h.storageTotalBytes, locale)),
                ),
            ),
            InspectorRow(R.string.inspector_battery, batteryValue(h, locale)),
        ),
    )

    private fun batteryValue(h: HardwareInfo, locale: Locale): RowValue {
        val percent = h.batteryPercent ?: return RowValue.Localized(R.string.inspector_unknown)
        val text = if (h.charging ==
            true
        ) {
            R.string.inspector_battery_charging
        } else {
            R.string.inspector_battery_discharging
        }
        val temperature = h.batteryTemperatureC?.let { String.format(locale, "%.1f °C", it) } ?: NO_VALUE
        return RowValue.Localized(text, listOf(percent, temperature))
    }

    private fun system(s: SystemInfo) = InspectorSection(
        id = "system",
        titleRes = R.string.inspector_system_title,
        whyRes = R.string.inspector_system_why,
        rows = listOf(
            InspectorRow(
                R.string.inspector_android_version,
                RowValue.Localized(R.string.inspector_android_value, listOf(s.androidRelease, s.sdkInt)),
            ),
            row(R.string.inspector_security_patch, s.securityPatch.ifEmpty { NO_VALUE }),
            row(R.string.inspector_build_fingerprint, s.buildFingerprint),
            row(R.string.inspector_uptime, formatDuration(s.uptimeMillis)),
        ),
    )

    private fun display(d: DisplayInfo, locale: Locale) = InspectorSection(
        id = "display",
        titleRes = R.string.inspector_display_title,
        whyRes = R.string.inspector_display_why,
        rows = listOf(
            row(R.string.inspector_resolution, "${d.widthPx} × ${d.heightPx} px"),
            row(
                R.string.inspector_density,
                String.format(locale, "%d dpi (×%.2f)", d.densityDpi, d.density),
            ),
            row(R.string.inspector_size_dp, "${dp(d.widthPx, d.density)} × ${dp(d.heightPx, d.density)} dp"),
            row(R.string.inspector_refresh_rate, String.format(locale, "%.0f Hz", d.refreshRateHz)),
            InspectorRow(
                R.string.inspector_orientation,
                localized(if (d.portrait) R.string.inspector_portrait else R.string.inspector_landscape),
            ),
            InspectorRow(R.string.inspector_hdr, yesNo(d.hdr)),
            row(R.string.inspector_font_scale, String.format(locale, "×%.2f", d.fontScale)),
            InspectorRow(R.string.inspector_dark_mode, yesNo(d.darkMode)),
        ),
    )

    private fun network(n: NetworkInfo) = InspectorSection(
        id = "network",
        titleRes = R.string.inspector_network_title,
        whyRes = R.string.inspector_network_why,
        rows = buildList {
            add(InspectorRow(R.string.inspector_connection, localized(transportRes(n.transport))))
            if (n.transport != Transport.None) {
                add(InspectorRow(R.string.inspector_internet_validated, yesNo(n.validated)))
                add(InspectorRow(R.string.inspector_metered, yesNo(n.metered)))
                add(InspectorRow(R.string.inspector_vpn, yesNo(n.vpn)))
                add(row(R.string.inspector_bandwidth, bandwidth(n.downstreamKbps, n.upstreamKbps)))
            }
            add(
                InspectorRow(
                    R.string.inspector_data_saver,
                    n.dataSaver?.let(::yesNo) ?: localized(R.string.inspector_unknown),
                ),
            )
            add(row(R.string.inspector_operator, n.operatorName.ifEmpty { NO_VALUE }))
            add(row(R.string.inspector_sim_country, n.simCountryIso.uppercase(Locale.ROOT).ifEmpty { NO_VALUE }))
        },
    )

    private fun region(r: RegionInfo, locale: Locale) = InspectorSection(
        id = "region",
        titleRes = R.string.inspector_region_title,
        whyRes = R.string.inspector_region_why,
        rows = listOf(
            row(R.string.inspector_languages, r.locales.joinToString(", ").ifEmpty { NO_VALUE }),
            row(R.string.inspector_time_zone, r.timeZoneId),
            InspectorRow(R.string.inspector_24h, yesNo(r.use24HourFormat)),
            InspectorRow(R.string.inspector_screen_reader, yesNo(r.screenReaderOn)),
            row(R.string.inspector_animation_scale, String.format(locale, "×%.1f", r.animationScale)),
        ),
    )

    private fun identifiers(snapshot: DeviceSnapshot, locale: Locale): InspectorSection {
        val ids = snapshot.identifiers
        return InspectorSection(
            id = "identifiers",
            titleRes = R.string.inspector_identifiers_title,
            whyRes = R.string.inspector_identifiers_why,
            rows = listOf(
                row(R.string.inspector_android_id, ids.androidId.ifEmpty { NO_VALUE }),
                InspectorRow(R.string.inspector_developer_options, yesNo(ids.developerOptions)),
                InspectorRow(R.string.inspector_usb_debugging, yesNo(ids.usbDebugging)),
                InspectorRow(
                    R.string.inspector_visible_apps,
                    RowValue.Localized(R.string.inspector_visible_apps_value, listOf(ids.visibleLauncherApps)),
                ),
                InspectorRow(
                    R.string.inspector_sensors,
                    RowValue.Localized(R.string.inspector_sensors_value, listOf(ids.sensorNames.size)),
                ),
                row(R.string.inspector_fingerprint, fingerprint(snapshot, locale)),
            ),
        )
    }

    /**
     * Huella combinada: un hash de los datos que cambian poco entre reinicios y que no dependen de un
     * identificador explícito. Dos apps distintas obtienen la misma huella en el mismo móvil, aunque su
     * ANDROID_ID sea diferente: eso es lo que hace el fingerprinting.
     */
    fun fingerprint(snapshot: DeviceSnapshot, locale: Locale = Locale.ROOT): String {
        val h = snapshot.hardware
        val d = snapshot.display
        val stable = listOf(
            h.manufacturer, h.model, h.socModel.orEmpty(), h.supportedAbis.joinToString(","), h.cpuCores,
            // La RAM se redondea a GB: la que el sistema reserva varía un poco entre arranques.
            ramBucketGb(h.totalRamBytes), snapshot.system.buildFingerprint,
            d.widthPx.coerceAtMost(d.heightPx), d.widthPx.coerceAtLeast(d.heightPx), d.densityDpi,
            snapshot.region.locales.joinToString(","), snapshot.region.timeZoneId,
            snapshot.identifiers.sensorNames.sorted().joinToString(","),
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(stable.toByteArray(Charsets.UTF_8))
        return digest.take(FINGERPRINT_BYTES).joinToString("") { String.format(locale, "%02x", it) }
    }

    fun ramBucketGb(bytes: Long): Long = (bytes + GB / 2) / GB

    fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
        val value = bytes.coerceAtLeast(0).toDouble()
        return when {
            value >= GB -> String.format(locale, "%.1f GB", value / GB)
            value >= MB -> String.format(locale, "%.0f MB", value / MB)
            value >= KB -> String.format(locale, "%.0f KB", value / KB)
            else -> "${value.toLong()} B"
        }
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis.coerceAtLeast(0) / 60_000
        val days = totalMinutes / (24 * 60)
        val hours = totalMinutes / 60 % 24
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}min"
            hours > 0 -> "${hours}h ${minutes}min"
            else -> "${minutes}min"
        }
    }

    fun bandwidth(downKbps: Int?, upKbps: Int?): String {
        if (downKbps == null && upKbps == null) return NO_VALUE
        fun mbps(kbps: Int?) = kbps?.let { (it / 1000).toString() } ?: "?"
        return "↓ ${mbps(downKbps)} / ↑ ${mbps(upKbps)} Mbps"
    }

    @StringRes
    fun transportRes(transport: Transport): Int = when (transport) {
        Transport.Wifi -> R.string.transport_wifi
        Transport.Cellular -> R.string.transport_cellular
        Transport.Ethernet -> R.string.transport_ethernet
        Transport.Vpn -> R.string.transport_vpn
        Transport.Bluetooth -> R.string.transport_bluetooth
        Transport.Other -> R.string.transport_other
        Transport.None -> R.string.transport_none
    }

    private fun dp(px: Int, density: Float): Int = if (density > 0f) (px / density).toInt() else px

    private fun row(@StringRes labelRes: Int, text: String) = InspectorRow(labelRes, RowValue.Plain(text))

    private fun localized(@StringRes res: Int) = RowValue.Localized(res)

    private fun yesNo(value: Boolean) = localized(if (value) R.string.inspector_yes else R.string.inspector_no)

    const val NO_VALUE = "—"
    private const val FINGERPRINT_BYTES = 8
    private const val KB = 1024.0
    private const val MB = KB * 1024
    private const val GB = 1024L * 1024 * 1024
}
