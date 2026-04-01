package com.iith.attendanceapp

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class UserRole { STUDENT, PROFESSOR }

@Composable
fun AttendanceApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(UserRole.STUDENT) }

    if (isLoggedIn) {
        when (role) {
            UserRole.STUDENT   -> StudentTabView(onLogout = { isLoggedIn = false })
            UserRole.PROFESSOR -> ProfessorTabView(onLogout = { isLoggedIn = false })
        }
    } else {
        RoleSelectionScreen(
            onContinue = { selectedRole ->
                role = selectedRole
                isLoggedIn = true
            }
        )
    }
}
