package com.example.scopeinspector.feature.location

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.scopeinspector.R
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Mapa de OpenStreetMap con un marcador en [current] y la ruta recorrida como línea.
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
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val youAreHere = stringResource(R.string.map_you_are_here)
    val zoomed = remember { BooleanArray(1) }
    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
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

    AndroidView(
        factory = { mapView },
        modifier = modifier,
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
}
