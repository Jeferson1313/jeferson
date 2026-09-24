package com.contexto.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.contexto.app.context.Permissions
import com.contexto.app.data.ItemEntity
import com.contexto.app.data.PlaceEntity
import com.contexto.app.ui.AppViewModel
import com.contexto.app.ui.Nav
import com.contexto.app.ui.Routes
import com.contexto.app.ui.components.Banner
import com.contexto.app.ui.components.BannerKind
import com.contexto.app.ui.components.ButtonKind
import com.contexto.app.ui.components.ContextRing
import com.contexto.app.ui.components.ContextoButton
import com.contexto.app.ui.components.ContextoChip
import com.contexto.app.ui.components.CountBadge
import com.contexto.app.ui.components.EmptyState
import com.contexto.app.ui.components.ItemRow
import com.contexto.app.ui.components.Logo
import com.contexto.app.ui.components.PlaceIcons
import com.contexto.app.ui.components.RingState
import com.contexto.app.ui.components.SectionHeader
import com.contexto.app.ui.theme.C
import com.contexto.app.ui.theme.Space
import com.contexto.app.ui.theme.Type
import com.contexto.core.Detection
import com.contexto.core.Geo
import com.contexto.core.RemindWhen
import com.contexto.core.Words
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun AgoraScreen(vm: AppViewModel, nav: Nav) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val maybeDismissed by vm.maybeDismissed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.permissionsChanged() }

    val byId = places.associateBy { it.id }
    val here = ctx.currentPlaceId?.let(byId::get)
    val maybe = (snapshot?.detection as? Detection.Maybe)?.takeIf { here == null && !maybeDismissed }?.let { byId[it.place.id] }
    val now = System.currentTimeMillis()

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 96.dp),
    ) {
        item { TopRow() }

        when {
            !perms.location -> {
                item {
                    Hero("Agora", "Onde você está?", "Localização desativada", RingState.WARN, live = false)
                    Spacer(Modifier.height(14.dp))
                    Banner(
                        BannerKind.WARN, Icons.Outlined.LocationOff,
                        "Não conseguimos perceber quando você chega",
                        "Sem localização, avisos de chegada e saída ficam pausados.",
                        action = "Permitir localização",
                        onAction = { askLocation.launch(Permissions.foregroundLocation) },
                    )
                    if (places.isNotEmpty()) {
                        SectionHeader("Escolha onde está")
                        PlaceChips(places) { vm.setHere(it.id) }
                    }
                }
                if (here != null) hereSection(here, allItems, vm, nav)
            }
            places.isEmpty() -> item {
                Hero("Agora", "Bem-vindo", "Nenhum lugar salvo ainda", RingState.OUT, live = false)
                Spacer(Modifier.height(40.dp))
                EmptyState(
                    Icons.Outlined.Place,
                    "Comece salvando um lugar",
                    "Casa, trabalho ou o mercado. Depois é só colocar ali o que você precisa lembrar.",
                ) { ContextoButton("Salvar um lugar", onClick = { nav.go(Routes.placeEdit()) }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth()) }
            }
            here != null -> {
                item {
                    val justArrived = now - ctx.arrivedAt < 10 * 60_000
                    Hero(
                        kicker = if (justArrived) "Você chegou" else "Agora · você está em",
                        title = here.name,
                        meta = listOfNotNull(here.address?.substringBefore(" - ")?.substringBefore(","), since(ctx.arrivedAt, now)).joinToString("  ·  "),
                        state = RingState.IN,
                        live = true,
                        action = "Não é aqui?" to { vm.notHere() },
                    )
                }
                hereSection(here, allItems, vm, nav)
            }
            maybe != null -> {
                item {
                    val acc = snapshot?.fix?.accuracyM?.toInt() ?: 0
                    Hero("Talvez você esteja em", "${maybe.name}?", "Sinal de localização fraco  ·  ±$acc m", RingState.OUT, live = false)
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ContextoButton("Sim, estou aqui", onClick = { vm.setHere(maybe.id) }, small = true, modifier = Modifier.weight(1f))
                        ContextoButton("Não estou", onClick = vm::dismissMaybe, kind = ButtonKind.OUTLINE, small = true, modifier = Modifier.weight(1f))
                    }
                    SectionHeader("Ou escolha onde está")
                    PlaceChips(places.filter { it.id != maybe.id }) { vm.setHere(it.id) }
                }
            }
            else -> {
                item {
                    val left = ctx.lastLeftPlaceId?.let(byId::get)
                    Hero(
                        "Agora", "Em trânsito",
                        if (left != null && now - ctx.lastLeftAt < 60 * 60_000) "Saiu ${Words.de(left.name)} ${since(ctx.lastLeftAt, now)}" else "Fora dos seus lugares",
                        RingState.OUT, live = false,
                    )
                }
                val fix = snapshot?.fix
                val near = places
                    .map { p -> p to allItems.count { it.placeId == p.id && it.isAlive && it.remind != RemindWhen.TIME } }
                    .filter { it.second > 0 }
                    .sortedBy { (p, _) -> fix?.let { Geo.distanceM(it.lat, it.lng, p.lat, p.lng) } ?: 0.0 }
                if (near.isNotEmpty()) item {
                    SectionHeader("Perto de você", action = "Mapa", onAction = { nav.tab(Routes.LUGARES) })
                    LazyRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(near, key = { it.first.id }) { (p, n) ->
                            NearCard(
                                PlaceIcons.of(p.icon), p.name,
                                listOfNotNull(fix?.let { Geo.formatDistance(Geo.distanceM(it.lat, it.lng, p.lat, p.lng)) }, if (n == 1) "1 item" else "$n itens").joinToString(" · "),
                            ) { nav.go(Routes.place(p.id)) }
                        }
                    }
                }
                val snoozed = allItems.filter { it.placeId != null && it.placeId == ctx.lastLeftPlaceId && it.snoozed && !it.done && !it.archived }
                if (snoozed.isNotEmpty()) {
                    item { SectionHeader("Ficou para a próxima visita", snoozed.size) }
                    items(snoozed, key = { "s${it.id}" }) { ItemRow(it, byId[it.placeId]?.name, onToggle = { vm.toggleDone(it) }, onClick = { nav.item(it) }) }
                }
            }
        }

        // Hoje, em qualquer lugar: lembretes por horário e itens sem lugar.
        if (places.isNotEmpty()) {
            val today = allItems.filter { it.isAlive && (it.remind == RemindWhen.TIME || it.placeId == null) }
                .sortedBy { it.timeOfDayMin ?: Int.MAX_VALUE }
            if (today.isNotEmpty()) {
                item { SectionHeader("Hoje, em qualquer lugar", today.size) }
                items(today, key = { "t${it.id}" }) { ItemRow(it, it.placeId?.let(byId::get)?.name, onToggle = { vm.toggleDone(it) }, onClick = { nav.item(it) }) }
            }
        }
    }
}

