@file:OptIn(ExperimentalMaterial3Api::class)

package com.contexto.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.contexto.app.data.ItemEntity
import com.contexto.app.ui.components.ButtonKind
import com.contexto.app.ui.components.ContextoButton
import com.contexto.app.ui.components.ContextoField
import com.contexto.app.ui.components.OptionGroup
import com.contexto.app.ui.components.OptionRow
import com.contexto.app.ui.components.bottomLine
import com.contexto.app.ui.screens.AgoraScreen
import com.contexto.app.ui.screens.ListaScreen
import com.contexto.app.ui.screens.LugaresScreen
import com.contexto.app.ui.screens.MemoryEditScreen
import com.contexto.app.ui.screens.MemoryScreen
import com.contexto.app.ui.screens.OnboardingScreen
import com.contexto.app.ui.screens.PlaceEditScreen
import com.contexto.app.ui.screens.PlaceScreen
import com.contexto.app.ui.screens.TaskEditScreen
import com.contexto.app.ui.screens.VoceScreen
import com.contexto.app.ui.theme.C
import com.contexto.app.ui.theme.Type
import com.contexto.core.ItemKind
import com.contexto.core.Words

object Routes {
    const val AGORA = "agora"
    const val LUGARES = "lugares"
    const val LISTA = "lista"
    const val VOCE = "voce"
    fun place(id: Long) = "place/$id"
    fun placeEdit(id: Long = 0, name: String? = null) = "placeedit?id=$id" + (name?.let { "&name=" + android.net.Uri.encode(it) } ?: "")
    fun task(id: Long = 0, placeId: Long? = null) = "taskedit?id=$id&place=${placeId ?: -1}"
    fun memoryEdit(id: Long = 0, placeId: Long? = null) = "memoryedit?id=$id&place=${placeId ?: -1}"
    fun memory(id: Long) = "memory/$id"
}

/** Ações comuns passadas às telas. */
class Nav(val controller: NavHostController, private val openItem: (ItemEntity) -> Unit) {
    fun go(route: String) = controller.navigate(route) { launchSingleTop = true }
    fun back() = controller.popBackStack()
    fun item(item: ItemEntity) = if (item.kind == ItemKind.MEMORY) go(Routes.memory(item.id)) else openItem(item)
    fun tab(route: String) = controller.navigate(route) {
        popUpTo(Routes.AGORA) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun ContextoRoot(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(C.colors.bg)) {
        if (!settings.onboardingDone) OnboardingScreen(vm) else MainScaffold(vm)
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel) {
    val controller = rememberNavController()
    var sheetItem by remember { mutableStateOf<ItemEntity?>(null) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    val nav = remember(controller) { Nav(controller) { sheetItem = it } }
    val backStack by controller.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Routes.AGORA, Routes.LUGARES, Routes.LISTA, Routes.VOCE)
    val showTabs = route in tabs
    val message by vm.message.collectAsStateWithLifecycle()
    val leavePlace by vm.leaveSheet.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(controller, startDestination = Routes.AGORA) {
                composable(Routes.AGORA) { AgoraScreen(vm, nav) }
                composable(Routes.LUGARES) { LugaresScreen(vm, nav) }
                composable(Routes.LISTA) { ListaScreen(vm, nav) }
                composable(Routes.VOCE) { VoceScreen(vm) }
                composable("place/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    PlaceScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
                composable(
                    "placeedit?id={id}&name={name}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) {
                    PlaceEditScreen(vm, nav, it.arguments?.getLong("id") ?: 0, it.arguments?.getString("name"))
                }
                composable(
                    "taskedit?id={id}&place={place}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("place") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) {
                    TaskEditScreen(vm, nav, it.arguments?.getLong("id") ?: 0, it.arguments?.getLong("place")?.takeIf { p -> p >= 0 })
                }
                composable(
                    "memoryedit?id={id}&place={place}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("place") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) {
                    MemoryEditScreen(vm, nav, it.arguments?.getLong("id") ?: 0, it.arguments?.getLong("place")?.takeIf { p -> p >= 0 })
                }
                composable("memory/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    MemoryScreen(vm, nav, it.arguments?.getLong("id") ?: 0)
                }
            }
            MessageBar(message, onUndo = { message?.undo?.invoke(); vm.clearMessage() }, modifier = Modifier.align(Alignment.BottomCenter))
        }
        if (showTabs) TabBar(route, onTab = nav::tab, onAdd = { addOpen = true })
    }

    if (addOpen) AddSheet(vm, nav, onDismiss = { addOpen = false })
    sheetItem?.let { item -> ItemSheet(vm, nav, item, onDismiss = { sheetItem = null }) }
    leavePlace?.let { LeaveSheet(vm, nav, it) }
}

/* =========================================================
   Barra inferior: Agora · Lugares · + · Lista · Você
   ========================================================= */
@Composable
private fun TabBar(route: String?, onTab: (String) -> Unit, onAdd: () -> Unit) {
    val c = C.colors
    Row(
        Modifier.fillMaxWidth().background(c.bg).bottomLineTop(c.line).windowInsetsPadding(WindowInsets.navigationBars).height(68.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem("Agora", Icons.Outlined.RadioButtonChecked, route == Routes.AGORA) { onTab(Routes.AGORA) }
        TabItem("Lugares", Icons.Outlined.Map, route == Routes.LUGARES) { onTab(Routes.LUGARES) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(width = 52.dp, height = 40.dp).clip(RoundedCornerShape(14.dp)).background(c.accent).clickable(onClickLabel = "Adicionar", role = Role.Button, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, "Adicionar", Modifier.size(26.dp), tint = c.accentInk) }
        }
        TabItem("Lista", Icons.AutoMirrored.Outlined.List, route == Routes.LISTA) { onTab(Routes.LISTA) }
        TabItem("Você", Icons.Outlined.Person, route == Routes.VOCE) { onTab(Routes.VOCE) }
    }
}

private fun Modifier.bottomLineTop(color: androidx.compose.ui.graphics.Color) = this.drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = 1f)
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val c = C.colors
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Tab, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (selected) c.accent else c.ink3)
        Text(label, style = Type.tab, color = if (selected) c.ink else c.ink3)
    }
}

