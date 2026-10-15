package com.example.scopeinspector.navigation

import androidx.annotation.StringRes
import com.example.scopeinspector.R
import com.example.scopeinspector.permissions.AppPermission

/** Los cuatro módulos de la demo y los permisos que cada uno justifica. */
enum class DemoModule(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    @StringRes val rationaleRes: Int,
    val permissions: List<AppPermission>,
) {
    Location(
        route = "location",
        titleRes = R.string.module_location_title,
        summaryRes = R.string.module_location_summary,
        rationaleRes = R.string.module_location_rationale,
        permissions = listOf(
            AppPermission.FineLocation,
            AppPermission.CoarseLocation,
            AppPermission.ActivityRecognition,
        ),
    ),
    Camera(
        route = "camera",
        titleRes = R.string.module_camera_title,
        summaryRes = R.string.module_camera_summary,
        rationaleRes = R.string.module_camera_rationale,
        permissions = listOf(AppPermission.Camera, AppPermission.RecordAudio),
    ),
    Biometric(
        route = "biometric",
        titleRes = R.string.module_biometric_title,
        summaryRes = R.string.module_biometric_summary,
        rationaleRes = R.string.module_biometric_rationale,
        permissions = listOf(AppPermission.UseBiometric),
    ),
    Background(
        route = "background",
        titleRes = R.string.module_background_title,
        summaryRes = R.string.module_background_summary,
        rationaleRes = R.string.module_background_rationale,
        permissions = listOf(AppPermission.PostNotifications),
    ),
}
