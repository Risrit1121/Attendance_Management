package com.iith.attendanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GBlue   = Color(0xFF1A73E8)
val GGreen  = Color(0xFF34A853)
val GOrange = Color(0xFFFB7817)
val GPurple = Color(0xFF6A5ACD)
val BGGray  = Color(0xFFF0F0F0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels(this)
        setContent { AttendanceApp() }
    }
}
