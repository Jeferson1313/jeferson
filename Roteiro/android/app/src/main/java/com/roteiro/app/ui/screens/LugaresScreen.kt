package com.roteiro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roteiro.app.data.ItemEntity
import com.roteiro.app.data.PlaceEntity
import com.roteiro.app.ui.AppViewModel
import com.roteiro.app.ui.Nav
import com.roteiro.app.ui.Routes
import com.roteiro.app.ui.components.RoteiroButton
import com.roteiro.app.ui.components.RoteiroChip
import com.roteiro.app.ui.components.ItemRow
import com.roteiro.app.ui.components.PlaceIcons
import com.roteiro.app.ui.components.PlaceRow
import com.roteiro.app.ui.map.CameraFocus
import com.roteiro.app.ui.map.ContextMap
import com.roteiro.app.ui.map.MapPlace
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.Space
import com.roteiro.app.ui.theme.Type
import com.roteiro.core.Geo
import com.roteiro.core.RemindWhen
import org.maplibre.android.geometry.LatLng

/** Itens que contam no pino/linha de um lugar. */
internal fun pendingCount(all: List<ItemEntity>, placeId: Long) =
    all.count { it.placeId == placeId && it.isAlive && it.remind != RemindWhen.TIME }

internal fun placeMeta(all: List<ItemEntity>, place: PlaceEntity, here: Boolean): String {
    val t = all.count { it.placeId == place.id && it.isTask && it.isAlive }
    val m = all.count { it.placeId == place.id && !it.isTask && it.isAlive }
    val parts = mutableListOf<String>()
    if (here) parts += "Você está aqui"
    parts += when (t) { 0 -> "sem tarefas"; 1 -> "1 tarefa"; else -> "$t tarefas" }
    if (m > 0) parts += if (m == 1) "1 memória" else "$m memórias"
    return parts.joinToString(" · ")
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LugaresScreen(vm: AppViewModel, nav: Nav) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var focus by remember { mutableStateOf<CameraFocus>(CameraFocus.FitAll(key = 0)) }
    val perms by vm.permissions.collectAsStateWithLifecycle()
    // Posição ao vivo enquanto esta tela está aberta; ao sair, o GPS é desligado.
    val liveFlow = remember(perms.location) { vm.liveLocation() }
    val live by liveFlow.collectAsStateWithLifecycle(initialValue = null)
    val fix = live ?: snapshot?.fix
    val me = fix?.let { LatLng(it.lat, it.lng) }
    val c = C.colors
    val geoPlaces = places.filter { it.isGeo }
    // Lugares do tipo "qualquer mercado" só aparecem na lista quando têm algo pendente.
    val typePlaces = places.filter { !it.isGeo && pendingCount(allItems, it.id) > 0 }

    val mapPlaces = geoPlaces.map { MapPlace(it.id, it.name, it.lat, it.lng, it.radiusM, pendingCount(allItems, it.id), it.id == (selected ?: ctx.currentPlaceId)) }
    val sorted = geoPlaces.sortedWith(compareBy<PlaceEntity> { it.id != ctx.currentPlaceId }.thenBy { p -> fix?.let { Geo.distanceM(it.lat, it.lng, p.lat, p.lng) } ?: 0.0 })

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.fillMaxWidth().weight(1.1f)) {
            ContextMap(
                places = mapPlaces,
                modifier = Modifier.fillMaxSize(),
                me = me,
                focus = focus,
                onPlaceClick = { selected = it },
                onMapClick = { selected = null },
            )
            Row(Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingPill("Lugares")
                Spacer(Modifier.weight(1f))
                FloatingIcon(Icons.Outlined.MyLocation, "Minha localização") {
                    selected = null
                    val now = live
                    if (now != null) focus = CameraFocus.At(now.lat, now.lng, 16.5, key = System.nanoTime())
                    else vm.locateMe { f -> if (f != null) focus = CameraFocus.At(f.lat, f.lng, 16.5, key = System.nanoTime()) }
                }
                FloatingIcon(Icons.Rounded.Add, "Novo lugar") { nav.go(Routes.placeEdit()) }
            }
            val sel = places.firstOrNull { it.id == selected }
            if (sel != null) {
                PlacePreview(
                    sel, allItems, fix?.let { Geo.formatDistance(Geo.distanceM(it.lat, it.lng, sel.lat, sel.lng)) },
                    onOpen = { nav.go(Routes.place(sel.id)) },
                    onClose = { selected = null },
                    onItem = nav::item,
                    onToggle = { vm.toggleDone(it) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        if (geoPlaces.isEmpty() && typePlaces.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = Space.gutter, vertical = 20.dp)) {
                Text("Onde suas coisas acontecem?", style = Type.h2, color = c.ink)
                Text("Salve os lugares que você frequenta. Comece por estes:", style = Type.secondary, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Casa", "Trabalho", "Supermercado", "Academia", "Farmácia").forEach { name ->
                        RoteiroChip(name, false, { nav.go(Routes.placeEdit(name = name)) }, icon = PlaceIcons.of(PlaceIcons.guess(name)))
                    }
                }
                Spacer(Modifier.height(18.dp))
                RoteiroButton("Adicionar lugar", onClick = { nav.go(Routes.placeEdit()) }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = Space.gutter, vertical = 8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Seus lugares", style = Type.h2, color = c.ink, modifier = Modifier.weight(1f))
                        Text(if (geoPlaces.size == 1) "1 lugar" else "${geoPlaces.size} lugares", style = Type.data, color = c.ink3)
                    }
                }
                items(sorted, key = { it.id }) { p ->
                    val here = p.id == ctx.currentPlaceId
                    PlaceRow(
                        PlaceIcons.of(p.icon), p.name, placeMeta(allItems, p, here),
                        onClick = { nav.go(Routes.place(p.id)) },
                        here = here,
                        distance = if (!here) fix?.let { Geo.formatDistance(Geo.distanceM(it.lat, it.lng, p.lat, p.lng)) } else null,
                    )
                }
                if (typePlaces.isNotEmpty()) {
                    item { com.roteiro.app.ui.components.SectionHeader("Qualquer lugar do tipo") }
                    items(typePlaces, key = { "c${it.id}" }) { p ->
                        PlaceRow(PlaceIcons.of(p.icon), p.name, placeMeta(allItems, p, false), onClick = { nav.go(Routes.place(p.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacePreview(
    place: PlaceEntity,
    all: List<ItemEntity>,
    distance: String?,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onItem: (ItemEntity) -> Unit,
    onToggle: (ItemEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = C.colors
    val pending = all.filter { it.placeId == place.id && it.isAlive && it.remind != RemindWhen.TIME }
    Column(
        modifier.padding(12.dp).fillMaxWidth().shadow(12.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(c.bg).padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = Type.h2, color = c.ink)
                Text(listOfNotNull(place.address, distance).joinToString(" · ").ifEmpty { "Raio de ${place.radiusM} m" }, style = Type.secondary, color = c.ink2, maxLines = 1)
            }
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.surface).clickable(onClickLabel = "Fechar", onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, null, Modifier.size(18.dp), tint = c.ink)
            }
        }
        pending.take(2).forEachIndexed { i, it -> ItemRow(it, null, onToggle = { onToggle(it) }, onClick = { onItem(it) }, divider = i == 0 && pending.size > 1) }
        if (pending.isEmpty()) Text("Nada pendente aqui.", style = Type.secondary, color = c.ink3, modifier = Modifier.padding(vertical = 12.dp))
        RoteiroButton("Abrir lugar", onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
internal fun FloatingPill(text: String) {
    val c = C.colors
    Box(Modifier.height(46.dp).shadow(8.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(c.bg).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
    }
}

@Composable
internal fun FloatingIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = C.colors
    Box(
        Modifier.size(46.dp).shadow(8.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(c.bg).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(22.dp), tint = c.ink) }
}