/* =========================================================
   Mensagem de rodapé ("Feito. Faltam 2 aqui." · Desfazer)
   ========================================================= */
@Composable
private fun MessageBar(message: UiMessage?, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val c = C.colors
    AnimatedVisibility(message != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val m = message ?: return@AnimatedVisibility
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.ink).padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = c.ok)
            Text(m.text, style = Type.secondary, color = c.bg, modifier = Modifier.weight(1f))
            if (m.undo != null) Text("Desfazer", style = Type.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.bg.copy(alpha = 0.8f), modifier = Modifier.clickable(onClick = onUndo).padding(4.dp))
        }
    }
}

/* =========================================================
   Folha do botão +
   ========================================================= */
@Composable
private fun AddSheet(vm: AppViewModel, nav: Nav, onDismiss: () -> Unit) {
    val c = C.colors
    val ctx by vm.context.collectAsStateWithLifecycle()
    val places by vm.places.collectAsStateWithLifecycle()
    val here = places.firstOrNull { it.id == ctx.currentPlaceId }
    var quick by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bg) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding().imePadding()) {
            if (here != null) {
                ContextoField(
                    value = quick,
                    onValueChange = { quick = it.take(80) },
                    placeholder = "Anotar algo aqui ${Words.em(here.name)}…",
                    leading = Icons.Outlined.Place,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (quick.isNotBlank()) { vm.quickCapture(quick); onDismiss() } }),
                    trailing = {
                        if (quick.isNotBlank()) Text("Salvar", style = Type.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.accent,
                            modifier = Modifier.clickable { vm.quickCapture(quick); onDismiss() })
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            AddOption(Icons.Outlined.TaskAlt, "Tarefa", "Algo para fazer em um lugar", c.accentSoft, c.accent) { onDismiss(); nav.go(Routes.task(placeId = here?.id)) }
            AddOption(Icons.Outlined.BookmarkBorder, "Memória", "Algo para ver ou anotar quando estiver lá", c.memSoft, c.mem) { onDismiss(); nav.go(Routes.memoryEdit(placeId = here?.id)) }
            AddOption(Icons.Outlined.Place, "Lugar", "Casa, trabalho, mercado ou qualquer endereço", c.surface, c.ink, divider = false) { onDismiss(); nav.go(Routes.placeEdit()) }
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, sub: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, divider: Boolean = true, onClick: () -> Unit) {
    val c = C.colors
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.bottomLine(c.line) else Modifier).clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), tint = fg) }
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
            Text(sub, style = Type.meta, color = c.ink3)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = c.ink3)
    }
}

