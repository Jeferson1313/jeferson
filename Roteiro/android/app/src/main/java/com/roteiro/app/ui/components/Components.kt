package com.roteiro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.TextUnit
import com.roteiro.app.data.ItemEntity
import com.roteiro.app.ui.theme.C
import com.roteiro.app.ui.theme.Radius
import com.roteiro.app.ui.theme.Type
import com.roteiro.core.ItemKind
import com.roteiro.core.RemindWhen
import com.roteiro.core.Repeat
import com.roteiro.core.Words

/* =========================================================
   Ícones de lugar
   ========================================================= */
object PlaceIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "home" to Icons.Outlined.Home,
        "work" to Icons.Outlined.WorkOutline,
        "cart" to Icons.Outlined.ShoppingCart,
        "pharmacy" to Icons.Outlined.LocalPharmacy,
        "gym" to Icons.Outlined.FitnessCenter,
        "tool" to Icons.Outlined.Build,
        "friends" to Icons.Outlined.Group,
        "school" to Icons.Outlined.School,
        "food" to Icons.Outlined.Restaurant,
        "place" to Icons.Outlined.Place,
    )

    fun of(key: String): ImageVector = all.firstOrNull { it.first == key }?.second ?: Icons.Outlined.Place

    /** Sugere um ícone a partir do nome digitado. */
    fun guess(name: String): String {
        val n = name.lowercase()
        return when {
            "casa" in n || "apart" in n -> "home"
            "trabalho" in n || "escrit" in n || "empresa" in n -> "work"
            "mercado" in n || "padaria" in n || "feira" in n || "loja" in n -> "cart"
            "farm" in n || "drogaria" in n -> "pharmacy"
            "academia" in n || "gin" in n -> "gym"
            "oficina" in n || "mecân" in n -> "tool"
            "escola" in n || "facul" in n -> "school"
            "restaurante" in n || "bar" in n -> "food"
            else -> "place"
        }
    }
}

/* =========================================================
   Marca e anel de contexto
   ========================================================= */
@Composable
fun Logo(size: Dp = 22.dp, ring: Color = C.colors.accent, dot: Color = ring) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 48f
        drawCircle(ring, radius = 17 * s, center = Offset(24 * s, 24 * s), style = Stroke(width = 5 * s))
        drawCircle(dot, radius = 6 * s, center = Offset(27.5f * s, 20.5f * s))
    }
}

enum class RingState { IN, OUT, WARN }

/** Anel da tela Agora: você dentro, fora ou sem sinal. */
@Composable
fun ContextRing(state: RingState, modifier: Modifier = Modifier) {
    val c = C.colors
    Canvas(modifier.size(64.dp)) {
        val s = size.minDimension / 64f
        val dash = PathEffect.dashPathEffect(floatArrayOf(3 * s, 4 * s))
        when (state) {
            RingState.IN -> {
                drawCircle(c.accentSoft, 29 * s, Offset(32 * s, 32 * s))
                drawCircle(c.accent, 29 * s, Offset(32 * s, 32 * s), style = Stroke(1.5f * s, pathEffect = dash))
                drawCircle(c.bg, 10.5f * s, Offset(36 * s, 28 * s))
                drawCircle(c.accent, 7.5f * s, Offset(36 * s, 28 * s))
            }
            RingState.OUT -> {
                drawCircle(c.ink3, 24 * s, Offset(30 * s, 34 * s), style = Stroke(1.5f * s, pathEffect = dash))
                drawCircle(c.bg, 9.5f * s, Offset(53 * s, 12 * s))
                drawCircle(c.ink2, 6.5f * s, Offset(53 * s, 12 * s))
            }
            RingState.WARN -> {
                drawCircle(c.lateSoft, 29 * s, Offset(32 * s, 32 * s))
                drawCircle(c.late, 29 * s, Offset(32 * s, 32 * s), style = Stroke(1.5f * s, pathEffect = dash))
                drawLine(c.late, Offset(32 * s, 22 * s), Offset(32 * s, 34 * s), strokeWidth = 3 * s, cap = StrokeCap.Round)
                drawLine(c.late, Offset(32 * s, 40 * s), Offset(32 * s, 40.5f * s), strokeWidth = 3 * s, cap = StrokeCap.Round)
            }
        }
    }
}

