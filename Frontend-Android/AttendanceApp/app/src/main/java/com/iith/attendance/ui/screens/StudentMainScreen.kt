package com.iith.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iith.attendance.ui.theme.IITHBlue

// ── Student main shell ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(onLogout: () -> Unit) {
    var selectedTab    by remember { mutableIntStateOf(0) }
    var showProfile    by remember { mutableStateOf(false) }

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
                    icon     = { Icon(Icons.Default.Dashboard, null) },
                    label    = { Text("Dashboard") },
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon     = { Icon(Icons.Default.PlayCircle, null) },
                    label    = { Text("Active Sessions") },
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> StudentDashboardScreen()
                1 -> ActiveSessionsScreen()
            }
        }
    }

    if (showProfile) {
        ProfileSheet(
            name    = "Student User",
            email   = "student@iith.ac.in",
            id      = "ES22BTECH11000",
            onLogout = onLogout,
            onDismiss = { showProfile = false }
        )
    }
}

// ── Profile bottom sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    name: String, email: String, id: String,
    onLogout: () -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp), tint = IITHBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(id, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { onLogout(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Log Out")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
