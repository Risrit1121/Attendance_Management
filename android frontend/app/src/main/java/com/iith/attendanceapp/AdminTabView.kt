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
fun AdminTabView(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var showProfile by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("courses") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (currentTab == "courses") "Courses" else "Analytics") },
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
                    selected = currentTab == "courses",
                    onClick = { currentTab = "courses"; navController.navigate("courses") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Book, null) },
                    label = { Text("Courses") }
                )
                NavigationBarItem(
                    selected = currentTab == "analytics",
                    onClick = { currentTab = "analytics"; navController.navigate("analytics") { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Analytics") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "courses", modifier = Modifier.padding(padding)) {
            composable("courses")   { AdminCoursesScreen() }
            composable("analytics") { AdminAnalyticsScreen() }
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
