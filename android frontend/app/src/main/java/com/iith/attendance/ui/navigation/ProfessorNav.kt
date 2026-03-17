package com.iith.attendance.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iith.attendance.UserSession
import com.iith.attendance.ui.screens.ClassDetailScreen
import com.iith.attendance.ui.screens.CourseAnalyticsDetailScreen
import com.iith.attendance.ui.screens.ProfBLESessionScreen
import com.iith.attendance.ui.screens.ProfManualSessionScreen
import com.iith.attendance.ui.screens.ProfQRSessionScreen
import com.iith.attendance.ui.screens.ProfessorAnalyticsScreen
import com.iith.attendance.ui.screens.ProfessorCoursesPlaceholder
import com.iith.attendance.ui.screens.ProfessorScheduleScreen

@Composable
fun ProfessorNav(session: UserSession?, onLogout: () -> Unit) {
    var selected by remember { mutableStateOf(0) }
    val rootTabs = listOf("Schedule", "Courses", "Analytics")
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                rootTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = {
                            selected = index
                            val route = when (index) {
                                0 -> "schedule"
                                1 -> "courses"
                                else -> "analytics"
                            }
                            navController.navigate(route)
                        },
                        icon = {
                            Icon(
                                if (index == 0) Icons.Default.Schedule else if (index == 1) Icons.Default.Book else Icons.Default.Analytics,
                                contentDescription = tab
                            )
                        },
                        label = { Text(tab) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "schedule") {
            composable("schedule") { ProfessorScheduleScreen(session, onLogout, navController, padding) }
            composable("courses") { ProfessorCoursesPlaceholder(session, onLogout, padding) }
            composable("analytics") { ProfessorAnalyticsScreen(session, onLogout, navController, padding) }
            composable("class_detail/{classCode}") { backStack ->
                ClassDetailScreen(
                    classCode = backStack.arguments?.getString("classCode").orEmpty(),
                    session = session,
                    onLogout = onLogout,
                    navController = navController,
                    paddingValues = padding
                )
            }
            composable("prof_qr_session") { ProfQRSessionScreen(session, onLogout, padding) }
            composable("prof_ble_session") { ProfBLESessionScreen(session, onLogout, padding) }
            composable("prof_manual_session") { ProfManualSessionScreen(session, onLogout, padding) }
            composable("course_analytics_detail/{courseCode}") { backStack ->
                CourseAnalyticsDetailScreen(
                    code = backStack.arguments?.getString("courseCode").orEmpty(),
                    session = session,
                    onLogout = onLogout,
                    paddingValues = padding
                )
            }
        }
    }
}
