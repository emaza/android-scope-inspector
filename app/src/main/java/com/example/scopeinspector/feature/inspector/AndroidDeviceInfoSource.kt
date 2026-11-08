package com.example.scopeinspector.feature.inspector

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService
import java.util.TimeZone

/**
 * Lee los datos con las APIs públicas de Android. Ninguna de estas llamadas muestra un diálogo de
 * permisos: solo usan `ACCESS_NETWORK_STATE`, un permiso normal que se concede al instalar.
 */
class AndroidDeviceInfoSource(context: Context) : DeviceInfoSource {
    private val context = context.applicationContext

    override fun snapshot(): DeviceSnapshot = DeviceSnapshot(
        hardware = hardware(),
        system = SystemInfo(
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            buildFingerprint = Build.FINGERPRINT.orEmpty(),
            uptimeMillis = SystemClock.elapsedRealtime(),
        ),
        display = display(),
        network = network(),
        region = region(),
        identifiers = identifiers(),
    )

    private fun hardware(): HardwareInfo {
        val memory = ActivityManager.MemoryInfo()
        context.getSystemService<ActivityManager>()?.getMemoryInfo(memory)
        val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        // Un broadcast "sticky": registrarse con receptor nulo devuelve el último estado sin escuchar nada.
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val temperature = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        return HardwareInfo(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim().takeUnless {
                    it.isEmpty() ||
                        it == "unknown unknown"
                }
            } else {
                null
            },
            supportedAbis = Build.SUPPORTED_ABIS.orEmpty().toList(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamBytes = memory.totalMem,
            availableRamBytes = memory.availMem,
            storageTotalBytes = stat?.totalBytes ?: 0L,
            storageFreeBytes = stat?.availableBytes ?: 0L,
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else null,
            charging = if (status < 0) {
                null
            } else {
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            },
            batteryTemperatureC = if (temperature == Int.MIN_VALUE) null else temperature / 10f,
        )
    }

    private fun display(): DisplayInfo {
        val metrics = context.resources.displayMetrics
        val configuration = context.resources.configuration
        val windowManager = context.getSystemService<WindowManager>()
        val (width, height) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            metrics.widthPixels to metrics.heightPixels
        }
        @Suppress("DEPRECATION") // Display.getRefreshRate sigue siendo la forma sencilla en todas las versiones.
        val defaultDisplay = windowManager?.defaultDisplay
        return DisplayInfo(
            widthPx = width,
            heightPx = height,
            densityDpi = metrics.densityDpi,
            density = metrics.density,
            refreshRateHz = defaultDisplay?.refreshRate ?: 0f,
            fontScale = configuration.fontScale,
            darkMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
            portrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE,
            hdr = configuration.isScreenHdr,
        )
    }

    private fun network(): NetworkInfo {
        val connectivity = context.getSystemService<ConnectivityManager>()
        val capabilities = connectivity?.let { it.getNetworkCapabilities(it.activeNetwork) }
        val telephony = context.getSystemService<TelephonyManager>()
        return NetworkInfo(
            transport = capabilities?.let(::transportOf) ?: Transport.None,
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            metered = connectivity?.isActiveNetworkMetered == true,
            vpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            downstreamKbps = capabilities?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
            upstreamKbps = capabilities?.linkUpstreamBandwidthKbps?.takeIf { it > 0 },
            dataSaver = connectivity?.restrictBackgroundStatus?.let {
                it == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
            },
            operatorName = telephony?.networkOperatorName.orEmpty(),
            simCountryIso = telephony?.simCountryIso.orEmpty(),
        )
    }

    private fun transportOf(capabilities: NetworkCapabilities): Transport = when {
        // La VPN se comprueba antes: una red VPN también declara el transporte de debajo.
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> Transport.Vpn

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.Wifi

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.Cellular

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Transport.Ethernet

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> Transport.Bluetooth

        else -> Transport.Other
    }

    private fun region(): RegionInfo {
        val locales = context.resources.configuration.locales
        return RegionInfo(
            locales = (0 until locales.size()).map { locales[it].toLanguageTag() },
            timeZoneId = TimeZone.getDefault().id,
            use24HourFormat = DateFormat.is24HourFormat(context),
            screenReaderOn = context.getSystemService<AccessibilityManager>()?.isTouchExplorationEnabled == true,
            animationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ),
        )
    }

    // ANDROID_ID es el identificador que Android ofrece a todas las apps sin permiso; lint avisa
    // precisamente porque se puede usar para rastrear, que es lo que esta pantalla quiere enseñar.
    // Tampoco se declara `<queries>`: se muestra cuántas apps ve una app normal con la visibilidad de
    // paquetes de Android 11+, que es justo lo que filtra.
    @SuppressLint("HardwareIds", "QueryPermissionsNeeded")
    private fun identifiers(): IdentifierInfo {
        val resolver = context.contentResolver
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return IdentifierInfo(
            androidId = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID).orEmpty(),
            developerOptions = Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1,
            usbDebugging = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1,
            visibleLauncherApps = context.packageManager.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .size,
            sensorNames = context.getSystemService<SensorManager>()
                ?.getSensorList(Sensor.TYPE_ALL)
                .orEmpty()
                .map { it.name },
        )
    }
}
