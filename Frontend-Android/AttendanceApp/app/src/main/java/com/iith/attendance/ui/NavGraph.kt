package com.iith.attendance.ui

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iith.attendance.data.models.UserRole
import com.iith.attendance.ui.screens.*

object Routes {
    const val ROLE_SELECTION = "role_selection"
    const val LOGIN          = "login/{role}"
    const val STUDENT_MAIN   = "student_main"
    const val PROFESSOR_MAIN = "professor_main"

    fun login(role: String) = "login/$role"
}

@Composable
fun AttendanceNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ROLE_SELECTION
    ) {
        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onContinue = { role ->
                    navController.navigate(Routes.login(role.name))
                }
            )
        }

        composable(Routes.LOGIN) { backStack ->
            val roleName = backStack.arguments?.getString("role") ?: "STUDENT"
            val role     = UserRole.valueOf(roleName)
            LoginScreen(
                role = role,
                onLoginSuccess = {
                    val dest = if (role == UserRole.STUDENT) Routes.STUDENT_MAIN
                               else Routes.PROFESSOR_MAIN
                    navController.navigate(dest) {
                        popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.STUDENT_MAIN) {
            StudentMainScreen(
                onLogout = {
                    navController.navigate(Routes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROFESSOR_MAIN) {
            ProfessorMainScreen(
                onLogout = {
                    navController.navigate(Routes.ROLE_SELECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
