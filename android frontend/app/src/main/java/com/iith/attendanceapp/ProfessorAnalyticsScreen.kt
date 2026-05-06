package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorAnalyticsScreen(professorId: String, token: String) {
    var courses    by remember { mutableStateOf<List<ProfCourse>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var selected   by remember { mutableStateOf<ProfCourse?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    fun load() {
        apiGetProfCourses(professorId, token) { result, err ->
            loading = false; refreshing = false
            if (result != null) courses = result else errorMsg = err
        }
    }

    LaunchedEffect(professorId) { load() }

    if (selected != null) {
        CourseAnalyticsDetailScreen(
            course      = selected!!,
            professorId = professorId,
            token       = token,
            onBack      = { selected = null }
        )
        return
    }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; load() }) {
        Column(modifier = Modifier.fillMaxSize().background(BGGray)
            .verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GBlue)
                }
                errorMsg != null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMsg", color = Color.Red, textAlign = TextAlign.Center)
                }
                courses.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No courses found.", color = Color.Gray)
                }
                else -> courses.forEachIndexed { i, course ->
                    ProfCourseCard(course = course, index = i, onClick = { selected = course })
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseAnalyticsDetailScreen(course: ProfCourse, professorId: String, token: String, onBack: () -> Unit) {
    var records    by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    fun load() {
        apiGetCourseAnalytics(professorId, course.courseCode, token) { result, err ->
            loading = false; refreshing = false
            if (result != null) records = result else errorMsg = err
        }
    }

    LaunchedEffect(course.id) { load() }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text(course.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; load() }) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GBlue)
                }
                errorMsg != null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMsg", color = Color.Red, textAlign = TextAlign.Center)
                }
                else -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    records.forEach { student ->
                        val pct   = student.percentage
                        val color = when { pct >= 75 -> GGreen; pct >= 60 -> GOrange; else -> Color.Red }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(student.studentId, fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                SolidProgressBar((pct / 100).toFloat(), color, Modifier.width(140.dp).height(5.dp))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${pct.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                                Text("${student.attended}/${student.total}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    if (records.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No attendance data yet.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
