package com.iith.attendanceapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ExperimentalMaterial3Api
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTabView(
    userId: String,
    userName: String,
    email: String,
    token: String,
    onLogout: () -> Unit
) {
    val navController  = rememberNavController()
    var showProfile    by remember { mutableStateOf(false) }
    var showCamera     by remember { mutableStateOf(false) }   // lifted outside sheet
    var currentTab     by remember { mutableStateOf("dashboard") }
    var photoUrl       by remember { mutableStateOf<String?>(null) }
    var photoEnrolled  by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        apiGetPhotoStatus(userId, token) { status ->
            photoEnrolled = status.enrolled
            if (status.photoUrl.isNotBlank()) photoUrl = status.photoUrl
        }
    }

    // Camera screen renders full screen — completely outside the bottom sheet
    if (showCamera) {
        PhotoCaptureFullScreen(
            userId  = userId,
            token   = token,
            onDone  = { newUrl ->
                showCamera    = false
                if (newUrl.isNotBlank()) { photoUrl = newUrl; photoEnrolled = true }
            },
            onCancel = { showCamera = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (currentTab == "dashboard") "Analytics" else "Active Sessions") },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(2.dp, GBlue, CircleShape)
                            .clickable { showProfile = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl != null) {
                            Image(painter = rememberAsyncImagePainter(photoUrl),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile",
                                tint = GBlue, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick  = { currentTab = "dashboard"; navController.navigate("dashboard") { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.BarChart, null) },
                    label    = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = currentTab == "sessions",
                    onClick  = { currentTab = "sessions"; navController.navigate("sessions") { launchSingleTop = true } },
                    icon     = { Icon(Icons.Default.Schedule, null) },
                    label    = { Text("Active Sessions") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { StudentDashboardScreen(userId = userId, token = token) }
            composable("sessions")  { StudentSessionsScreen(userId = userId, token = token) }
        }
    }

    if (showProfile) {
        ModalBottomSheet(onDismissRequest = { showProfile = false }) {
            StudentProfileSheet(
                userId        = userId,
                userName      = userName,
                email         = email,
                token         = token,
                photoUrl      = photoUrl,
                photoEnrolled = photoEnrolled,
                onUploadPhoto = {
                    showProfile = false   // close sheet first
                    showCamera  = true    // then open full-screen camera
                },
                onPhotoUpdated = { newUrl ->
                    photoUrl      = newUrl
                    photoEnrolled = true
                    showProfile   = false
                },
                onLogout = { showProfile = false; onLogout() },
                onClose  = { showProfile = false }
            )
        }
    }
}