/** Seções do lugar atual: pendências, memórias e o que vem a seguir. */
private fun androidx.compose.foundation.lazy.LazyListScope.hereSection(here: PlaceEntity, all: List<ItemEntity>, vm: AppViewModel, nav: Nav) {
    val recent = System.currentTimeMillis() - 2 * 60 * 60_000
    val shown = all.filter { it.placeId == here.id && !it.archived && it.remind != RemindWhen.TIME && (it.isAlive || (it.done && (it.doneAt ?: 0) > recent)) }
    val tasks = shown.filter { it.isTask }
    val mems = shown.filter { !it.isTask }
    val pending = shown.count { it.isAlive }
    if (shown.isEmpty()) {
        item {
            Column(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.colors.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Nada esperando por você ${Words.em(here.name)}.", style = Type.secondary, color = C.colors.ink2)
                ContextoButton("Adicionar ${Words.em(here.name)}", onClick = { nav.go(Routes.task(placeId = here.id)) }, kind = ButtonKind.SECONDARY, small = true, icon = Icons.Rounded.Add)
            }
        }
    } else {
        if (tasks.isNotEmpty()) {
            item { SectionHeader(if (pending > 0) "Pendências aqui" else "Tudo feito aqui", if (pending > 0) pending else null, "Ver lugar", { nav.go(Routes.place(here.id)) }) }
            items(tasks, key = { "h${it.id}" }) { ItemRow(it, null, onToggle = { vm.toggleDone(it) }, onClick = { nav.item(it) }, divider = it != tasks.last()) }
        }
        if (mems.isNotEmpty()) {
            item { SectionHeader(if (mems.size > 1) "Memórias deste lugar" else "Memória deste lugar") }
            items(mems, key = { "m${it.id}" }) { ItemRow(it, null, onToggle = {}, onClick = { nav.item(it) }, divider = it != mems.last()) }
        }
    }
    val leaveCount = all.count { it.placeId == here.id && it.isTask && it.isAlive && it.remind != RemindWhen.NONE && it.remind != RemindWhen.TIME }
    item {
        SectionHeader("Próximos gatilhos")
        NextRow(
            "saída", Icons.AutoMirrored.Outlined.Logout, "Ao sair daqui",
            when {
                !here.notifyLeave -> "Aviso de saída desligado"
                leaveCount == 0 -> "Nada pendente, sem aviso"
                leaveCount == 1 -> "Confere 1 pendência"
                else -> "Confere $leaveCount pendências"
            },
        )
        val nowMin = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        all.filter { it.remind == RemindWhen.TIME && it.isAlive && (it.timeOfDayMin ?: 0) > nowMin }
            .sortedBy { it.timeOfDayMin }
            .take(2)
            .forEach { NextRow(Words.time(it.timeOfDayMin ?: 0), Icons.Outlined.Schedule, it.title, "Lembrete por horário") }
    }
}

