package com.roteiro.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roteiro.app.context.Permissions
import com.roteiro.app.ui.AppViewModel
import com.roteiro.app.ui.components.ButtonKind
import com.roteiro.app.ui.components.ContextRing
import com.roteiro.app.ui.components.RoteiroButton
import com.roteiro.app.ui.components.RoteiroChip
import com.roteiro.app.ui.components.Logo
import com.roteiro.app.ui.components.PlaceIcons
import com.roteiro.app.ui.components.RingState
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.Type

/**
 * Onboarding curto: 3 telas de conceito (puláveis), explicação da localização
 * antes do pedido do sistema, e o primeiro lugar.
 */
@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val perms by vm.permissions.collectAsStateWithLifecycle()

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.permissionsChanged(); step = 5
    }
    val askBackground = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.permissionsChanged()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) else step = 4
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.permissionsChanged()
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        when {
            granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> step = 4 // mostra a explicação do "o tempo todo"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> step = 5
        }
    }

    AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding") { s ->
        when (s) {
            0 -> Concept(1, { ArtPlaces() }, "Coloque cada coisa no lugar onde ela acontece.",
                "Comprar café fica no mercado. Enviar o relatório fica no trabalho. Sua cabeça fica livre.", "Continuar", { step = 1 }, { step = 3 })
            1 -> Concept(2, { ArtArrive() }, "Chegou no lugar certo? A gente lembra você.",
                "Ao chegar ou sair de um lugar, o Roteiro mostra só o que importa ali. O resto espera.", "Continuar", { step = 2 }, { step = 3 })
            2 -> Concept(3, { ArtContexts() }, "Casa, trabalho, mercado ou qualquer lugar.",
                "Salve os lugares que você frequenta e coloque neles o que precisa lembrar.", "Começar", { step = 3 }, null)
            3 -> PermissionStep(
                onAllow = { askLocation.launch(Permissions.foregroundLocation) },
                onSkip = { step = 5 },
            )
            4 -> BackgroundStep(
                alreadyGranted = perms.backgroundLocation,
                onAllow = {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !perms.backgroundLocation -> askBackground.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !perms.notifications -> askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> step = 5
                    }
                },
                onSkip = { step = 5 },
            )
            else -> FirstPlaceStep(vm, locationGranted = perms.location, onDone = { vm.finishOnboarding() })
        }
    }
}

@Composable
private fun Frame(skip: (() -> Unit)?, content: @Composable () -> Unit) {
    val c = C.colors
    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            if (skip != null) Text("Pular", style = Type.secondary.copy(fontWeight = FontWeight.Medium), color = c.ink3, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = skip).padding(8.dp))
        }
        content()
    }
}

