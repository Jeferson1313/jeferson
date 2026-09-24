package com.contexto.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.contexto.app.data.ItemEntity
import com.contexto.app.data.PlaceEntity
import com.contexto.app.ui.AppViewModel
import com.contexto.app.ui.Nav
import com.contexto.app.ui.Routes
import com.contexto.app.ui.components.ButtonKind
import com.contexto.app.ui.components.ContextoButton
import com.contexto.app.ui.components.ContextoChip
import com.contexto.app.ui.components.ContextoField
import com.contexto.app.ui.components.FieldLabel
import com.contexto.app.ui.components.OptionGroup
import com.contexto.app.ui.components.OptionRow
import com.contexto.app.ui.components.PlaceIcons
import com.contexto.app.ui.components.Segmented
import com.contexto.app.ui.components.Tag
import com.contexto.app.ui.components.TitleInput
import com.contexto.app.ui.components.ToggleRow
import com.contexto.app.ui.components.Tone
import com.contexto.app.ui.components.dashedBorder
import com.contexto.app.ui.theme.C
import com.contexto.app.ui.theme.Space
import com.contexto.app.ui.theme.Type
import com.contexto.core.ItemKind
import com.contexto.core.MemoryShow
import com.contexto.core.RemindWhen
import com.contexto.core.Repeat
import com.contexto.core.Words

/* =========================================================
   Estrutura comum: cabeçalho, conteúdo rolável, rodapé com resumo
   ========================================================= */
@Composable
private fun EditScaffold(
    title: String,
    onClose: () -> Unit,
    summaryIcon: ImageVector,
    summaryTint: androidx.compose.ui.graphics.Color,
    summary: String,
    saveLabel: String,
    canSave: Boolean,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = C.colors
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).androidClick(onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, "Fechar", tint = c.ink)
            }
            Text(title, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f).padding(end = 44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter).padding(bottom = 24.dp)) { content() }
        Column(Modifier.fillMaxWidth().topLine(c.line).padding(horizontal = Space.gutter).padding(top = 12.dp, bottom = 16.dp).navigationBarsPadding()) {
            Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(summaryIcon, null, Modifier.size(18.dp).padding(top = 1.dp), tint = summaryTint)
                Text(summary, style = Type.secondary, color = c.ink2)
            }
            ContextoButton(saveLabel, onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun Modifier.androidClick(onClick: () -> Unit) = this.clickable(onClick = onClick)

private fun Modifier.topLine(color: androidx.compose.ui.graphics.Color) = this.drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1f)
}

/** Chips "Onde": lugares salvos (o atual primeiro) + "Novo lugar". */
@Composable
private fun PlacePicker(places: List<PlaceEntity>, currentId: Long?, selected: Long?, allowNone: Boolean, onSelect: (Long?) -> Unit, onNew: () -> Unit) {
    val ordered = places.sortedBy { it.id != currentId }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ordered, key = { it.id }) { p -> ContextoChip(p.name, selected == p.id, { onSelect(p.id) }, icon = PlaceIcons.of(p.icon)) }
        if (allowNone) item { ContextoChip("Sem lugar", selected == null, { onSelect(null) }) }
        item { ContextoChip("Novo lugar", false, onNew, icon = Icons.Rounded.Add, dashed = true) }
    }
}

/* =========================================================
   Nova tarefa / editar tarefa
   ========================================================= */
