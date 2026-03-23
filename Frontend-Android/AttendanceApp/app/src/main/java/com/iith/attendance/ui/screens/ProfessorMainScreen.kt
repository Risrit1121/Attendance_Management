package com.iith.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.iith.attendance.ui.theme.IITHBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showProfile by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Portal", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showProfile = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IITHBlue,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon     = { Icon(Icons.Default.CalendarMonth, null) },
                    label    = { Text("Schedule") },
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Default.MenuBook, null) },
                    label    = { Text("Courses") },
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Default.BarChart, null) },
                    label    = { Text("Analytics") },
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ProfessorScheduleScreen()
                1 -> ProfessorCoursesScreen()
                2 -> ProfessorAnalyticsScreen()
            }
        }
    }

    if (showProfile) {
        ProfileSheet(
            name      = "Professor User",
            email     = "prof@iith.ac.in",
            id        = "PROF001",
            onLogout  = onLogout,
            onDismiss = { showProfile = false }
        )
    }
}
