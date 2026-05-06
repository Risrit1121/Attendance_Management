package com.iith.attendanceapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorTabView(userId: String, userName: String, token: String, onLogout: () -> Unit) {
    val navController = rememberNavController()
    var showProfile by remember { mutableStateOf(false) }
    var currentTab  by remember { mutableStateOf("session") }

    // Notification poller — runs in background while app is open
    ProfessorNotificationPoller(professorId = userId, token = token)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (currentTab == "session") "Courses" else "Analytics") },
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
                    selected = currentTab == "session",
                    onClick  = { currentTab = "session"; navController.navigate("session") { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.Book, null) }, label = { Text("Courses") }
                )
                NavigationBarItem(
                    selected = currentTab == "analytics",
                    onClick  = { currentTab = "analytics"; navController.navigate("analytics") { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.BarChart, null) }, label = { Text("Analytics") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "session", modifier = Modifier.padding(padding)) {
            composable("session")   { ProfessorSessionScreen(professorId = userId, token = token) }
            composable("analytics") { ProfessorAnalyticsScreen(professorId = userId, token = token) }
        }
    }

    if (showProfile) {
        ModalBottomSheet(onDismissRequest = { showProfile = false }) {
            ProfileSheet(userId = userId, userName = userName,
                onLogout = { showProfile = false; onLogout() }, onClose = { showProfile = false })
        }
    }
}
