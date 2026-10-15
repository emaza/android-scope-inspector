package com.example.scopeinspector.shortcuts

/**
 * Reglas de los accesos directos, sin depender de clases de Android para poder probarlas en CI.
 *
 * Android no permite ejecutar nada en el momento de la instalación: el acceso directo en la
 * pantalla de inicio se ofrece la primera vez que se abre la app y el usuario lo acepta o no.
 */
object ShortcutPolicy {
    const val HOME_SHORTCUT_ID = "home"

    private const val DEEP_LINK_SCHEME = "scopeinspector"
    private const val DEEP_LINK_HOST = "module"

    /** Enlace interno de un módulo; lo usan los atajos y el grafo de navegación. */
    fun moduleUri(route: String): String = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$route"

    fun moduleShortcutId(route: String): String = "module-$route"

    /**
     * Ruta del módulo a la que apunta un enlace interno, o null si no es un enlace de módulo o la
     * ruta no está en [knownRoutes]. Solo se admiten rutas conocidas: un intent externo no puede
     * hacer navegar a pantallas arbitrarias.
     */
    fun moduleRoute(uri: String?, knownRoutes: Collection<String>): String? {
        val prefix = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/"
        val route = uri?.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
        return route?.takeIf { it in knownRoutes }
    }

    /** Se ofrece una sola vez, solo si el lanzador lo soporta y no hay ya uno fijado. */
    fun shouldOfferPin(supported: Boolean, alreadyOffered: Boolean, alreadyPinned: Boolean): Boolean =
        supported && !alreadyOffered && !alreadyPinned
}
