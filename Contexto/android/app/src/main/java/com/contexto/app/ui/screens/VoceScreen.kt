package com.contexto.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.contexto.app.context.Permissions
import com.contexto.app.ui.AppViewModel
import com.contexto.app.ui.components.ButtonKind
import com.contexto.app.ui.components.ContextoButton
import com.contexto.app.ui.components.SectionHeader
import com.contexto.app.ui.components.Segmented
import com.contexto.app.ui.components.StatusText
import com.contexto.app.ui.components.ToggleRow
import com.contexto.app.ui.components.Tone
import com.contexto.app.ui.theme.C
import com.contexto.app.ui.theme.Space
import com.contexto.app.ui.theme.Type

/** Ajustes: o que o app consegue perceber, como avisa e privacidade. */
@Composable
fun VoceScreen(vm: AppViewModel) {
    val perms by vm.permissions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = C.colors
    var confirmErase by remember { mutableStateOf(false) }

    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.permissionsChanged() }
    val askBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.permissionsChanged() }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.permissionsChanged() }

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 96.dp)) {
        item {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Você", style = Type.h2, color = c.ink)
            }
            Text("Seus dados ficam só neste aparelho. Não há conta nem servidor.", style = Type.secondary, color = c.ink2)
        }
        item {
            SectionHeader("O que o Contexto percebe")
            PermissionCard(
                Icons.Outlined.LocationOn, "Localização",
                when {
                    !perms.location -> "Negada. Chegadas e saídas estão pausadas."
                    !perms.backgroundLocation -> "Só com o app aberto. Para avisar ao chegar, escolha \"Permitir o tempo todo\"."
                    else -> "O tempo todo. Chegadas e saídas ativas."
                },
                status = when { !perms.location -> "Negada" to Tone.LATE; !perms.backgroundLocation -> "Parcial" to Tone.MEMORY; else -> "Ativa" to Tone.OK },
                action = when {
                    !perms.location -> "Permitir" to { askLocation.launch(Permissions.foregroundLocation) }
                    !perms.backgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        "Ajustar" to { askBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                    else -> null
                },
            )
            PermissionCard(
                Icons.Outlined.Notifications, "Notificações",
                if (perms.notifications) "Avisos de chegada, saída e horário." else "Desligadas. O app não consegue avisar você.",
                status = if (perms.notifications) "Ativas" to Tone.OK else "Desligadas" to Tone.LATE,
                action = when {
                    perms.notifications -> null
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "Permitir" to { askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    else -> "Abrir ajustes" to { context.startActivity(Permissions.notificationSettingsIntent(context)) }
                },
            )
            PermissionCard(
                Icons.Outlined.BatteryAlert, "Economia de bateria",
                if (perms.batteryUnrestricted) "O Contexto pode rodar em segundo plano." else "Alguns aparelhos atrasam ou bloqueiam avisos. Libere o Contexto na lista.",
                status = if (perms.batteryUnrestricted) "Ok" to Tone.OK else "Pode atrasar" to Tone.MEMORY,
                action = if (perms.batteryUnrestricted) null else "Resolver" to { context.startActivity(Permissions.batterySettingsIntent()) },
            )
        }
        item {
            SectionHeader("Avisos")
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Schedule, null, Modifier.size(20.dp), tint = c.ink2)
                Column(Modifier.weight(1f)) {
                    Text("Tempo mínimo no lugar", style = Type.bodyRegular, color = c.ink)
                    Text("Evita aviso quando você só passa perto", style = Type.meta, color = c.ink3)
                }
            }
            val options = listOf(1, 2, 5)
            Segmented(options.map { "$it min" }, options.indexOf(settings.dwellMinutes).coerceAtLeast(0), { vm.setDwell(options[it]) }, Modifier.padding(top = 10.dp, bottom = 6.dp))
            ToggleRow("Horário silencioso", settings.quietHours, vm::setQuietHours, Icons.Outlined.Nightlight, "Das 22h às 7h, os avisos chegam sem som")
            ToggleRow("Um aviso por chegada", true, {}, Icons.Outlined.Layers, "Todos os itens do lugar numa notificação só", divider = false)
        }
        item {
            SectionHeader("Privacidade")
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Shield, null, Modifier.size(20.dp), tint = c.ink2)
                Text(
                    "A localização é lida pelo próprio Android para detectar chegadas e saídas. O Contexto não guarda histórico de visitas e não envia dados para ninguém. O mapa baixa imagens do OpenStreetMap.",
                    style = Type.secondary, color = c.ink2,
                )
            }
            ContextoButton("Apagar todos os dados", onClick = { confirmErase = true }, kind = ButtonKind.DANGER, icon = Icons.Outlined.DeleteOutline, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text("Contexto 1.0 · mapas © OpenStreetMap, OpenFreeMap", style = Type.meta, color = c.ink3, modifier = Modifier.padding(top = 16.dp))
        }
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Apagar todos os dados?") },
            text = { Text("Lugares, tarefas e memórias serão apagados deste aparelho. Não dá para desfazer.") },
            confirmButton = { TextButton(onClick = { confirmErase = false; vm.deleteEverything() }) { Text("Apagar", color = c.late) } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancelar") } },
            containerColor = c.bg,
        )
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, text: String, status: Pair<String, Tone>, action: Pair<String, () -> Unit>?) {
    val c = C.colors
    Box(Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, Modifier.size(20.dp), tint = c.ink2)
                Text(title, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, modifier = Modifier.weight(1f))
                StatusText(status.first, status.second)
            }
            Text(text, style = Type.secondary, color = c.ink2, modifier = Modifier.padding(top = 6.dp, start = 32.dp))
            if (action != null) {
                ContextoButton(action.first, onClick = action.second, kind = ButtonKind.OUTLINE, small = true, modifier = Modifier.padding(top = 10.dp, start = 32.dp))
            }
        }
    }
}
