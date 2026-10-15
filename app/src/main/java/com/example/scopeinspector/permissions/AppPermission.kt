package com.example.scopeinspector.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import com.example.scopeinspector.R

/**
 * Permisos que la demo declara y consulta. [sinceSdk] es la primera versión de Android
 * en la que el permiso existe: por debajo de ella no se pide y el estado es
 * [PermissionState.NotApplicable]. [grantedAtInstall] marca los permisos normales, que
 * Android concede al instalar la app y nunca muestran un diálogo en ejecución.
 *
 * `InlinedApi` se suprime a propósito: las constantes de `Manifest.permission` son cadenas que el
 * compilador copia, y [sinceSdk] ya impide pedir un permiso en una versión donde no existe.
 */
@Suppress("InlinedApi")
enum class AppPermission(
    val manifestName: String,
    val sinceSdk: Int,
    @StringRes val labelRes: Int,
    val grantedAtInstall: Boolean = false,
) {
    FineLocation(Manifest.permission.ACCESS_FINE_LOCATION, 1, R.string.perm_fine_location),
    CoarseLocation(Manifest.permission.ACCESS_COARSE_LOCATION, 1, R.string.perm_coarse_location),
    ActivityRecognition(Manifest.permission.ACTIVITY_RECOGNITION, 29, R.string.perm_activity_recognition),
    Camera(Manifest.permission.CAMERA, 1, R.string.perm_camera),
    RecordAudio(Manifest.permission.RECORD_AUDIO, 1, R.string.perm_record_audio),
    UseBiometric(
        Manifest.permission.USE_BIOMETRIC,
        28,
        R.string.perm_use_biometric,
        grantedAtInstall = true,
    ),
    PostNotifications(Manifest.permission.POST_NOTIFICATIONS, 33, R.string.perm_post_notifications),
    ;

    fun isApplicable(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= sinceSdk

    /** Texto del estado; un permiso normal concedido se muestra como "Concedido al instalar". */
    @StringRes
    fun stateLabelRes(state: PermissionState): Int = if (grantedAtInstall && state == PermissionState.Granted) {
        R.string.state_granted_at_install
    } else {
        state.labelRes()
    }
}
