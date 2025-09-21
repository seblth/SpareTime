package com.example.sparetimeapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// 1) Einmal zentral definieren
private val DarkColors = darkColorScheme(
    primary          = BlueOutline,
    onPrimary        = TextPrimary,
    background       = Bg,            // <- dein Bg aus Color.kt
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline          = BlueOutline,
)

private val AppShapes = Shapes(
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(20.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp) // „Pill“
)

// 2) Und hier einfach wiederverwenden
@Composable
fun SpareTimeAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = Typography,
        shapes      = AppShapes,
        content     = content
    )
}