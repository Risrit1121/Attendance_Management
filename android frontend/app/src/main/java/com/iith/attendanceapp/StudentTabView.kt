package com.iith.attendanceapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTabView(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var showProfile by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("dashboard") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (currentTab == "dashboard") "Analytics" else "Active Sessions") },
                actions = {
                    IconButton(onClick = { showProfile = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = GBlue)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { currentTab = "dashboard"; navController.navigate("dashboard") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentTab == "sessions",
                    onClick = { currentTab = "sessions"; navController.navigate("sessions") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Schedule, null) },
                    label = { Text("Active Sessions") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { StudentDashboardScreen() }
            composable("sessions")  { StudentSessionsScreen() }
        }
    }

    if (showProfile) {
        ModalBottomSheet(onDismissRequest = { showProfile = false }) {
            ProfileSheet(
                onLogout = { showProfile = false; onLogout() },
                onClose  = { showProfile = false }
            )
        }
    }
}
