package com.iith.attendanceapp

import androidx.compose.runtime.*

enum class UserRole { STUDENT, PROFESSOR, ADMIN }

// Tracks what screen to show after role selection
private sealed class AppScreen {
    object RoleSelect : AppScreen()
    data class Login(val role: UserRole) : AppScreen()
    // Student-only: after login, choose enroll or enter
    data class StudentEnroll(val userId: String) : AppScreen()
    data class StudentHome(val userId: String) : AppScreen()
    object ProfessorHome : AppScreen()
    object AdminHome : AppScreen()
}

@Composable
fun AttendanceApp() {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.RoleSelect) }

    when (val s = screen) {
        is AppScreen.RoleSelect -> RoleSelectionScreen { role ->
            screen = AppScreen.Login(role)
        }

        is AppScreen.Login -> LoginScreen(
            role = s.role,
            onEnter = { userId ->
                screen = when (s.role) {
                    UserRole.STUDENT   -> AppScreen.StudentHome(userId)
                    UserRole.PROFESSOR -> AppScreen.ProfessorHome
                    UserRole.ADMIN     -> AppScreen.AdminHome
                }
            },
            onEnroll = { userId ->
                // Only students enroll; professors/admins go straight in
                screen = AppScreen.StudentEnroll(userId)
            }
        )

        is AppScreen.StudentEnroll -> FaceEnrollScreen(
            userId = s.userId,
            onDone = { screen = AppScreen.StudentHome(s.userId) },
            onBack = { screen = AppScreen.Login(UserRole.STUDENT) }
        )

        is AppScreen.StudentHome -> StudentTabView(
            userId   = s.userId,
            onLogout = { screen = AppScreen.RoleSelect }
        )

        is AppScreen.ProfessorHome -> ProfessorTabView(
            onLogout = { screen = AppScreen.RoleSelect }
        )

        is AppScreen.AdminHome -> AdminTabView(
            onLogout = { screen = AppScreen.RoleSelect }
        )
    }
}