/* =========================================================
   Texto e rótulos
   ========================================================= */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    late: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 22.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = Type.label, color = if (late) C.colors.late else C.colors.ink3)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = Type.label.copy(fontFamily = com.roteiro.app.ui.theme.PlexMono), color = C.colors.ink2)
        }
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = Type.meta.copy(fontWeight = FontWeight.SemiBold),
                color = C.colors.accent,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAction).padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}

enum class Tone { NEUTRAL, ACCENT, MEMORY, LATE, OK }

@Composable
fun Tag(text: String, tone: Tone = Tone.NEUTRAL, icon: ImageVector? = null) {
    val c = C.colors
    val (bg, fg) = when (tone) {
        Tone.NEUTRAL -> c.surface to c.ink2
        Tone.ACCENT -> c.accentSoft to c.accent
        Tone.MEMORY -> c.memSoft to c.mem
        Tone.LATE -> c.lateSoft to c.late
        Tone.OK -> c.okSoft to c.ok
    }
    Row(
        Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(12.dp), tint = fg)
        Text(text, style = Type.label.copy(letterSpacing = TextUnit.Unspecified, fontSize = 11.5.sp), color = fg)
    }
}

@Composable
fun CountBadge(n: Int) {
    Box(
        Modifier.heightIn(min = 22.dp).widthIn(min = 22.dp).clip(RoundedCornerShape(11.dp)).background(C.colors.accentSoft).padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text("$n", style = Type.data.copy(fontSize = 12.sp), color = C.colors.accent) }
}

/* =========================================================
   Linha de item (tarefa ou memória)
   ========================================================= */
@Composable
fun CheckCircle(done: Boolean, onClick: () -> Unit) {
    val c = C.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClickLabel = if (done) "Marcar como pendente" else "Concluir", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .then(if (done) Modifier.background(c.ok) else Modifier.border(1.75.dp, c.ink3, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Color.White)
        }
    }
}

@Composable
fun MemoryBadge() {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(C.colors.memSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(14.dp), tint = C.colors.mem)
        }
    }
}

