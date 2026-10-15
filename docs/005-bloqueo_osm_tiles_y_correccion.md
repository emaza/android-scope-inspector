# Bloqueo de OpenStreetMap en el mapa: causa y corrección

> Relacionado con el Módulo de Ubicación del [plan de fases](004-plan_de_fases_detallado.md).

## Síntoma

En el mapa del módulo de Ubicación aparecía, repetida en cada recuadro, la imagen:

> **403 – Access blocked.** App is not following the tile usage policy of OpenStreetMap's volunteer-run servers: osm.wiki/blocked

## Causa

OpenStreetMap bloquea a la app por el **User-Agent** que envía. Lo puse yo en `RouteMap.kt`: `userAgentValue = context.packageName`, que vale `com.example.scopeinspector`.

El servidor de tiles de OSM identifica a las apps por ese encabezado. A las que no cumplen la política no les sirve el mapa: devuelve una imagen de 256×256 con el texto "403 – Access blocked" (con HTTP 200). Esa imagen repetida en cada tile es lo que se veía en el móvil.

## Prueba

Se pidió el mismo tile (`/3/4/2.png`) con distintos User-Agent. Una petición suelta por cada uno, no descarga masiva.

| User-Agent | Resultado |
|---|---|
| `com.example.scopeinspector` (el que usaba la app) | **Bloqueado** (imagen de 6.987 bytes) |
| `osmdroid` (el por defecto de la librería) | Bloqueado |
| `Dalvik/2.1.0 (Linux; U; Android 14)` | Bloqueado |
| `com.example.scopeinspector/0.1 (https://github.com/emaza/android-scope-inspector)` | Bloqueado |
| `ScopeInspector/0.1` | **Mapa real** |
| `ScopeInspector/0.1 (https://github.com/emaza/android-scope-inspector)` | **Mapa real**, con cabeceras de caché correctas |

Lo que lo desencadena es el prefijo **`com.example`**: es el nombre de paquete de plantilla que usan miles de proyectos de ejemplo. Con ese prefijo se bloquea aunque se añada un contacto. Un nombre propio de la app sí pasa. El bloqueo no depende de la IP ni de la red, porque la prueba se hizo desde otra máquina distinta del móvil.

La política exige un User-Agent claro y único que nombre la app, con un contacto opcional. También exige no usar el que trae la librería por defecto ni suplantar a otra app. No menciona osmdroid ni Android, pero esas reglas valen para cualquier app.

## Corrección aplicada (rama `fase-2-ubicacion`)

1. **User-Agent propio:** `ScopeInspector/<versión> (https://github.com/emaza/android-scope-inspector)`, construido en `TileUsagePolicy.userAgent(...)` con `BuildConfig.VERSION_NAME`.
2. **Atribución visible:** "© OpenStreetMap contributors" abajo a la derecha del mapa; al tocarla abre `https://www.openstreetmap.org/copyright`.
3. **Caché de tiles nueva:** carpeta `osmdroid-v2` en la caché de la app, y borrado de la carpeta antigua `osmdroid`, que podía contener tiles de bloqueo ya guardados.

Un test unitario (`TileUsagePolicyTest`) comprueba el formato: nombre de la app, versión y contacto, que no empiece por `com.example` ni por `osmdroid`, y que las URLs sean HTTPS. No puede comprobar que OSM acepte la app, solo que el formato sea el correcto.

## Cómo comprobarlo en el móvil

Instalar el APK del run de CI de la rama y abrir el módulo de Ubicación. El mapa debe verse con calles y países, y no como un recuadro que dice "Access blocked". Después, "Mi posición ahora" debe colocar el marcador "Estás aquí".

## Pendiente

- ~~El `applicationId` sigue siendo `com.example.scopeinspector`.~~ Resuelto en la rama `fase-versionado`: ahora es `io.github.emaza.scopeinspector` (ver [007](007-versionado_semantico.md)). El mapa no dependía de él, pero un `com.example.*` no se puede publicar en Google Play.
- OSM puede bloquear sin aviso si la app hace descargas masivas o no respeta la caché. La demo solo carga los tiles que el usuario está viendo.

## Fuentes

- [Tile Usage Policy – OSM Foundation](https://operations.osmfoundation.org/policies/tiles/)
- [Blocked – OpenStreetMap Wiki](https://wiki.openstreetmap.org/wiki/Blocked)
- [Blocked applications – OpenStreetMap Wiki](https://wiki.openstreetmap.org/wiki/Blocked_applications)
