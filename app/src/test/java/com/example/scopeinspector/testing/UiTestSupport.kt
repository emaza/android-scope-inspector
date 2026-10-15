package com.example.scopeinspector.testing

import android.app.Application
import android.content.Context
import com.example.scopeinspector.permissions.AppPermission
import com.example.scopeinspector.permissions.RequestedPermissionsStore
import org.robolectric.Shadows.shadowOf

/** Concede permisos al "sistema" simulado por Robolectric. */
fun Application.grant(vararg permissions: String) {
    shadowOf(this).grantPermissions(*permissions)
}

/** Simula que la app ya pidió estos permisos antes (necesario para detectar la denegación permanente). */
fun Context.markRequested(vararg permissions: AppPermission) {
    RequestedPermissionsStore(this).markRequested(permissions.toList())
}
