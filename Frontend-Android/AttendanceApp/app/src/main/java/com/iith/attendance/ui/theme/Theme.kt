package com.iith.attendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary brand blue: matches iOS Color(red:0.102, green:0.451, blue:0.910) = #1A73E8
val IITHBlue        = Color(0xFF1A73E8)
val IITHBlueDark    = Color(0xFF1558B0)
val IITHBlueLight   = Color(0xFFD2E3FC)

val AttendanceGreen  = Color(0xFF34A853)   // ≥ 75 %
val AttendanceOrange = Color(0xFFFFA726)   // ≥ 60 %
val AttendanceRed    = Color(0xFFE53935)   // <  60 %

val SurfaceGray = Color(0xFFF1F3F4)
val CardWhite   = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary          = IITHBlue,
    onPrimary        = Color.White,
    primaryContainer = IITHBlueLight,
    secondary        = IITHBlueDark,
    background       = SurfaceGray,
    surface          = CardWhite,
    onBackground     = Color(0xFF202124),
    onSurface        = Color(0xFF202124),
)

@Composable
fun AttendanceAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