@Composable
fun ItemRow(
    item: ItemEntity,
    placeName: String? = null,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    divider: Boolean = true,
) {
    val c = C.colors
    Row(
        modifier.fillMaxWidth().then(if (divider) Modifier.bottomLine(c.line) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.offset(x = (-11).dp)) {
            if (item.kind == ItemKind.TASK) CheckCircle(item.done, onToggle) else MemoryBadge()
        }
        Column(
            Modifier.weight(1f).offset(x = (-11).dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 2.dp),
        ) {
            Text(
                item.title,
                style = Type.body,
                color = if (item.done) c.ink3 else c.ink,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = itemMeta(item, placeName)
            if (meta.isNotEmpty()) {
                Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    meta.forEachIndexed { i, m ->
                        if (i > 0) Text("·", style = Type.meta, color = c.ink3)
                        if (m.tag) Tag(m.text, Tone.MEMORY)
                        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (m.icon != null) Icon(m.icon, null, Modifier.size(14.dp), tint = c.ink3)
                            Text(m.text, style = Type.meta, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private data class Meta(val text: String, val icon: ImageVector? = null, val tag: Boolean = false)

private fun itemMeta(item: ItemEntity, placeName: String?): List<Meta> {
    val out = mutableListOf<Meta>()
    if (item.kind == ItemKind.MEMORY) {
        out += Meta("Memória", tag = true)
        when {
            item.fieldValue != null -> out += Meta(item.fieldValue)
            item.fieldLabel != null -> out += Meta("anotar lá")
        }
    } else {
        when (item.remind) {
            RemindWhen.LEAVE -> out += Meta("Ao sair", Icons.AutoMirrored.Outlined.Logout)
            RemindWhen.TIME -> item.timeOfDayMin?.let { out += Meta(Words.time(it), Icons.Outlined.Schedule) }
            RemindWhen.NONE -> out += Meta("Sem aviso", Icons.Outlined.NotificationsOff)
            RemindWhen.ARRIVE -> if (placeName != null) Unit
        }
        if (item.repeat != Repeat.ONCE) out += Meta(repeatLabel(item.repeat), Icons.Outlined.Repeat)
        if (item.snoozed) out += Meta("próxima visita")
    }
    if (placeName != null) out += Meta(placeName, if (item.remind == RemindWhen.ARRIVE && item.kind == ItemKind.TASK) Icons.AutoMirrored.Outlined.Login else null)
    return out.take(3)
}

fun repeatLabel(r: Repeat) = when (r) {
    Repeat.ONCE -> "Uma vez"
    Repeat.DAILY -> "Diária"
    Repeat.WEEKLY -> "Semanal"
    Repeat.MONTHLY -> "Mensal"
}

/* =========================================================
   Controles
   ========================================================= */
@Composable
fun RoteiroChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    dashed: Boolean = false,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val c = C.colors
    val bg = when { selected -> c.ink; accent -> c.accentSoft; else -> c.bg }
    val fg = when { selected -> c.bg; accent -> c.accent; else -> c.ink }
    val border = if (selected || accent) Color.Transparent else c.line
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .then(
                if (dashed) Modifier.dashedBorder(c.line, 18.dp)
                else Modifier.border(BorderStroke(1.dp, border), RoundedCornerShape(18.dp))
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(16.dp), tint = if (selected) c.bg else if (accent) c.accent else c.ink2)
        if (text.isNotEmpty()) Text(text, style = Type.secondary.copy(fontWeight = FontWeight.Medium), color = if (dashed) c.ink2 else fg, maxLines = 1)
    }
}

/** Controle segmentado (Uma vez / Diária / Semanal / Mensal). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = C.colors
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(3.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.bg else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = Type.secondary.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (on) c.ink else c.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

enum class ButtonKind { PRIMARY, SECONDARY, OUTLINE, GHOST, DANGER }

@Composable
fun RoteiroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    small: Boolean = false,
    icon: ImageVector? = null,
) {
    val c = C.colors
    val (bg, fg) = when {
        !enabled -> c.surface2 to c.ink3
        kind == ButtonKind.PRIMARY -> c.accent to c.accentInk
        kind == ButtonKind.SECONDARY -> c.surface to c.ink
        kind == ButtonKind.OUTLINE -> c.bg to c.ink
        kind == ButtonKind.GHOST -> Color.Transparent to c.accent
        else -> c.lateSoft to c.late
    }
    Row(
        modifier
            .height(if (small) 38.dp else 52.dp)
            .clip(RoundedCornerShape(if (small) 11.dp else 14.dp))
            .background(bg)
            .then(if (kind == ButtonKind.OUTLINE && enabled) Modifier.border(1.dp, c.line, RoundedCornerShape(if (small) 11.dp else 14.dp)) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = fg)
        Text(text, style = if (small) Type.secondary.copy(fontWeight = FontWeight.SemiBold) else Type.button, color = fg, maxLines = 1)
    }
}

/** Campo de texto com fundo de superfície (sem contorno), como no design. */
@Composable
fun RoteiroField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    textStyle: TextStyle = Type.bodyRegular,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = C.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = textStyle.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (leading != null) Icon(leading, null, Modifier.size(20.dp), tint = c.ink2)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = c.ink3)
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/** Título grande das telas de criação ("Comprar café"). */
@Composable
fun TitleInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, cursor: Color = C.colors.accent) {
    val c = C.colors
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(80)) },
        textStyle = Type.titleInput.copy(color = c.ink),
        cursorBrush = SolidColor(cursor),
        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = Type.titleInput, color = c.ink3)
                inner()
            }
        },
    )
}

