package com.example.scopeinspector.permissions

import android.content.Context
import androidx.core.content.edit

/** Recuerda qué permisos se han pedido ya, para poder detectar la denegación permanente. */
internal class RequestedPermissionsStore(context: Context) {
    private val prefs = context.getSharedPreferences("permission_requests", Context.MODE_PRIVATE)

    fun wasRequested(permission: AppPermission): Boolean = prefs.getBoolean(permission.manifestName, false)

    fun markRequested(permissions: Collection<AppPermission>) {
        prefs.edit {
            permissions.forEach { putBoolean(it.manifestName, true) }
        }
    }
}
