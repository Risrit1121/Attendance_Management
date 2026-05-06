package com.iith.attendanceapp

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

enum class UserRole { STUDENT, PROFESSOR }

private sealed class AppScreen {
    object RoleSelect : AppScreen()
    object LivenessTest : AppScreen()
    data class Login(val role: UserRole) : AppScreen()
    data class StudentHome(val userId: String, val userName: String, val email: String, val token: String) : AppScreen()
    data class ProfessorHome(val userId: String, val userName: String, val token: String) : AppScreen()
}

@Composable
fun AttendanceApp() {
    val context = LocalContext.current
    var screen  by remember {
        // Auto-login: restore saved credentials on startup
        val saved = CredentialStore.load(context)
        val initial: AppScreen = when {
            saved == null -> AppScreen.RoleSelect
            saved.role == "prof" -> AppScreen.ProfessorHome(saved.userId, saved.userName, saved.token)
            else -> AppScreen.StudentHome(saved.userId, saved.userName, saved.email, saved.token)
        }
        mutableStateOf(initial)
    }

    fun logout() {
        CredentialStore.clear(context)
        NotificationWorker.cancel(context)
        screen = AppScreen.RoleSelect
    }

    when (val s = screen) {
        is AppScreen.RoleSelect -> RoleSelectionScreen(
            onContinue     = { role -> screen = AppScreen.Login(role) },
            onTestLiveness = { screen = AppScreen.LivenessTest }
        )

        is AppScreen.LivenessTest -> LivenessTestScreen(onBack = { screen = AppScreen.RoleSelect })

        is AppScreen.Login -> LoginScreen(
            role    = s.role,
            onLogin = { userId, userName, email, token, role ->
                // Persist credentials and start background worker
                CredentialStore.save(context, userId, userName, email, token, role)
                NotificationWorker.schedule(context)
                screen = when (s.role) {
                    UserRole.STUDENT   -> AppScreen.StudentHome(userId, userName, email, token)
                    UserRole.PROFESSOR -> AppScreen.ProfessorHome(userId, userName, token)
                }
            }
        )

        is AppScreen.StudentHome -> StudentTabView(
            userId   = s.userId,
            userName = s.userName,
            email    = s.email,
            token    = s.token,
            onLogout = { logout() }
        )

        is AppScreen.ProfessorHome -> ProfessorTabView(
            userId   = s.userId,
            userName = s.userName,
            token    = s.token,
            onLogout = { logout() }
        )
    }
}
