package com.example.scopeinspector.feature.location

/**
 * Requisitos de la política de uso de tiles de OpenStreetMap
 * (https://operations.osmfoundation.org/policies/tiles/): un User-Agent propio que nombre la
 * app, con un contacto, y atribución visible. Los User-Agent genéricos o basados en el
 * nombre de paquete de plantilla (`com.example.*`) devuelven una imagen "403 Access blocked".
 */
object TileUsagePolicy {
    const val APP_NAME = "ScopeInspector"
    const val CONTACT_URL = "https://github.com/emaza/android-scope-inspector"
    const val COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"

    fun userAgent(versionName: String): String = "$APP_NAME/$versionName ($CONTACT_URL)"
}
