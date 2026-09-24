package com.roteiro.app.ui.map

import android.graphics.RectF
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.RoteiroColors
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.cos
import kotlin.math.sin

/** Um lugar desenhado no mapa. */
data class MapPlace(
    val id: Long,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val count: Int,
    val selected: Boolean,
)

/** Para onde a câmera deve olhar. Mude o [key] para forçar um novo enquadramento. */
sealed interface CameraFocus {
    val key: Any
    /** Enquadra todos os lugares (e a posição do usuário). */
    data class FitAll(override val key: Any = Unit) : CameraFocus
    data class At(val lat: Double, val lng: Double, val zoom: Double = 15.5, override val key: Any = lat to lng) : CameraFocus
}

/**
 * Mapa MapLibre com os lugares do usuário.
 * Estilo vetorial gratuito do OpenFreeMap (dados © OpenStreetMap), sem chave de API.
 */
@Composable
fun ContextMap(
    places: List<MapPlace>,
    modifier: Modifier = Modifier,
    me: LatLng? = null,
    focus: CameraFocus = CameraFocus.FitAll(),
    interactive: Boolean = true,
    allRadii: Boolean = false,
    centerRadiusM: Int? = null,
    onPlaceClick: (Long) -> Unit = {},
    onMapClick: () -> Unit = {},
    onCameraIdle: (LatLng) -> Unit = {},
) {
    // Criar o MapView é pesado: espera a transição de tela terminar para não travar a animação.
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MAP_DELAY_MS)
        ready = true
    }
    Box(modifier.background(C.colors.surface)) {
        if (ready) {
            ContextMapView(places, Modifier.fillMaxSize(), me, focus, interactive, allRadii, centerRadiusM, onPlaceClick, onMapClick, onCameraIdle)
        }
    }
}

