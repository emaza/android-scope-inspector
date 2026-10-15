package com.example.scopeinspector.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import com.example.scopeinspector.R

/**
 * Permisos que la demo declara y consulta. [sinceSdk] es la primera versión de Android
 * en la que el permiso existe: por debajo de ella no se pide y el estado es
 * [PermissionState.NotApplicable].
 */
enum class AppPermission(
    val manifestName: String,
    val sinceSdk: Int,
    @StringRes val labelRes: Int,
) {
    FineLocation(Manifest.permission.ACCESS_FINE_LOCATION, 1, R.string.perm_fine_location),
    CoarseLocation(Manifest.permission.ACCESS_COARSE_LOCATION, 1, R.string.perm_coarse_location),
    ActivityRecognition(Manifest.permission.ACTIVITY_RECOGNITION, 29, R.string.perm_activity_recognition),
    Camera(Manifest.permission.CAMERA, 1, R.string.perm_camera),
    RecordAudio(Manifest.permission.RECORD_AUDIO, 1, R.string.perm_record_audio),
    UseBiometric(Manifest.permission.USE_BIOMETRIC, 28, R.string.perm_use_biometric),
    PostNotifications(Manifest.permission.POST_NOTIFICATIONS, 33, R.string.perm_post_notifications);

    fun isApplicable(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= sinceSdk
}
