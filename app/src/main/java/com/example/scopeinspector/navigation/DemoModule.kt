package com.example.scopeinspector.navigation

import androidx.annotation.StringRes
import com.example.scopeinspector.R
import com.example.scopeinspector.permissions.AppPermission

/** Los cuatro módulos de la demo y los permisos que cada uno justifica. */
enum class DemoModule(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    @StringRes val featureRes: Int,
    @StringRes val exampleRes: Int,
    @StringRes val howToTestRes: Int,
    @StringRes val rationaleRes: Int,
    val permissions: List<AppPermission>,
) {
    Location(
        route = "location",
        titleRes = R.string.module_location_title,
        summaryRes = R.string.module_location_summary,
        featureRes = R.string.module_location_feature,
        exampleRes = R.string.module_location_example,
        howToTestRes = R.string.module_location_how_to_test,
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
        featureRes = R.string.module_camera_feature,
        exampleRes = R.string.module_camera_example,
        howToTestRes = R.string.module_camera_how_to_test,
        rationaleRes = R.string.module_camera_rationale,
        permissions = listOf(AppPermission.Camera, AppPermission.RecordAudio),
    ),
    Biometric(
        route = "biometric",
        titleRes = R.string.module_biometric_title,
        summaryRes = R.string.module_biometric_summary,
        featureRes = R.string.module_biometric_feature,
        exampleRes = R.string.module_biometric_example,
        howToTestRes = R.string.module_biometric_how_to_test,
        rationaleRes = R.string.module_biometric_rationale,
        permissions = listOf(AppPermission.UseBiometric),
    ),
    Background(
        route = "background",
        titleRes = R.string.module_background_title,
        summaryRes = R.string.module_background_summary,
        featureRes = R.string.module_background_feature,
        exampleRes = R.string.module_background_example,
        howToTestRes = R.string.module_background_how_to_test,
        rationaleRes = R.string.module_background_rationale,
        permissions = listOf(AppPermission.PostNotifications),
    ),
}
