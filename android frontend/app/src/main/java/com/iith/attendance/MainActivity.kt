package com.iith.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iith.attendance.ui.navigation.ProfessorNav
import com.iith.attendance.ui.navigation.StudentNav
import com.iith.attendance.ui.screens.AdminHomeScreen
import com.iith.attendance.ui.screens.LoginScreen
import com.iith.attendance.ui.screens.RoleSelectionScreen
import com.iith.attendance.ui.theme.IITHAttendanceTheme

enum class UserRole { STUDENT, PROFESSOR, ADMIN }

data class UserSession(
    val role: UserRole,
    val name: String,
    val email: String,
    val id: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IITHAttendanceTheme(forceLight = true) {
                Surface(color = Color(0xFFF1F3F4)) {
                    AttendanceApp()
                }
            }
        }
    }
}

@Composable
private fun AttendanceApp() {
    val navController = rememberNavController()
    var session by remember { mutableStateOf<UserSession?>(null) }

    NavHost(navController = navController, startDestination = "role_selection") {
        composable("role_selection") {
            RoleSelectionScreen(
                onContinue = { role -> navController.navigate("login/${role.name}") }
            )
        }
        composable(
            route = "login/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStack ->
            val role = UserRole.valueOf(backStack.arguments?.getString("role") ?: UserRole.STUDENT.name)
            LoginScreen(role = role) { newSession ->
                session = newSession
                when (newSession.role) {
                    UserRole.STUDENT -> navController.navigate("student") {
                        popUpTo("role_selection") { inclusive = false }
                    }
                    UserRole.PROFESSOR -> navController.navigate("professor") {
                        popUpTo("role_selection") { inclusive = false }
                    }
                    UserRole.ADMIN -> navController.navigate("admin") {
                        popUpTo("role_selection") { inclusive = false }
                    }
                }
            }
        }
        composable("student") {
            StudentNav(
                session = session,
                onLogout = {
                    session = null
                    navController.navigate("role_selection") { popUpTo("role_selection") { inclusive = true } }
                }
            )
        }
        composable("professor") {
            ProfessorNav(
                session = session,
                onLogout = {
                    session = null
                    navController.navigate("role_selection") { popUpTo("role_selection") { inclusive = true } }
                }
            )
        }
        composable("admin") {
            AdminHomeScreen(session = session) {
                session = null
                navController.navigate("role_selection") { popUpTo("role_selection") { inclusive = true } }
            }
        }
    }
}