@Composable
private fun TopRow() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("pt-BR")))
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) }.replace("-feira", "").replace(".", "")
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Logo(22.dp)
        Text(date, style = Type.secondary.copy(fontWeight = FontWeight.Medium), color = C.colors.ink2)
    }
}

@Composable
private fun Hero(
    kicker: String,
    title: String,
    meta: String,
    state: RingState,
    live: Boolean,
    action: Pair<String, () -> Unit>? = null,
) {
    val c = C.colors
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (live) Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(c.accent))
                Text(kicker.uppercase(), style = Type.label.copy(fontSize = Type.meta.fontSize), color = if (live) c.accent else c.ink3)
            }
            Text(title, style = Type.context, color = c.ink, modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(meta, style = Type.meta, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
            if (action != null) {
                Text(action.first, style = Type.meta.copy(fontWeight = FontWeight.SemiBold), color = c.accent,
                    modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = action.second).padding(vertical = 4.dp))
            }
        }
        ContextRing(state)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PlaceChips(places: List<PlaceEntity>, onPick: (PlaceEntity) -> Unit) {
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        places.take(6).forEach { p -> ContextoChip(p.name, false, { onPick(p) }, icon = PlaceIcons.of(p.icon)) }
    }
}

@Composable
private fun NearCard(icon: ImageVector, name: String, meta: String, onClick: () -> Unit) {
    val c = C.colors
    Column(Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.ink2)
            Text(name, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(meta, style = Type.meta, color = c.ink3, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun NextRow(whenText: String, icon: ImageVector, title: String, sub: String, count: Int? = null) {
    val c = C.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(whenText, style = Type.data, color = c.ink2, modifier = Modifier.width(50.dp))
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.ink2)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.body.copy(fontSize = Type.secondary.fontSize), color = c.ink)
            Text(sub, style = Type.meta, color = c.ink3)
        }
        if (count != null) CountBadge(count)
    }
}

private fun since(t: Long, now: Long): String {
    val min = ((now - t) / 60_000).toInt()
    return when {
        t <= 0 -> ""
        min < 1 -> "agora mesmo"
        min < 60 -> "há $min min"
        min < 24 * 60 -> "há ${min / 60} h"
        else -> "há ${min / (24 * 60)} dias"
    }
}
