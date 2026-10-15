package com.example.scopeinspector.feature.location

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.scopeinspector.BuildConfig
import com.example.scopeinspector.R
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val TILE_CACHE_DIR = "osmdroid-v2"
private const val OLD_TILE_CACHE_DIR = "osmdroid"

/**
 * Mapa de OpenStreetMap con un marcador en [current] y la ruta recorrida como línea.
 * Cumple la política de tiles de OSM: User-Agent propio ([TileUsagePolicy]) y atribución visible.
 * La caché de tiles vive en el almacenamiento propio de la app: no necesita permisos de almacenamiento.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun RouteMap(
    current: RoutePoint?,
    route: List<RoutePoint>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val youAreHere = stringResource(R.string.map_you_are_here)
    val zoomed = remember { BooleanArray(1) }
    val mapView = remember {
        // La caché anterior pudo guardar tiles "403 Access blocked" servidos con el User-Agent antiguo.
        File(context.cacheDir, OLD_TILE_CACHE_DIR).deleteRecursively()
        Configuration.getInstance().apply {
            userAgentValue = TileUsagePolicy.userAgent(BuildConfig.VERSION_NAME)
            osmdroidBasePath = File(context.cacheDir, TILE_CACHE_DIR)
            osmdroidTileCache = File(context.cacheDir, "$TILE_CACHE_DIR/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(2.5)
            controller.setCenter(GeoPoint(20.0, 0.0))
            // El mapa vive dentro de una columna con scroll: sin esto, arrastrar el mapa desplaza la pantalla.
            setOnTouchListener { view, _ ->
                view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { mapView.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { mapView.onPause() }
    DisposableEffect(mapView) { onDispose { mapView.onDetach() } }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize(),
            update = { map ->
                map.overlays.clear()
                if (route.size >= 2) {
                    map.overlays.add(
                        Polyline().apply {
                            setPoints(route.map { GeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = routeColor
                            outlinePaint.strokeWidth = 10f
                        },
                    )
                }
                if (current != null) {
                    val point = GeoPoint(current.latitude, current.longitude)
                    map.overlays.add(
                        Marker(map).apply {
                            position = point
                            title = youAreHere
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        },
                    )
                    if (!zoomed[0]) {
                        map.controller.setZoom(17.0)
                        zoomed[0] = true
                    }
                    map.controller.animateTo(point)
                }
                map.invalidate()
            },
        )
        // Atribución obligatoria de OpenStreetMap, siempre visible en la esquina inferior derecha.
        Text(
            text = stringResource(R.string.map_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .clickable { uriHandler.openUri(TileUsagePolicy.COPYRIGHT_URL) }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
