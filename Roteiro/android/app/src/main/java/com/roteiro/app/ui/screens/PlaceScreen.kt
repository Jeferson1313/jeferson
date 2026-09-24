package com.roteiro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roteiro.app.ui.AppViewModel
import com.roteiro.app.ui.Nav
import com.roteiro.app.ui.Routes
import com.roteiro.app.ui.components.ButtonKind
import com.roteiro.app.ui.components.RoteiroButton
import com.roteiro.app.ui.components.ItemRow
import com.roteiro.app.ui.components.SectionHeader
import com.roteiro.app.ui.components.Tag
import com.roteiro.app.ui.components.ToggleRow
import com.roteiro.app.ui.components.Tone
import com.roteiro.app.ui.map.CameraFocus
import com.roteiro.app.ui.map.ContextMap
import com.roteiro.app.ui.map.MapPlace
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.Space
import com.roteiro.app.ui.theme.Type
import com.roteiro.core.RemindWhen

/** Detalhe de um lugar: tudo que pertence a ele e quando lembrar. */
@Composable
fun PlaceScreen(vm: AppViewModel, nav: Nav, placeId: Long) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val c = C.colors
    val place = places.firstOrNull { it.id == placeId } ?: return
    val here = ctx.currentPlaceId == place.id
    val recent = System.currentTimeMillis() - 2 * 60 * 60_000
    val mine = allItems.filter { it.placeId == place.id && !it.archived && (it.isAlive || it.snoozed || (it.done && (it.doneAt ?: 0) > recent)) }
    val tasks = mine.filter { it.isTask }
    val mems = mine.filter { !it.isTask }

    LazyColumn(Modifier.fillMaxSize().background(c.bg), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                ContextMap(
                    places = listOf(MapPlace(place.id, place.name, place.lat, place.lng, place.radiusM, 0, selected = true)),
                    modifier = Modifier.fillMaxSize(),
                    focus = CameraFocus.At(place.lat, place.lng, zoom = 16.0, key = place.id to place.radiusM),
                    interactive = false,
                )
                Row(Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FloatingIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") { nav.back() }
                    FloatingIcon(Icons.Outlined.Edit, "Editar lugar") { nav.go(Routes.placeEdit(place.id)) }
                }
            }
        }
        item {
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = Space.gutter).padding(top = 16.dp)) {
                if (here) Tag("Você está aqui", Tone.ACCENT, Icons.Outlined.RadioButtonChecked)
                Text(place.name, style = Type.h1, color = c.ink, modifier = Modifier.padding(top = 8.dp))
                Text(listOfNotNull(place.address, "raio de ${place.radiusM} m").joinToString(" · "), style = Type.secondary, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
                if (!here) RoteiroButton("Estou aqui agora", onClick = { vm.setHere(place.id) }, kind = ButtonKind.SECONDARY, small = true, modifier = Modifier.padding(top = 12.dp))
            }
        }
        item { Pad { SectionHeader("Tarefas", tasks.count { it.isAlive }.takeIf { it > 0 }, "+ Adicionar", { nav.go(Routes.task(placeId = place.id)) }) } }
        if (tasks.isEmpty()) item { Pad { Text("Nenhuma tarefa aqui.", style = Type.secondary, color = c.ink3, modifier = Modifier.padding(vertical = 8.dp)) } }
        items(tasks, key = { "t${it.id}" }) { Pad { ItemRow(it, null, onToggle = { vm.toggleDone(it) }, onClick = { nav.item(it) }) } }

        item { Pad { SectionHeader("Memórias", mems.size.takeIf { it > 0 }, "+ Adicionar", { nav.go(Routes.memoryEdit(placeId = place.id)) }) } }
        if (mems.isEmpty()) item { Pad { Text("Nenhuma memória aqui.", style = Type.secondary, color = c.ink3, modifier = Modifier.padding(vertical = 8.dp)) } }
        items(mems, key = { "m${it.id}" }) { Pad { ItemRow(it, null, onToggle = {}, onClick = { nav.item(it) }) } }

        item {
            Pad {
                SectionHeader("Quando lembrar")
                ToggleRow("Ao chegar", place.notifyArrive, { vm.togglePlaceNotice(place, arrive = true) }, Icons.AutoMirrored.Outlined.Login, "Depois de ${settings.dwellMinutes} min no local")
                ToggleRow("Ao sair", place.notifyLeave, { vm.togglePlaceNotice(place, arrive = false) }, Icons.AutoMirrored.Outlined.Logout, "Se alguma tarefa ficou pendente", divider = false)
            }
        }
        val timed = allItems.count { it.placeId == place.id && it.remind == RemindWhen.TIME && it.isAlive }
        if (timed > 0) item { Pad { Text("$timed ${if (timed == 1) "lembrete" else "lembretes"} por horário também ${if (timed == 1) "está" else "estão"} ligado${if (timed == 1) "" else "s"} a este lugar.", style = Type.meta, color = c.ink3, modifier = Modifier.padding(top = 12.dp)) } }
    }
}

@Composable
private fun Pad(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = Space.gutter)) { content() }
}