@Composable
private fun Concept(n: Int, art: @Composable () -> Unit, title: String, text: String, cta: String, onNext: () -> Unit, onSkip: (() -> Unit)?) {
    val c = C.colors
    Frame(onSkip) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) { art() }
            Text(title, style = Type.h1, color = c.ink)
            Text(text, style = Type.bodyRegular, color = c.ink2, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(vertical = 28.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..3).forEach { i -> Box(Modifier.height(6.dp).width(if (i == n) 22.dp else 6.dp).clip(CircleShape).background(if (i == n) c.accent else c.surface2)) }
            }
            RoteiroButton(cta, onClick = onNext, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionStep(onAllow: () -> Unit, onSkip: () -> Unit) {
    val c = C.colors
    Frame(null) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { ContextRing(RingState.IN, Modifier.size(150.dp)) }
            Text("Para lembrar no lugar certo, precisamos saber onde você está.", style = Type.h2.copy(fontSize = Type.h1.fontSize, lineHeight = Type.h1.lineHeight), color = c.ink)
            Perk(Icons.Outlined.Shield, "Fica no seu aparelho", "Sua localização não é enviada a ninguém.")
            Perk(Icons.Outlined.BatteryChargingFull, "Leve para a bateria", "Usamos as cercas virtuais do próprio Android, não GPS contínuo.")
            Perk(Icons.Outlined.NotificationsNone, "Só avisos úteis", "No máximo um aviso por chegada.")
            Spacer(Modifier.weight(1f))
            RoteiroButton("Permitir localização", onClick = onAllow, modifier = Modifier.fillMaxWidth())
            RoteiroButton("Agora não", onClick = onSkip, kind = ButtonKind.GHOST, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun BackgroundStep(alreadyGranted: Boolean, onAllow: () -> Unit, onSkip: () -> Unit) {
    val c = C.colors
    Frame(null) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { ContextRing(RingState.IN, Modifier.size(150.dp)) }
            Text("Um último passo: permitir o tempo todo.", style = Type.h2.copy(fontSize = Type.h1.fontSize, lineHeight = Type.h1.lineHeight), color = c.ink)
            Text(
                "Para avisar quando você chega com o app fechado, o Android pede a opção \"Permitir o tempo todo\". Na próxima tela, toque nela.",
                style = Type.bodyRegular, color = c.ink2, modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Sem ela, o Roteiro ainda funciona: ele mostra o que é do lugar quando você abre o app.",
                style = Type.secondary, color = c.ink3, modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            RoteiroButton(if (alreadyGranted) "Continuar" else "Abrir a permissão", onClick = onAllow, modifier = Modifier.fillMaxWidth())
            RoteiroButton("Agora não", onClick = onSkip, kind = ButtonKind.GHOST, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun FirstPlaceStep(vm: AppViewModel, locationGranted: Boolean, onDone: () -> Unit) {
    val c = C.colors
    var name by rememberSaveable { mutableStateOf("Casa") }
    var saving by remember { mutableStateOf(false) }
    Frame(null) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { ArtContexts(small = true) }
            Text("Onde você está agora?", style = Type.h1, color = c.ink)
            Text(
                if (locationGranted) "Salve este lugar para começar. Os outros você adiciona depois, pelo mapa."
                else "Sem localização, adicione seus lugares depois, na aba Lugares.",
                style = Type.bodyRegular, color = c.ink2, modifier = Modifier.padding(top = 12.dp),
            )
            if (locationGranted) {
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Casa", "Trabalho", "Outro lugar").forEach { n ->
                        RoteiroChip(n, name == n, { name = n }, icon = PlaceIcons.of(PlaceIcons.guess(n)))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (locationGranted) {
                RoteiroButton(
                    if (saving) "Salvando…" else "Salvar aqui como $name",
                    enabled = !saving,
                    onClick = {
                        saving = true
                        vm.saveCurrentLocationAs(if (name == "Outro lugar") "Meu lugar" else name, PlaceIcons.guess(name)) { onDone() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                RoteiroButton("Fazer isso depois", onClick = onDone, kind = ButtonKind.GHOST, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            } else {
                RoteiroButton("Entrar no app", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Perk(icon: ImageVector, title: String, text: String) {
    val c = C.colors
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = c.accent)
        Column {
            Text(title, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
            Text(text, style = Type.secondary, color = c.ink2)
        }
    }
}

/* ---------- Ilustrações simples, desenhadas com as cores do tema ---------- */

@Composable
private fun ArtPlaces() {
    val c = C.colors
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            drawCircle(c.accentSoft)
            drawCircle(c.accent, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * density, 6 * density))))
        }
        Box(Modifier.size(64.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(30.dp), tint = c.accentInk)
        }
        Pill("Comprar café", Modifier.align(Alignment.TopStart).padding(top = 30.dp))
        Pill("Sabão em pó", Modifier.align(Alignment.BottomEnd).padding(bottom = 36.dp))
    }
}

@Composable
private fun Pill(text: String, modifier: Modifier) {
    val c = C.colors
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.bg).padding(1.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(c.bg))
        Text(text, style = Type.secondary.copy(fontWeight = FontWeight.Medium), color = c.ink)
    }
}

@Composable
private fun ArtArrive() {
    val c = C.colors
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp)) {
            val s = size.minDimension / 260f
            drawCircle(c.accentSoft, 84 * s, Offset(150 * s, 150 * s))
            drawCircle(c.accent, 84 * s, Offset(150 * s, 150 * s), style = Stroke(1.5f * s * density / density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))))
            for (i in 0..6) drawCircle(c.ink3, 3f * s, Offset((30 + i * 17) * s, (240 - i * 14) * s))
            drawCircle(c.bg, 16 * s, Offset(150 * s, 150 * s))
            drawCircle(c.accent, 12 * s, Offset(150 * s, 150 * s))
        }
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 10.dp).clip(RoundedCornerShape(18.dp)).background(c.surface).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.accent), contentAlignment = Alignment.Center) { Logo(18.dp, c.accentInk) }
            Column {
                Text("Você chegou ao mercado.", style = Type.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                Text("3 coisas esperam por você", style = Type.meta, color = c.ink2)
            }
        }
    }
}

@Composable
private fun ArtContexts(small: Boolean = false) {
    val c = C.colors
    val size = if (small) 190.dp else 260.dp
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            drawCircle(c.line, style = Stroke(1.5f * density))
            drawCircle(c.line, radius = this.size.minDimension / 4, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))))
        }
        Logo(if (small) 40.dp else 56.dp)
        val token = if (small) 44.dp else 60.dp
        Token(Icons.Outlined.Home, false, token, Modifier.align(Alignment.TopCenter))
        Token(Icons.Outlined.WorkOutline, false, token, Modifier.align(Alignment.CenterEnd))
        Token(Icons.Outlined.DirectionsCar, true, token, Modifier.align(Alignment.BottomCenter))
        Token(Icons.Outlined.ShoppingCart, false, token, Modifier.align(Alignment.CenterStart))
    }
}

@Composable
private fun Token(icon: ImageVector, on: Boolean, size: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    val c = C.colors
    Box(modifier.size(size).clip(CircleShape).background(if (on) c.accent else c.surface), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size / 2.2f), tint = if (on) c.accentInk else c.ink)
    }
}