@Composable
fun FieldLabel(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
        Text(text.uppercase(), style = Type.label, color = C.colors.ink3)
        Spacer(Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Type.meta, color = C.colors.ink3)
    }
}

/** Grupo de opções com rádio (fundo de superfície). */
@Composable
fun OptionGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).background(C.colors.surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
        content()
    }
}

@Composable
fun OptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    divider: Boolean = true,
) {
    val c = C.colors
    Row(
        Modifier.fillMaxWidth()
            .then(if (divider) Modifier.bottomLine(c.line) else Modifier)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = c.ink2)
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.bodyRegular.copy(fontSize = 15.sp), color = c.ink)
            if (subtitle != null) Text(subtitle, style = Type.meta, color = c.ink3)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .border(if (selected) 7.dp else 1.75.dp, if (selected) c.accent else c.ink3, CircleShape)
        )
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    divider: Boolean = true,
) {
    val c = C.colors
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.bottomLine(c.line) else Modifier)
            .clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = c.ink2)
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.bodyRegular.copy(fontSize = 15.sp), color = c.ink)
            if (subtitle != null) Text(subtitle, style = Type.meta, color = c.ink3)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.ok, checkedThumbColor = Color.White, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.surface2, uncheckedThumbColor = Color.White),
        )
    }
}

enum class BannerKind { WARN, INFO }

@Composable
fun Banner(kind: BannerKind, icon: ImageVector, title: String, text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = C.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (kind == BannerKind.WARN) c.lateSoft else c.accentSoft).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (kind == BannerKind.WARN) c.late else c.accent)
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
            Text(text, style = Type.secondary, color = c.ink)
            if (action != null && onAction != null) {
                Text(action, style = Type.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.accent,
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onAction).padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, tone: Tone = Tone.ACCENT, action: @Composable (() -> Unit)? = null) {
    val c = C.colors
    val (bg, fg) = when (tone) {
        Tone.OK -> c.okSoft to c.ok
        Tone.MEMORY -> c.memSoft to c.mem
        else -> c.accentSoft to c.accent
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(132.dp)) {
                drawCircle(fg.copy(alpha = 0.5f), size.minDimension / 2 - 2f, style = Stroke(1.5f * density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4 * density, 6 * density))))
                drawCircle(bg, size.minDimension / 3)
            }
            Icon(icon, null, Modifier.size(40.dp), tint = fg)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = Type.h2.copy(fontSize = 21.sp), color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, style = Type.bodyRegular.copy(fontSize = 15.sp), color = c.ink2, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun StatusText(text: String, tone: Tone) {
    val c = C.colors
    val color = when (tone) { Tone.OK -> c.ok; Tone.LATE -> c.late; Tone.MEMORY -> c.mem; else -> c.ink3 }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, style = Type.meta.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

@Composable
fun PlaceRow(
    icon: ImageVector,
    name: String,
    meta: String,
    onClick: () -> Unit,
    here: Boolean = false,
    distance: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = C.colors
    Row(
        Modifier.fillMaxWidth().bottomLine(c.line).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (here) c.accent else c.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = if (here) c.accentInk else c.ink)
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = Type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(meta, style = Type.meta, color = if (here) c.accent else c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (distance != null) Text(distance, style = Type.data.copy(fontSize = 12.5.sp), color = c.ink3)
        trailing?.invoke()
    }
}

/* =========================================================
   Modificadores auxiliares
   ========================================================= */
fun Modifier.bottomLine(color: Color): Modifier = this.drawBehind {
    drawLine(color, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), strokeWidth = 1f)
}

fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = this.drawBehind {
    val r = radius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
    )
}
