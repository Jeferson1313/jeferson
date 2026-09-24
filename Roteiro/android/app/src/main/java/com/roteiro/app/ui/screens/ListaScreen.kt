package com.roteiro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roteiro.app.data.ItemEntity
import com.roteiro.app.ui.AppViewModel
import com.roteiro.app.ui.Nav
import com.roteiro.app.ui.Routes
import com.roteiro.app.ui.components.RoteiroButton
import com.roteiro.app.ui.components.RoteiroChip
import com.roteiro.app.ui.components.EmptyState
import com.roteiro.app.ui.components.ItemRow
import com.roteiro.app.ui.components.SectionHeader
import com.roteiro.app.ui.components.Segmented
import com.roteiro.app.ui.components.Tone
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.Space
import com.roteiro.app.ui.theme.Type
import com.roteiro.core.ItemKind
import com.roteiro.core.RemindWhen

private enum class ListFilter(val label: String) { ALL("Todas"), BY_PLACE("Por lugar"), TODAY("Hoje"), DONE("Concluídas") }

/**
 * Visão geral para revisar de vez em quando. De propósito, fica em segundo plano:
 * o jeito principal de usar o app é a tela Agora.
 */
@Composable
fun ListaScreen(vm: AppViewModel, nav: Nav) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    var kindIndex by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(ListFilter.ALL) }
    val c = C.colors
    val kind = if (kindIndex == 0) ItemKind.TASK else ItemKind.MEMORY
    val byId = places.associateBy { it.id }

    val ofKind = allItems.filter { it.kind == kind }
    val shown: List<ItemEntity> = when (filter) {
        ListFilter.DONE -> ofKind.filter { it.done }.sortedByDescending { it.doneAt ?: 0 }
        ListFilter.TODAY -> ofKind.filter { !it.done && !it.archived && (it.remind == RemindWhen.TIME || it.placeId == null || it.placeId == ctx.currentPlaceId) }
        else -> ofKind.filter { !it.done && !it.archived }
    }
    val nTasks = allItems.count { it.kind == ItemKind.TASK && !it.done && !it.archived }
    val nMems = allItems.count { it.kind == ItemKind.MEMORY && !it.done && !it.archived }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lista", style = Type.h2, color = c.ink)
            }
            Segmented(listOf("Tarefas · $nTasks", "Memórias · $nMems"), kindIndex, { kindIndex = it })
            LazyRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ListFilter.entries) { f -> RoteiroChip(f.label, filter == f, { filter = f }) }
            }
        }
        if (shown.isEmpty()) {
            item {
                Column(Modifier.padding(top = 64.dp)) {
                    when (filter) {
                        ListFilter.DONE -> EmptyState(Icons.Outlined.TaskAlt, "Nada concluído ainda", "Marque um item na tela Agora e ele aparece aqui.", Tone.OK)
                        ListFilter.TODAY -> EmptyState(Icons.Outlined.CalendarToday, "Nada para hoje", "Itens presos a lugares aparecem quando você chegar lá.")
                        else -> EmptyState(Icons.Rounded.Add, if (kind == ItemKind.TASK) "Nenhuma tarefa" else "Nenhuma memória", "Toque em + para colocar algo no lugar onde precisa acontecer.") {
                            RoteiroButton(if (kind == ItemKind.TASK) "Nova tarefa" else "Nova memória", onClick = { nav.go(if (kind == ItemKind.TASK) Routes.task() else Routes.memoryEdit()) })
                        }
                    }
                }
            }
        } else if (filter == ListFilter.BY_PLACE) {
            val groups = shown.groupBy { it.placeId }.toList().sortedWith(
                compareBy<Pair<Long?, List<ItemEntity>>>({ it.first != ctx.currentPlaceId }, { it.first == null }, { it.first?.let(byId::get)?.name ?: "" })
            )
            groups.forEach { (placeId, list) ->
                val p = placeId?.let(byId::get)
                item(key = "g$placeId") {
                    SectionHeader(
                        (p?.name ?: "Sem lugar") + if (placeId != null && placeId == ctx.currentPlaceId) " · aqui" else "",
                        list.size,
                        if (p != null) "Abrir" else null,
                        p?.let { { nav.go(Routes.place(it.id)) } },
                    )
                }
                items(list, key = { it.id }) { ItemRow(it, null, onToggle = { vm.toggleDone(it) }, onClick = { nav.item(it) }) }
            }
        } else {
            items(shown, key = { it.id }) { ItemRow(it, it.placeId?.let(byId::get)?.name, onToggle = { vm.toggleDone(it) }, onClick = { nav.item(it) }) }
        }
    }
}
