package com.iith.attendance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.BannerCard
import com.iith.attendance.ui.components.MainScaffold

data class CourseAttendance(val name: String, val code: String, val percent: Int)
data class StudentAnalytics(val name: String, val roll: String, val attended: Int, val total: Int)

@Composable
fun ProfessorAnalyticsScreen(
    session: UserSession?,
    onLogout: () -> Unit,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val courses = listOf(
        CourseAttendance("Operating Systems", "CS301", 76),
        CourseAttendance("Data Mining", "CS402", 69)
    ) // TODO: replace with API call

    MainScaffold("Analytics", session, onLogout) { modifier ->
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(courses) { course ->
                Column(modifier = Modifier.clickable { navController.navigate("course_analytics_detail/${course.code}") }) {
                    BannerCard(title = course.name, bannerColor = Color(0xFF1A73E8)) {
                        Text("${course.code} • Overall ${course.percent}%")
                    }
                }
            }
        }
    }
}

@Composable
fun CourseAnalyticsDetailScreen(
    code: String,
    session: UserSession?,
    onLogout: () -> Unit,
    paddingValues: PaddingValues
) {
    val students = listOf(
        StudentAnalytics("Ananya Reddy", "CS22BTECH11010", 23, 28),
        StudentAnalytics("Rahul N", "CS22BTECH11022", 18, 28),
        StudentAnalytics("Ishita M", "CS22BTECH11031", 15, 28)
    ) // TODO: replace with API call

    MainScaffold("$code Analytics", session, onLogout) { modifier ->
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(students) { student ->
                val p = student.attended.toFloat() / student.total
                val color = when {
                    p >= 0.75f -> Color(0xFF34A853)
                    p >= 0.60f -> Color(0xFFFB8C00)
                    else -> Color(0xFFEA4335)
                }
                androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${student.name} (${student.roll})")
                        Text("${student.attended}/${student.total}")
                        LinearProgressIndicator(progress = { p }, color = color, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
