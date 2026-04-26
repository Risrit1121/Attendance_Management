package com.iith.attendanceapp

import androidx.compose.runtime.*

enum class UserRole { STUDENT, PROFESSOR, ADMIN }

private sealed class AppScreen {
    object RoleSelect : AppScreen()
    object LivenessTest : AppScreen()
    data class Login(val role: UserRole) : AppScreen()
    data class StudentHome(val userId: String, val userName: String, val email: String, val token: String) : AppScreen()
    data class ProfessorHome(val userId: String, val userName: String, val token: String) : AppScreen()
    data class AdminHome(val userId: String, val userName: String, val token: String) : AppScreen()
}

@Composable
fun AttendanceApp() {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.RoleSelect) }

    when (val s = screen) {
        is AppScreen.RoleSelect -> RoleSelectionScreen(
            onContinue = { role -> screen = AppScreen.Login(role) },
            onTestLiveness = { screen = AppScreen.LivenessTest }
        )

        is AppScreen.LivenessTest -> LivenessTestScreen(onBack = { screen = AppScreen.RoleSelect })

        is AppScreen.Login -> LoginScreen(
            role    = s.role,
            onLogin = { userId, userName, email, token ->
                screen = when (s.role) {
                    UserRole.STUDENT   -> AppScreen.StudentHome(userId, userName, email, token)
                    UserRole.PROFESSOR -> AppScreen.ProfessorHome(userId, userName, token)
                    UserRole.ADMIN     -> AppScreen.AdminHome(userId, userName, token)
                }
            }
        )

        is AppScreen.StudentHome -> StudentTabView(
            userId   = s.userId,
            userName = s.userName,
            email    = s.email,
            token    = s.token,
            onLogout = { screen = AppScreen.RoleSelect }
        )

        is AppScreen.ProfessorHome -> ProfessorTabView(
            userId   = s.userId,
            userName = s.userName,
            token    = s.token,
            onLogout = { screen = AppScreen.RoleSelect }
        )

        is AppScreen.AdminHome -> AdminTabView(
            userId   = s.userId,
            userName = s.userName,
            token    = s.token,
            onLogout = { screen = AppScreen.RoleSelect }
        )
    }
}
