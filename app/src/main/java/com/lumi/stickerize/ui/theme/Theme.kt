package com.lumi.stickerize.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LumiBlueLight = Color(0xFF74BBF9)
val LumiBlue = Color(0xFF3E84E8)
val LumiBlueDark = Color(0xFF2F6FDC)
val LumiInk = Color(0xFF0E2A55)
val LumiBg = Color(0xFFF4F8FF)
val LumiTile = Color(0xFFE6EEFA)
val WhatsAppGreen = Color(0xFF1FAF5A)

val LumiGradient = Brush.linearGradient(listOf(LumiBlueLight, LumiBlue, LumiBlueDark))

private val scheme = lightColorScheme(
    primary = LumiBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = LumiInk,
    secondary = LumiBlueDark,
    background = LumiBg,
    onBackground = LumiInk,
    surface = Color.White,
    onSurface = LumiInk,
    surfaceVariant = Color(0xFFEAF2FF),
    onSurfaceVariant = Color(0xFF4A5B78),
    outline = Color(0xFFB9C9E2),
)

@Composable
fun LumiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
