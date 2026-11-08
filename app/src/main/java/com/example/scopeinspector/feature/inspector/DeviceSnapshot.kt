package com.example.scopeinspector.feature.inspector

/**
 * Lo que cualquier app instalada puede leer del dispositivo **sin pedir ningún permiso en ejecución**
 * (solo permisos normales, concedidos al instalar). Son datos en bruto: la presentación está en
 * [InspectorReport], que es lógica pura y se prueba sin Android.
 */
data class DeviceSnapshot(
    val hardware: HardwareInfo,
    val system: SystemInfo,
    val display: DisplayInfo,
    val network: NetworkInfo,
    val region: RegionInfo,
    val identifiers: IdentifierInfo,
)

data class HardwareInfo(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    /** Chip (SoC); Android solo lo expone desde Android 12. */
    val socModel: String?,
    val supportedAbis: List<String>,
    val cpuCores: Int,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val storageTotalBytes: Long,
    val storageFreeBytes: Long,
    /** Nivel de batería en %, o null si no hay batería o no se puede leer. */
    val batteryPercent: Int?,
    val charging: Boolean?,
    /** Temperatura de la batería en °C. */
    val batteryTemperatureC: Float?,
)

data class SystemInfo(
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val buildFingerprint: String,
    val uptimeMillis: Long,
)

data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val refreshRateHz: Float,
    val fontScale: Float,
    val darkMode: Boolean,
    val portrait: Boolean,
    val hdr: Boolean,
)

enum class Transport { Wifi, Cellular, Ethernet, Vpn, Bluetooth, Other, None }

data class NetworkInfo(
    val transport: Transport,
    val validated: Boolean,
    val metered: Boolean,
    val vpn: Boolean,
    val downstreamKbps: Int?,
    val upstreamKbps: Int?,
    /** Ahorro de datos activado para esta app (restricción de datos en segundo plano). */
    val dataSaver: Boolean?,
    val operatorName: String,
    val simCountryIso: String,
)

data class RegionInfo(
    /** Etiquetas BCP 47 en orden de preferencia (p. ej. "es-ES", "en-US"). */
    val locales: List<String>,
    val timeZoneId: String,
    val use24HourFormat: Boolean,
    val screenReaderOn: Boolean,
    val animationScale: Float,
)

data class IdentifierInfo(
    /** `Settings.Secure.ANDROID_ID`: único por dispositivo, usuario y clave de firma de la app. */
    val androidId: String,
    val developerOptions: Boolean,
    val usbDebugging: Boolean,
    /** Apps con icono en el lanzador que esta app puede ver (filtrado por la visibilidad de paquetes). */
    val visibleLauncherApps: Int,
    val sensorNames: List<String>,
)

/** Fuente de la instantánea; en la app la da [AndroidDeviceInfoSource] y en los tests un fake. */
fun interface DeviceInfoSource {
    fun snapshot(): DeviceSnapshot
}
