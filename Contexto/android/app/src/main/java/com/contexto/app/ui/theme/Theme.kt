package com.contexto.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.contexto.app.R

/* =========================================================
   Tokens do design system (mesmos de design/css/tokens.css)
   ========================================================= */

@Immutable
data class ContextoColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val mem: Color,
    val memSoft: Color,
    val late: Color,
    val lateSoft: Color,
    val ok: Color,
    val okSoft: Color,
    val scrim: Color,
)

val LightColors = ContextoColors(
    bg = Color(0xFFFFFFFF), surface = Color(0xFFF4F5F9), surface2 = Color(0xFFEAECF2),
    ink = Color(0xFF0F121A), ink2 = Color(0xFF555B6E), ink3 = Color(0xFF9197A8), line = Color(0xFFE6E8EF),
    accent = Color(0xFF2F4FF5), accentInk = Color(0xFFFFFFFF), accentSoft = Color(0xFFE9EDFF),
    mem = Color(0xFFA86400), memSoft = Color(0xFFFFF2DB),
    late = Color(0xFFD5373A), lateSoft = Color(0xFFFDECEC),
    ok = Color(0xFF138A5A), okSoft = Color(0xFFE3F4EC),
    scrim = Color(0x610C0F1A),
)

val DarkColors = ContextoColors(
    bg = Color(0xFF0F1117), surface = Color(0xFF181B24), surface2 = Color(0xFF20242F),
    ink = Color(0xFFEEF0F6), ink2 = Color(0xFFA4AABB), ink3 = Color(0xFF6B7185), line = Color(0xFF242834),
    accent = Color(0xFF7D93FF), accentInk = Color(0xFF0A0E26), accentSoft = Color(0xFF1C2347),
    mem = Color(0xFFF2B650), memSoft = Color(0xFF2B2213),
    late = Color(0xFFFF6D6D), lateSoft = Color(0xFF331B1D),
    ok = Color(0xFF42CC90), okSoft = Color(0xFF12291F),
    scrim = Color(0x8C000000),
)

object Space {
    val s1 = 4.dp; val s2 = 8.dp; val s3 = 12.dp; val s4 = 16.dp
    val s5 = 20.dp; val s6 = 24.dp; val s8 = 32.dp; val s10 = 40.dp
    /** Margem lateral das telas. */
    val gutter = 20.dp
}

object Radius {
    val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp
}

val Onest = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
)
val Schibsted = FontFamily(Font(R.font.schibsted_grotesk_semibold, FontWeight.SemiBold))
val PlexMono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium))

/** Escala tipográfica do app. */
object Type {
    val context = TextStyle(fontFamily = Schibsted, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.03).em)
    val h1 = TextStyle(fontFamily = Schibsted, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.025).em)
    val h2 = TextStyle(fontFamily = Schibsted, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.02).em)
    val titleInput = TextStyle(fontFamily = Schibsted, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em)
    val body = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val bodyRegular = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val button = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
    val secondary = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val meta = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val label = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em)
    val tab = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    val data = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp)
}

private val LocalColors = staticCompositionLocalOf { LightColors }

object C {
    val colors: ContextoColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
}

@Composable
fun ContextoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(primary = c.accent, onPrimary = c.accentInk, background = c.bg, surface = c.bg, onSurface = c.ink, onBackground = c.ink, surfaceVariant = c.surface, outline = c.line, error = c.late)
    } else {
        lightColorScheme(primary = c.accent, onPrimary = c.accentInk, background = c.bg, surface = c.bg, onSurface = c.ink, onBackground = c.ink, surfaceVariant = c.surface, outline = c.line, error = c.late)
    }
    CompositionLocalProvider(LocalColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(bodyLarge = Type.bodyRegular, bodyMedium = Type.secondary, labelLarge = Type.button),
            content = content,
        )
    }
}