@Composable
fun TaskEditScreen(vm: AppViewModel, nav: Nav, itemId: Long, presetPlace: Long?) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = C.colors
    val existing = allItems.firstOrNull { it.id == itemId }

    var title by rememberSaveable { mutableStateOf("") }
    var placeId by rememberSaveable { mutableStateOf(presetPlace ?: ctx.currentPlaceId) }
    var remind by rememberSaveable { mutableStateOf(RemindWhen.ARRIVE) }
    var repeat by rememberSaveable { mutableStateOf(Repeat.ONCE) }
    var time by rememberSaveable { mutableStateOf(18 * 60) }
    var loaded by rememberSaveable { mutableStateOf(itemId == 0L) }
    val focus = androidx.compose.runtime.remember { FocusRequester() }

    LaunchedEffect(existing?.id) {
        if (!loaded && existing != null) {
            title = existing.title; placeId = existing.placeId; remind = existing.remind; repeat = existing.repeat
            time = existing.timeOfDayMin ?: time; loaded = true
        }
    }
    LaunchedEffect(Unit) { if (itemId == 0L) runCatching { focus.requestFocus() } }

    // Sem lugar, só faz sentido lembrar por horário ou sem aviso.
    val eff = if (placeId == null && (remind == RemindWhen.ARRIVE || remind == RemindWhen.LEAVE)) RemindWhen.NONE else remind
    val place = places.firstOrNull { it.id == placeId }

    EditScaffold(
        title = if (existing != null) "Editar tarefa" else "Nova tarefa",
        onClose = { nav.back() },
        summaryIcon = Icons.Outlined.NotificationsNone,
        summaryTint = c.accent,
        summary = Words.taskSummary(place?.name, eff, repeat, time),
        saveLabel = "Salvar tarefa",
        canSave = title.isNotBlank(),
        onSave = {
            val base = existing ?: ItemEntity(kind = ItemKind.TASK, title = title.trim())
            vm.saveItem(
                base.copy(title = title.trim(), placeId = placeId, remind = eff, repeat = repeat, timeOfDayMin = if (eff == RemindWhen.TIME) time else null, lastFiredAt = if (existing?.timeOfDayMin != time) null else existing?.lastFiredAt),
            ) { nav.back() }
        },
    ) {
        TitleInput(title, { title = it }, "O que precisa ser feito?", Modifier.focusRequester(focus))
        FieldLabel("Onde")
        PlacePicker(places, ctx.currentPlaceId, placeId, allowNone = true, onSelect = { placeId = it }, onNew = { nav.go(Routes.placeEdit()) })
        FieldLabel("Quando lembrar")
        OptionGroup {
            if (placeId != null) {
                OptionRow("Ao chegar", eff == RemindWhen.ARRIVE, { remind = RemindWhen.ARRIVE }, Icons.AutoMirrored.Outlined.Login, "Depois de ${settings.dwellMinutes} min no local")
                OptionRow("Ao sair", eff == RemindWhen.LEAVE, { remind = RemindWhen.LEAVE }, Icons.AutoMirrored.Outlined.Logout, "Só se ainda estiver pendente")
            }
            OptionRow(
                "Em um horário", eff == RemindWhen.TIME,
                {
                    remind = RemindWhen.TIME
                    TimePickerDialog(context, { _, h, m -> time = h * 60 + m }, time / 60, time % 60, true).show()
                },
                Icons.Outlined.Schedule, if (eff == RemindWhen.TIME) "Às ${Words.time(time)} · toque para mudar" else null,
            )
            OptionRow("Sem aviso", eff == RemindWhen.NONE, { remind = RemindWhen.NONE }, Icons.Outlined.NotificationsOff, if (placeId != null) "Aparece quando você estiver lá ou abrir o lugar" else "Fica só na sua lista", divider = false)
        }
        FieldLabel("Repetir")
        Segmented(listOf("Uma vez", "Diária", "Semanal", "Mensal"), repeat.ordinal, { repeat = Repeat.entries[it] })
    }
}

/* =========================================================
   Nova memória / editar memória
   ========================================================= */
@Composable
fun MemoryEditScreen(vm: AppViewModel, nav: Nav, itemId: Long, presetPlace: Long?) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    val c = C.colors
    val existing = allItems.firstOrNull { it.id == itemId }

    var title by rememberSaveable { mutableStateOf("") }
    var placeId by rememberSaveable { mutableStateOf(presetPlace ?: ctx.currentPlaceId ?: places.firstOrNull()?.id) }
    var show by rememberSaveable { mutableStateOf(MemoryShow.ALWAYS) }
    var withField by rememberSaveable { mutableStateOf(false) }
    var fieldLabel by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(itemId == 0L) }
    val focus = androidx.compose.runtime.remember { FocusRequester() }

    LaunchedEffect(existing?.id) {
        if (!loaded && existing != null) {
            title = existing.title; placeId = existing.placeId; show = existing.memoryShow
            withField = existing.fieldLabel != null; fieldLabel = existing.fieldLabel ?: ""; note = existing.note ?: ""; loaded = true
        }
    }
    LaunchedEffect(places.isNotEmpty()) { if (placeId == null) placeId = places.firstOrNull()?.id }
    LaunchedEffect(Unit) { if (itemId == 0L) runCatching { focus.requestFocus() } }
    val place = places.firstOrNull { it.id == placeId }

    EditScaffold(
        title = if (existing != null) "Editar memória" else "Nova memória",
        onClose = { nav.back() },
        summaryIcon = Icons.Outlined.BookmarkBorder,
        summaryTint = c.mem,
        summary = Words.memorySummary(place?.name, show),
        saveLabel = "Salvar memória",
        canSave = title.isNotBlank() && placeId != null,
        onSave = {
            val base = existing ?: ItemEntity(kind = ItemKind.MEMORY, title = title.trim())
            vm.saveItem(
                base.copy(
                    title = title.trim(), placeId = placeId, remind = RemindWhen.ARRIVE, memoryShow = show,
                    fieldLabel = if (withField) fieldLabel.trim().ifEmpty { "Valor" } else null,
                    note = note.trim().ifEmpty { null },
                ),
            ) { nav.back() }
        },
    ) {
        Box(Modifier.padding(top = 4.dp)) { Tag("Memória", Tone.MEMORY, Icons.Outlined.BookmarkBorder) }
        TitleInput(title, { title = it }, "O que você quer lembrar lá?", Modifier.focusRequester(focus), cursor = c.mem)
        FieldLabel("Presa a")
        if (places.isEmpty()) {
            Text("Salve um lugar primeiro. Memórias sempre ficam presas a um lugar.", style = Type.secondary, color = c.ink2)
            ContextoButton("Salvar um lugar", onClick = { nav.go(Routes.placeEdit()) }, kind = ButtonKind.SECONDARY, small = true, modifier = Modifier.padding(top = 10.dp))
        } else {
            PlacePicker(places, ctx.currentPlaceId, placeId, allowNone = false, onSelect = { placeId = it }, onNew = { nav.go(Routes.placeEdit()) })
        }
        FieldLabel("Mostrar")
        Segmented(listOf("Sempre que estiver lá", "Só na próxima vez"), show.ordinal, { show = MemoryShow.entries[it] })
        FieldLabel("Guardar junto", "opcional")
        ToggleRow("Campo para anotar lá", withField, { withField = it }, Icons.Outlined.EditNote, "Ex.: número do medidor, preço, medida", divider = withField)
        if (withField) {
            ContextoField(fieldLabel, { fieldLabel = it.take(40) }, "Nome do campo (ex.: Leitura do medidor)", Modifier.padding(top = 10.dp))
        }
        ContextoField(note, { note = it.take(300) }, "Nota (opcional)", Modifier.padding(top = 10.dp), leading = Icons.AutoMirrored.Outlined.Notes, singleLine = false)
    }
}