/* =========================================================
   Ações de uma tarefa
   ========================================================= */
@Composable
private fun ItemSheet(vm: AppViewModel, nav: Nav, item: ItemEntity, onDismiss: () -> Unit) {
    val c = C.colors
    val places by vm.places.collectAsStateWithLifecycle()
    val place = places.firstOrNull { it.id == item.placeId }
    var moving by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bg) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()) {
            Text(item.title, style = Type.h2, color = c.ink)
            Text(listOfNotNull(place?.name, if (item.done) "Concluída" else null).joinToString(" · ").ifEmpty { "Sem lugar" }, style = Type.secondary, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(16.dp))
            ContextoButton(if (item.done) "Marcar como pendente" else "Concluir", onClick = { vm.toggleDone(item); onDismiss() }, icon = Icons.Rounded.Check, modifier = Modifier.fillMaxWidth())
            if (!item.done) {
                Text("ADIAR OU MOVER", style = Type.label, color = c.ink3, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                OptionGroup {
                    if (place != null) OptionRow("Na próxima vez que eu vier aqui", false, { vm.snoozeNextVisit(item); onDismiss() }, subtitle = "Some até você voltar ${Words.ao(place.name)}")
                    OptionRow("Mudar de lugar", moving, { moving = !moving }, subtitle = "Escolha onde ela deve aparecer", divider = moving)
                    if (moving) places.filter { it.id != item.placeId }.forEachIndexed { i, p ->
                        OptionRow(p.name, false, { vm.moveTo(item, p); onDismiss() }, divider = i < places.size - 2)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContextoButton("Editar", onClick = { onDismiss(); nav.go(Routes.task(item.id)) }, kind = ButtonKind.GHOST, modifier = Modifier.weight(1f))
                ContextoButton("Não preciso mais", onClick = { vm.archive(item); onDismiss() }, kind = ButtonKind.GHOST, modifier = Modifier.weight(1f))
            }
        }
    }
}

/* =========================================================
   "Saindo de X? Antes de ir, ainda falta:"
   ========================================================= */
@Composable
private fun LeaveSheet(vm: AppViewModel, nav: Nav, placeId: Long) {
    val c = C.colors
    val places by vm.places.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val place = places.firstOrNull { it.id == placeId }
    if (place == null) {
        LaunchedEffect(placeId) { vm.closeLeaveSheet() }
        return
    }
    val pending = items.filter { it.placeId == placeId && it.isTask && !it.archived && !it.snoozed && (it.isAlive || it.done) }
        .filter { !it.done || (it.doneAt ?: 0) > System.currentTimeMillis() - 30 * 60_000 }
    ModalBottomSheet(onDismissRequest = vm::closeLeaveSheet, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.bg) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = c.accent)
                }
                Column {
                    Text("Saindo ${Words.de(place.name)}?", style = Type.h2, color = c.ink)
                    Text(if (pending.any { !it.done }) "Antes de ir, ainda falta:" else "Tudo certo por aqui.", style = Type.secondary, color = c.ink2)
                }
            }
            Spacer(Modifier.height(8.dp))
            pending.forEach { item ->
                Row(Modifier.fillMaxWidth().bottomLine(c.line).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = Type.body, color = if (item.done) c.ink3 else c.ink, modifier = Modifier.weight(1f),
                        textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    if (!item.done) ContextoButton("Peguei", onClick = { vm.toggleDone(item) }, kind = ButtonKind.SECONDARY, small = true)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContextoButton("Próxima vez", onClick = { vm.leaveNextTime(placeId) }, kind = ButtonKind.OUTLINE, modifier = Modifier.weight(1f))
                ContextoButton("Peguei tudo", onClick = { vm.leaveAllDone(placeId) }, modifier = Modifier.weight(1f))
            }
            ContextoButton("Ainda estou aqui", onClick = { vm.stillHere(placeId) }, kind = ButtonKind.GHOST, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}