@Composable
private fun ContextMapView(
    places: List<MapPlace>,
    modifier: Modifier = Modifier,
    me: LatLng? = null,
    focus: CameraFocus = CameraFocus.FitAll(),
    interactive: Boolean = true,
    /** Desenha o raio de todos os lugares, não só do selecionado. */
    allRadii: Boolean = false,
    /** Tela "Novo lugar": raio desenhado em volta do centro da câmera. */
    centerRadiusM: Int? = null,
    onPlaceClick: (Long) -> Unit = {},
    onMapClick: () -> Unit = {},
    onCameraIdle: (LatLng) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val colors = C.colors
    val holder = remember { MapHolder() }
    val mapView = remember { MapView(context).also { it.onCreate(null) } }

    val placeClick by rememberUpdatedState(onPlaceClick)
    val mapClick by rememberUpdatedState(onMapClick)
    val cameraIdle by rememberUpdatedState(onCameraIdle)

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { mapView.onStart(); holder.started = true }
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> { mapView.onStop(); holder.started = false }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (holder.started) {
                mapView.onPause()
                mapView.onStop()
                holder.started = false
            }
            mapView.onDestroy()
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            holder.map = map
            map.uiSettings.apply {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isCompassEnabled = false
                isLogoEnabled = false
                attributionGravity = Gravity.BOTTOM or Gravity.END
                if (!interactive) setAllGesturesEnabled(false)
            }
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                holder.style = style
                holder.setupLayers(style, colors)
                holder.render()
                holder.fittedWithData = holder.places.isNotEmpty()
                mapView.post { holder.applyCamera(force = true) }
            }
            map.addOnMapClickListener { point ->
                val p = map.projection.toScreenLocation(point)
                val hit = map.queryRenderedFeatures(RectF(p.x - TAP, p.y - TAP, p.x + TAP, p.y + TAP), LAYER_PINS)
                val id = hit.firstOrNull()?.getNumberProperty("id")?.toLong()
                if (id != null) placeClick(id) else mapClick()
                id != null
            }
            map.addOnCameraMoveListener { if (holder.centerRadiusM != null) holder.renderCenterRadius() }
            map.addOnCameraIdleListener { cameraIdle(map.cameraPosition.target ?: return@addOnCameraIdleListener) }
        }
        onDispose { }
    }

    LaunchedEffect(places, me, allRadii, centerRadiusM) {
        holder.places = places
        holder.me = me
        holder.allRadii = allRadii
        holder.centerRadiusM = centerRadiusM
        holder.render()
        // Os lugares chegam do banco depois do primeiro desenho: enquadra uma vez quando aparecem.
        if (!holder.fittedWithData && (places.isNotEmpty() || me != null) && holder.focus is CameraFocus.FitAll) {
            holder.fittedWithData = holder.style != null
            holder.applyCamera(force = true)
        }
    }
    LaunchedEffect(focus.key) {
        holder.focus = focus
        holder.applyCamera(force = true)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private class MapHolder {
    var map: MapLibreMap? = null
    var style: Style? = null
    var started = false
    var places: List<MapPlace> = emptyList()
    var me: LatLng? = null
    var allRadii = false
    var centerRadiusM: Int? = null
    var focus: CameraFocus = CameraFocus.FitAll()
    var fittedWithData = false

    fun setupLayers(style: Style, c: RoteiroColors) {
        val accent = c.accent.toArgb()
        style.addSource(GeoJsonSource(SRC_RADIUS, FeatureCollection.fromFeatures(emptyList<Feature>())))
        style.addSource(GeoJsonSource(SRC_PLACES, FeatureCollection.fromFeatures(emptyList<Feature>())))
        style.addSource(GeoJsonSource(SRC_ME, FeatureCollection.fromFeatures(emptyList<Feature>())))

        style.addLayer(FillLayer("radius-fill", SRC_RADIUS).withProperties(
            PropertyFactory.fillColor(accent), PropertyFactory.fillOpacity(0.10f),
        ))
        style.addLayer(LineLayer("radius-line", SRC_RADIUS).withProperties(
            PropertyFactory.lineColor(accent), PropertyFactory.lineWidth(1.5f), PropertyFactory.lineDasharray(arrayOf(2f, 2f)),
        ))
        style.addLayer(CircleLayer("me-halo", SRC_ME).withProperties(
            PropertyFactory.circleRadius(16f), PropertyFactory.circleColor(accent), PropertyFactory.circleOpacity(0.16f),
        ))
        style.addLayer(CircleLayer("me-dot", SRC_ME).withProperties(
            PropertyFactory.circleRadius(7f), PropertyFactory.circleColor(accent),
            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE), PropertyFactory.circleStrokeWidth(3f),
        ))
        val selected = Expression.toBool(Expression.get("selected"))
        style.addLayer(CircleLayer(LAYER_PINS, SRC_PLACES).withProperties(
            PropertyFactory.circleRadius(Expression.switchCase(selected, Expression.literal(21f), Expression.literal(18f))),
            PropertyFactory.circleColor(Expression.switchCase(selected, Expression.color(accent), Expression.color(c.bg.toArgb()))),
            PropertyFactory.circleStrokeColor(Expression.switchCase(selected, Expression.color(android.graphics.Color.WHITE), Expression.color(c.line.toArgb()))),
            PropertyFactory.circleStrokeWidth(Expression.switchCase(selected, Expression.literal(3f), Expression.literal(1.5f))),
        ))
        style.addLayer(SymbolLayer("pins-letter", SRC_PLACES).withProperties(
            PropertyFactory.textField(Expression.get("letter")),
            PropertyFactory.textFont(arrayOf(FONT_BOLD)),
            PropertyFactory.textSize(14f),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textColor(Expression.switchCase(selected, Expression.color(android.graphics.Color.WHITE), Expression.color(c.ink.toArgb()))),
        ))
        val hasCount = Expression.gt(Expression.get("count"), Expression.literal(0))
        style.addLayer(CircleLayer("pins-badge", SRC_PLACES).withProperties(
            PropertyFactory.circleRadius(9.5f),
            PropertyFactory.circleTranslate(arrayOf(15f, -15f)),
            PropertyFactory.circleColor(accent),
            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
            PropertyFactory.circleStrokeWidth(2f),
        ).withFilter(hasCount))
        style.addLayer(SymbolLayer("pins-count", SRC_PLACES).withProperties(
            PropertyFactory.textField(Expression.toString(Expression.get("count"))),
            PropertyFactory.textFont(arrayOf(FONT_BOLD)),
            PropertyFactory.textSize(11f),
            PropertyFactory.textTranslate(arrayOf(15f, -15f)),
            PropertyFactory.textAllowOverlap(true),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textColor(android.graphics.Color.WHITE),
        ).withFilter(hasCount))
        style.addLayer(SymbolLayer("pins-label", SRC_PLACES).withProperties(
            PropertyFactory.textField(Expression.get("name")),
            PropertyFactory.textFont(arrayOf(FONT_BOLD)),
            PropertyFactory.textSize(12f),
            PropertyFactory.textOffset(arrayOf(0f, 2.3f)),
            PropertyFactory.textColor(c.ink.toArgb()),
            PropertyFactory.textHaloColor(c.bg.toArgb()),
            PropertyFactory.textHaloWidth(2f),
        ).withFilter(selected))
    }

    fun render() {
        val style = style ?: return
        val features = places.map { p ->
            Feature.fromGeometry(Point.fromLngLat(p.lng, p.lat)).apply {
                addNumberProperty("id", p.id)
                addStringProperty("name", p.name)
                addStringProperty("letter", p.name.trim().take(1).uppercase())
                addNumberProperty("count", p.count)
                addBooleanProperty("selected", p.selected)
            }
        }
        style.getSourceAs<GeoJsonSource>(SRC_PLACES)?.setGeoJson(FeatureCollection.fromFeatures(features))

        val radii = places.filter { allRadii || it.selected }.map { circle(it.lat, it.lng, it.radiusM) }
        if (centerRadiusM == null) style.getSourceAs<GeoJsonSource>(SRC_RADIUS)?.setGeoJson(FeatureCollection.fromFeatures(radii))
        else renderCenterRadius()

        val meFeature = me?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))) } ?: emptyList()
        style.getSourceAs<GeoJsonSource>(SRC_ME)?.setGeoJson(FeatureCollection.fromFeatures(meFeature))
    }

    fun renderCenterRadius() {
        val style = style ?: return
        val target = map?.cameraPosition?.target ?: return
        val r = centerRadiusM ?: return
        style.getSourceAs<GeoJsonSource>(SRC_RADIUS)?.setGeoJson(FeatureCollection.fromFeatures(listOf(circle(target.latitude, target.longitude, r))))
    }

    fun applyCamera(force: Boolean) {
        val map = map ?: return
        if (style == null && !force) return
        when (val f = focus) {
            is CameraFocus.At -> map.cameraPosition = CameraPosition.Builder().target(LatLng(f.lat, f.lng)).zoom(f.zoom).build()
            is CameraFocus.FitAll -> {
                val points = places.map { LatLng(it.lat, it.lng) } + listOfNotNull(me)
                when (points.size) {
                    0 -> map.cameraPosition = CameraPosition.Builder().target(DEFAULT_CENTER).zoom(11.0).build()
                    1 -> map.cameraPosition = CameraPosition.Builder().target(points[0]).zoom(15.0).build()
                    else -> {
                        val bounds = LatLngBounds.Builder().includes(points).build()
                        try {
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                            if (map.cameraPosition.zoom > 16.0) map.moveCamera(CameraUpdateFactory.zoomTo(16.0))
                        } catch (e: Exception) {
                            map.cameraPosition = CameraPosition.Builder().target(points[0]).zoom(13.0).build()
                        }
                    }
                }
            }
        }
    }

    /** Polígono aproximando um círculo de [radiusM] metros. */
    private fun circle(lat: Double, lng: Double, radiusM: Int): Feature {
        val steps = 64
        val dLat = radiusM / 111_320.0
        val dLng = radiusM / (111_320.0 * cos(Math.toRadians(lat)))
        val ring = (0..steps).map { i ->
            val t = 2 * Math.PI * i / steps
            Point.fromLngLat(lng + dLng * sin(t), lat + dLat * cos(t))
        }
        return Feature.fromGeometry(Polygon.fromLngLats(listOf(ring)))
    }
}

private const val MAP_DELAY_MS = 280L
/** Estilo colorido do OpenFreeMap: ruas, nomes de lojas, parques e pontos de interesse. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val FONT_BOLD = "Noto Sans Bold"
private const val SRC_PLACES = "places"
private const val SRC_RADIUS = "radius"
private const val SRC_ME = "me"
private const val LAYER_PINS = "pins"
private const val TAP = 28f
private val DEFAULT_CENTER = LatLng(-23.5505, -46.6333) // São Paulo
