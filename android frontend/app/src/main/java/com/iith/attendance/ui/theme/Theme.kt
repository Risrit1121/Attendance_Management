package com.iith.attendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GoogleBlue = Color(0xFF1A73E8)
val ScaffoldGray = Color(0xFFF1F3F4)

private val LightScheme = lightColorScheme(
    primary = GoogleBlue,
    background = ScaffoldGray,
    surface = Color.White
)

@Composable
fun IITHAttendanceTheme(forceLight: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = Typography,
        content = content
    )
}
