package com.iith.attendance.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.iith.attendance.UserSession
import com.iith.attendance.ui.screens.DashboardScreen
import com.iith.attendance.ui.screens.StudentActiveSessionsScreen

@Composable
fun StudentNav(session: UserSession?, onLogout: () -> Unit) {
    var selected by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Active Sessions")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        label = { Text(tab) },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.Dashboard else Icons.Default.PlayCircle,
                                contentDescription = tab
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        when (selected) {
            0 -> DashboardScreen(session, onLogout, padding)
            else -> StudentActiveSessionsScreen(session, onLogout, padding)
        }
    }
}
