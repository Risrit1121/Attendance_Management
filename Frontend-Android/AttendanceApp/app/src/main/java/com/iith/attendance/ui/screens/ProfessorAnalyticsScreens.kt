package com.iith.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iith.attendance.data.models.*
import com.iith.attendance.ui.components.*
import com.iith.attendance.ui.theme.*
import kotlin.math.roundToInt

// ── Mock analytics data ───────────────────────────────────────────────────────

private val mockAnalytics = listOf(
    CourseAnalytics("c1", "Signals & Systems", "EE2030", 0xFF1A73E8, 0.84f, listOf(
        StudentAnalytics("Arjun Sharma",  "ES22BTECH11001", 18, 22),
        StudentAnalytics("Priya Nair",    "ES22BTECH11002", 20, 22),
        StudentAnalytics("Rahul Verma",   "ES22BTECH11003", 12, 22),
        StudentAnalytics("Sneha Reddy",   "ES22BTECH11004", 16, 22),
        StudentAnalytics("Kiran Kumar",   "ES22BTECH11005", 8,  22),
    )),
    CourseAnalytics("c2", "Digital Electronics", "EC2010", 0xFF34A853, 0.76f, listOf(
        StudentAnalytics("Arjun Sharma",  "ES22BTECH11001", 16, 20),
        StudentAnalytics("Priya Nair",    "ES22BTECH11002", 14, 20),
        StudentAnalytics("Rahul Verma",   "ES22BTECH11003", 18, 20),
    )),
    CourseAnalytics("c3", "VLSI Design", "EC4050", 0xFFE91E63, 0.61f, listOf(
        StudentAnalytics("Sneha Reddy",   "ES22BTECH11004", 9, 15),
        StudentAnalytics("Kiran Kumar",   "ES22BTECH11005", 7, 15),
        StudentAnalytics("Anika Patel",   "ES22BTECH11006", 12, 15),
    )),
)

// ── Courses tab (quick session start) ────────────────────────────────────────

@Composable
fun ProfessorCoursesScreen() {
    var sessionMode by remember { mutableStateOf<ProfMode?>(null) }

    when (sessionMode) {
        ProfMode.QR     -> ProfQRSessionScreen   { sessionMode = null }
        ProfMode.BLE    -> ProfBLESessionScreen  { sessionMode = null }
        ProfMode.MANUAL -> ProfManualSessionScreen(
            listOf(
                StudentRecord("1","Arjun Sharma","ES22BTECH11001"),
                StudentRecord("2","Priya Nair","ES22BTECH11002"),
                StudentRecord("3","Rahul Verma","ES22BTECH11003"),
            )
        ) { sessionMode = null }
        null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceGray)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Today's Classes")
            // Today's subset of classes
            mockClasses.take(2).forEach { cls ->
                TodayClassCard(cls) { mode -> sessionMode = mode }
            }
        }
    }
}

@Composable
private fun TodayClassCard(cls: ScheduledClass, onStart: (ProfMode) -> Unit) {
    BannerCard(bannerColor = Color(cls.bannerColor)) {
        Text(cls.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("${cls.code} · ${cls.room} · ${cls.time}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallStartButton("QR", Color(0xFF34A853)) { onStart(ProfMode.QR) }
            SmallStartButton("BLE", IITHBlue)         { onStart(ProfMode.BLE) }
            SmallStartButton("Manual", Color(0xFFE65100)) { onStart(ProfMode.MANUAL) }
        }
    }
}

@Composable
private fun SmallStartButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape   = RoundedCornerShape(8.dp),
        colors  = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ── Analytics tab ─────────────────────────────────────────────────────────────

@Composable
fun ProfessorAnalyticsScreen() {
    var selectedCourse by remember { mutableStateOf<CourseAnalytics?>(null) }

    if (selectedCourse != null) {
        CourseAnalyticsDetailScreen(course = selectedCourse!!) { selectedCourse = null }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Course Analytics")
        // TODO: Replace with API/ViewModel data
        mockAnalytics.forEach { course ->
            CourseAnalyticsCard(course) { selectedCourse = course }
        }
    }
}

@Composable
private fun CourseAnalyticsCard(course: CourseAnalytics, onClick: () -> Unit) {
    BannerCard(bannerColor = Color(course.bannerColor), modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(course.code, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                AttendanceBar(fraction = course.overallPct)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(course.overallPct * 100).roundToInt()}% overall",
                    style = MaterialTheme.typography.bodySmall,
                    color = attendanceColor(course.overallPct)
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

// ── Course detail: per-student analytics ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAnalyticsDetailScreen(course: CourseAnalytics, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IITHBlue, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceGray)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TODO: Replace with API/ViewModel data
            course.students.forEach { student ->
                StudentAnalyticsRow(student)
            }
        }
    }
}

@Composable
private fun StudentAnalyticsRow(student: StudentAnalytics) {
    val pct   = student.percentage
    val color = attendanceColor(pct)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(student.rollNumber, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                AttendanceBar(fraction = pct, modifier = Modifier.fillMaxWidth(0.7f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(pct * 100).roundToInt()}%",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
                Text(
                    "${student.attended}/${student.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
