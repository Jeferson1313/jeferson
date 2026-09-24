package com.roteiro.app.ui.screens

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roteiro.app.data.PlaceEntity
import com.roteiro.app.ui.AppViewModel
import com.roteiro.app.ui.Nav
import com.roteiro.app.ui.components.ButtonKind
import com.roteiro.app.ui.components.RoteiroButton
import com.roteiro.app.ui.components.RoteiroChip
import com.roteiro.app.ui.components.RoteiroField
import com.roteiro.app.ui.components.FieldLabel
import com.roteiro.app.ui.components.PlaceIcons
import com.roteiro.app.ui.map.CameraFocus
import com.roteiro.app.ui.map.ContextMap
import com.roteiro.app.ui.map.MapPlace
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.Space
import com.roteiro.app.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Novo lugar ou edição: o usuário move o mapa até o pino central,
 * dá um nome e ajusta o raio. O endereço é preenchido pelo Geocoder do aparelho.
 */
@Composable
fun PlaceEditScreen(vm: AppViewModel, nav: Nav, placeId: Long, presetName: String? = null) {
    val places by vm.places.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val c = C.colors
    val existing = places.firstOrNull { it.id == placeId }

    var name by rememberSaveable { mutableStateOf(presetName ?: "") }
    var icon by rememberSaveable { mutableStateOf(presetName?.let(PlaceIcons::guess) ?: "place") }
    var radius by rememberSaveable { mutableStateOf(100f) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchError by remember { mutableStateOf<String?>(null) }
    var center by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var cameraFocus by remember { mutableStateOf<CameraFocus?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Ponto de partida: o lugar em edição, a posição atual ou o centro padrão.
    LaunchedEffect(existing?.id, snapshot?.fix) {
        if (loaded) return@LaunchedEffect
        if (existing != null) {
            name = existing.name; icon = existing.icon; radius = existing.radiusM.toFloat()
            cameraFocus = CameraFocus.At(existing.lat, existing.lng, 16.5, key = "edit${existing.id}")
            center = existing.lat to existing.lng
            loaded = true
        } else if (placeId == 0L) {
            val fix = snapshot?.fix
            cameraFocus = if (fix != null) CameraFocus.At(fix.lat, fix.lng, 16.5, key = "me") else CameraFocus.FitAll(key = "start")
            if (fix != null) { center = fix.lat to fix.lng; loaded = true }
        }
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        focus.clearFocus()
        scope.launch {
            val found = geocode(context, q)
            if (found == null) searchError = "Não encontramos \"$q\". Tente com rua e cidade."
            else {
                searchError = null
                cameraFocus = CameraFocus.At(found.first, found.second, 16.5, key = "q$q${System.nanoTime()}")
            }
        }
    }

    fun save() {
        val (lat, lng) = center ?: return
        if (name.isBlank() || saving) return
        saving = true
        scope.launch {
            val address = reverseGeocode(context, lat, lng)
            val base = existing ?: PlaceEntity(name = name.trim(), lat = lat, lng = lng)
            vm.savePlace(base.copy(name = name.trim(), icon = icon, lat = lat, lng = lng, radiusM = radius.roundToInt(), address = address ?: base.address)) {
                nav.back()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                ContextMap(
                    places = places.filter { it.id != placeId && it.isGeo }.map { MapPlace(it.id, it.name, it.lat, it.lng, it.radiusM, 0, false) },
                    modifier = Modifier.fillMaxSize(),
                    focus = cameraFocus ?: CameraFocus.FitAll(key = "wait"),
                    centerRadiusM = radius.roundToInt(),
                    onCameraIdle = { center = it.latitude to it.longitude },
                )
                Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    FloatingIcon(Icons.Outlined.MyLocation, "Minha localização") {
                        vm.locateMe { f -> if (f != null) cameraFocus = CameraFocus.At(f.lat, f.lng, 17.0, key = System.nanoTime()) }
                    }
                }
                // Pino fixo no centro: o usuário move o mapa por baixo dele.
                Icon(
                    Icons.Outlined.Place, null,
                    Modifier.align(Alignment.Center).offset(y = (-18).dp).size(40.dp),
                    tint = c.accent,
                )
                Column(Modifier.statusBarsPadding().padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FloatingIcon(Icons.Outlined.Close, "Fechar") { nav.back() }
                        Box(Modifier.weight(1f).shadow(8.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))) {
                            RoteiroField(
                                value = query,
                                onValueChange = { query = it; searchError = null },
                                placeholder = "Buscar endereço",
                                leading = Icons.Outlined.Search,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { search() }),
                            )
                        }
                    }
                    searchError?.let {
                        Text(it, style = Type.meta, color = c.late, modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(c.bg).padding(8.dp))
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().background(c.bg).padding(horizontal = Space.gutter).padding(top = 16.dp, bottom = 12.dp).navigationBarsPadding().imePadding(),
            ) {
                Text(if (existing != null) "Editar lugar" else "Novo lugar", style = Type.h2, color = c.ink)
                Text("Mova o mapa até o pino ficar no lugar certo.", style = Type.meta, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
                FieldLabel("Nome")
                RoteiroField(
                    value = name,
                    onValueChange = { v -> name = v.take(40); if (existing == null) icon = PlaceIcons.guess(v) },
                    placeholder = "Ex.: Casa, Trabalho, Mercado",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                )
                LazyRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlaceIcons.all, key = { it.first }) { (key, vector) ->
                        RoteiroChip("", icon == key, {
                            // Tocar no ícone preenche o nome, se ainda não foi digitado um nome próprio.
                            val previous = PlaceIcons.labels[icon]
                            if (name.isBlank() || name == previous) PlaceIcons.labels[key]?.let { name = it }
                            icon = key
                        }, icon = vector)
                    }
                }
                FieldLabel("Raio", "${radius.roundToInt()} m")
                Slider(
                    value = radius,
                    onValueChange = { radius = (it / 10).roundToInt() * 10f },
                    valueRange = PlaceEntity.MIN_RADIUS_M.toFloat()..PlaceEntity.MAX_RADIUS_M.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = c.bg, activeTrackColor = c.accent, inactiveTrackColor = c.surface2),
                )
                Text(
                    if (radius < 80f) "Raio pequeno: o celular pode demorar alguns minutos para perceber a chegada, principalmente com a tela desligada."
                    else "Lugares pequenos, como uma loja, funcionam bem com 80 a 150 m.",
                    style = Type.meta, color = if (radius < 80f) c.mem else c.ink3,
                )
                Spacer(Modifier.height(14.dp))
                RoteiroButton(
                    if (saving) "Salvando…" else "Salvar lugar",
                    onClick = ::save,
                    enabled = name.isNotBlank() && center != null && !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing != null) {
                    RoteiroButton("Apagar lugar", onClick = { confirmDelete = true }, kind = ButtonKind.GHOST, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar ${existing.name}?") },
            text = { Text("As tarefas e memórias de lá continuam na sua lista, sem lugar.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deletePlace(existing) { nav.back(); nav.back() } }) {
                    Text("Apagar", color = c.late)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
            containerColor = c.bg,
        )
    }
}

@Suppress("DEPRECATION")
private suspend fun geocode(context: Context, query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    try {
        Geocoder(context, Locale.forLanguageTag("pt-BR")).getFromLocationName(query, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
    } catch (e: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    try {
        val a = Geocoder(context, Locale.forLanguageTag("pt-BR")).getFromLocation(lat, lng, 1)?.firstOrNull() ?: return@withContext null
        listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(", ").ifEmpty { a.subLocality ?: a.locality }
    } catch (e: Exception) {
        null
    }
}
