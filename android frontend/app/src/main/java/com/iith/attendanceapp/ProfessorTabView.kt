package com.iith.attendanceapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorTabView(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var showProfile by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("session") }

    val tabTitle = when (currentTab) {
        "session"   -> "Courses"
        else        -> "Analytics"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tabTitle) },
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
                    icon     = { Icon(Icons.Default.Book, null) },
                    label    = { Text("Courses") }
                )
                NavigationBarItem(
                    selected = currentTab == "analytics",
                    onClick  = { currentTab = "analytics"; navController.navigate("analytics") { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.BarChart, null) },
                    label    = { Text("Analytics") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "session", modifier = Modifier.padding(padding)) {
            composable("session")   { ProfessorSessionScreen() }
            composable("analytics") { ProfessorAnalyticsScreen() }
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
