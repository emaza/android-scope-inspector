package com.example.scopeinspector.feature.inspector

/** Instantánea de ejemplo con valores reconocibles; los tests cambian solo lo que les importa. */
fun fakeSnapshot(
    hardware: HardwareInfo = HardwareInfo(
        manufacturer = "motorola",
        model = "edge 40 neo",
        brand = "motorola",
        device = "manaus",
        socModel = "Mediatek MT6879",
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        cpuCores = 8,
        totalRamBytes = 12L * GIB,
        availableRamBytes = 4L * GIB,
        storageTotalBytes = 256L * GIB,
        storageFreeBytes = 100L * GIB,
        batteryPercent = 87,
        charging = false,
        batteryTemperatureC = 31.5f,
    ),
    system: SystemInfo = SystemInfo(
        androidRelease = "15",
        sdkInt = 35,
        securityPatch = "2026-09-01",
        buildFingerprint = "motorola/manaus/manaus:15/V1/1:user/release-keys",
        uptimeMillis = 3 * HOUR + 5 * MINUTE,
    ),
    display: DisplayInfo = DisplayInfo(
        widthPx = 1080,
        heightPx = 2400,
        densityDpi = 400,
        density = 2.5f,
        refreshRateHz = 144f,
        fontScale = 1.15f,
        darkMode = true,
        portrait = true,
        hdr = true,
    ),
    network: NetworkInfo = NetworkInfo(
        transport = Transport.Wifi,
        validated = true,
        metered = false,
        vpn = false,
        downstreamKbps = 300_000,
        upstreamKbps = 50_000,
        dataSaver = false,
        operatorName = "Movistar",
        simCountryIso = "es",
    ),
    region: RegionInfo = RegionInfo(
        locales = listOf("es-ES", "en-US"),
        timeZoneId = "Europe/Madrid",
        use24HourFormat = true,
        screenReaderOn = false,
        animationScale = 1f,
    ),
    identifiers: IdentifierInfo = IdentifierInfo(
        androidId = "a1b2c3d4e5f60789",
        developerOptions = true,
        usbDebugging = false,
        visibleLauncherApps = 3,
        sensorNames = listOf("LSM6DSO Accelerometer", "LSM6DSO Gyroscope", "TMD3702 Light"),
    ),
) = DeviceSnapshot(hardware, system, display, network, region, identifiers)

const val GIB = 1024L * 1024 * 1024
const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