/* =========================================================
   Memória aberta no lugar: anotar o valor, arquivar ou manter
   ========================================================= */
@Composable
fun MemoryScreen(vm: AppViewModel, nav: Nav, itemId: Long) {
    val places by vm.places.collectAsStateWithLifecycle()
    val allItems by vm.items.collectAsStateWithLifecycle()
    val ctx by vm.context.collectAsStateWithLifecycle()
    val c = C.colors
    val item = allItems.firstOrNull { it.id == itemId } ?: return
    val place = places.firstOrNull { it.id == item.placeId }
    var value by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).androidClick { nav.back() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = c.ink)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text("Editar", style = Type.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.accent, modifier = Modifier.padding(end = 12.dp).androidClick { nav.go(Routes.memoryEdit(item.id)) }.padding(8.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
            Tag(listOfNotNull("Memória", place?.name).joinToString(" · "), Tone.MEMORY, Icons.Outlined.BookmarkBorder)
            Text(item.title, style = Type.h1, color = c.ink, modifier = Modifier.padding(top = 10.dp))
            Text(
                when {
                    place == null -> "Sem lugar"
                    place.id == ctx.currentPlaceId -> "Você está ${Words.em(place.name)} agora"
                    else -> "Aparece quando você estiver ${Words.em(place.name)}"
                },
                style = Type.secondary, color = c.ink2, modifier = Modifier.padding(top = 4.dp),
            )
            if (item.fieldLabel != null) {
                Column(
                    Modifier.padding(top = 22.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.memSoft)
                        .then(Modifier.dashedMem(c.mem)).padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(item.fieldLabel.uppercase(), style = Type.label, color = c.mem)
                    androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = { value = it.take(40) },
                        singleLine = true,
                        textStyle = Type.data.copy(fontSize = Type.h2.fontSize, color = c.ink),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.mem),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        decorationBox = { inner ->
                            Box {
                                if (value.isEmpty()) Text(item.fieldValue?.let { "Último: $it" } ?: "Toque para anotar", style = Type.data.copy(fontSize = Type.h2.fontSize), color = c.ink3)
                                inner()
                            }
                        },
                    )
                }
            }
            if (!item.note.isNullOrBlank()) {
                Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Notes, null, tint = c.ink2)
                    Text(item.note, style = Type.secondary, color = c.ink2)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(bottom = 16.dp).navigationBarsPadding()) {
            ContextoButton(if (item.fieldLabel != null) "Salvar e arquivar" else "Arquivar", onClick = { vm.saveMemoryValue(item, value, archive = true) { nav.back() } }, modifier = Modifier.fillMaxWidth())
            ContextoButton("Manter para a próxima vez", onClick = { vm.saveMemoryValue(item, value, archive = false) { nav.back() } }, kind = ButtonKind.GHOST, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

private fun Modifier.dashedMem(color: androidx.compose.ui.graphics.Color) = this.dashedBorder(color, 16.dp)
