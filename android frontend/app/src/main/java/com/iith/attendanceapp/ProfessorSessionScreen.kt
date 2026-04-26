package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class ProfCourseScreen {
    object List : ProfCourseScreen()
    data class ModeSelect(val course: ProfCourse) : ProfCourseScreen()
    data class QR(val course: ProfCourse) : ProfCourseScreen()
    data class BLE(val course: ProfCourse) : ProfCourseScreen()
    data class Manual(val course: ProfCourse) : ProfCourseScreen()
}

@Composable
fun ProfessorSessionScreen(professorId: String, token: String) {
    var screen by remember { mutableStateOf<ProfCourseScreen>(ProfCourseScreen.List) }

    when (val s = screen) {
        is ProfCourseScreen.List       -> CourseListScreen(professorId, token) { screen = ProfCourseScreen.ModeSelect(it) }
        is ProfCourseScreen.ModeSelect -> ModeSelectScreen(s.course,
            onQR     = { screen = ProfCourseScreen.QR(s.course) },
            onBLE    = { screen = ProfCourseScreen.BLE(s.course) },
            onManual = { screen = ProfCourseScreen.Manual(s.course) },
            onBack   = { screen = ProfCourseScreen.List }
        )
        is ProfCourseScreen.QR     -> ProfQRSessionScreen(s.course, token)     { screen = ProfCourseScreen.ModeSelect(s.course) }
        is ProfCourseScreen.BLE    -> ProfBLESessionScreen(s.course, token)    { screen = ProfCourseScreen.ModeSelect(s.course) }
        is ProfCourseScreen.Manual -> ProfManualSessionScreen(s.course, token) { screen = ProfCourseScreen.ModeSelect(s.course) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseListScreen(professorId: String, token: String, onSelect: (ProfCourse) -> Unit) {
    var courses    by remember { mutableStateOf<List<ProfCourse>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    fun load() {
        apiGetProfCourses(professorId, token) { result, err ->
            loading = false; refreshing = false
            if (result != null) courses = result else errorMsg = err
        }
    }

    LaunchedEffect(professorId) { load() }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; load() }) {
        Column(modifier = Modifier.fillMaxSize().background(BGGray)
            .verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GBlue)
                }
                errorMsg != null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                courses.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No courses assigned.", color = Color.Gray)
                }
                else -> courses.forEachIndexed { i, course ->
                    ProfCourseCard(course = course, index = i, onClick = { onSelect(course) })
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeSelectScreen(course: ProfCourse, onQR: () -> Unit, onBLE: () -> Unit, onManual: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BGGray).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) { Text("← Back", color = GBlue) }
        BannerCard(course.name, courseBannerColors[0], Modifier.padding(horizontal = 16.dp)) {
            Text("${course.slot}  •  ${course.venue}", fontSize = 14.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(24.dp))
        Text("Choose Attendance Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(14.dp))
        ModeCard("QR Code", "Students scan a dynamic QR code displayed by you",                GBlue,   onQR)
        Spacer(Modifier.height(12.dp))
        ModeCard("BLE",     "Bluetooth beacon detects students automatically in the classroom", GPurple, onBLE)
        Spacer(Modifier.height(12.dp))
        ModeCard("Manual",  "Mark attendance manually from the student list",                   GOrange, onManual)
    }
}

@Composable
private fun ModeCard(title: String, description: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
        .clip(RoundedCornerShape(12.dp)).background(Color.White).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center) {
            Text(title.take(3), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
        Text("›", fontSize = 22.sp, color = Color.LightGray)
    }
}
