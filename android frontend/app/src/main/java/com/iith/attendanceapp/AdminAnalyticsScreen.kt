package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api

private val rowBlue   = Color(0xFFE8F0FE)
private val rowPurple = Color(0xFFF0EBFF)

@Composable
fun AdminAnalyticsScreen(token: String) {
    var tab by remember { mutableStateOf("courses") }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)).background(Color.White)) {
            ToggleBtn("Courses",  tab == "courses",  GBlue,   Modifier.weight(1f)) { tab = "courses" }
            ToggleBtn("Students", tab == "students", GPurple, Modifier.weight(1f)) { tab = "students" }
        }
        if (tab == "courses") CourseAttendanceTable(token)
        else StudentAttendanceTable(token)
    }
}

@Composable
private fun ToggleBtn(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(44.dp), shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color else Color.White,
            contentColor   = if (selected) Color.White else Color.Gray),
        elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

// ── Courses attendance overview ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseAttendanceTable(token: String) {
    var courses    by remember { mutableStateOf<List<ProfCourse>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var search     by remember { mutableStateOf("") }
    var selected   by remember { mutableStateOf<ProfCourse?>(null) }

    LaunchedEffect(token) {
        apiGetProfCourses("", token) { result, _ ->
            loading = false
            if (result != null) courses = result
        }
    }

    if (selected != null) {
        CourseStudentAttendance(course = selected!!, token = token, onBack = { selected = null })
        return
    }

    val filtered = courses.filter { c ->
        search.isBlank() || c.name.contains(search, true) || c.id.contains(search, true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(value = search, onValueChange = { search = it },
            placeholder = { Text("Search course", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp), singleLine = true)
        Spacer(Modifier.height(8.dp))

        TableRow(listOf("S.No", "Course Name", "Code", "Slot"), listOf(0.6f, 2.5f, 1.5f, 1f), GBlue, Color.White, bold = true)

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GBlue)
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                filtered.forEachIndexed { i, c ->
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(if (i % 2 == 0) rowBlue else rowPurple)
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                        .then(Modifier.clickable { selected = c }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", modifier = Modifier.weight(0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(c.name, modifier = Modifier.weight(2.5f), fontSize = 12.sp)
                        Text(c.id,   modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = Color.Gray)
                        Text(c.slot, modifier = Modifier.weight(1f),   fontSize = 11.sp, color = Color.Gray)
                    }
                    Divider(color = Color.White, thickness = 1.dp)
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No courses found", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseStudentAttendance(course: ProfCourse, token: String, onBack: () -> Unit) {
    var records  by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }
    var search   by remember { mutableStateOf("") }

    LaunchedEffect(course.id) {
        // Use empty professorId — admin can see all
        apiGetCourseAnalytics("", course.id, token) { result, _ ->
            loading = false
            if (result != null) records = result
        }
    }

    val filtered = records.filter { r ->
        search.isBlank() || r.name.contains(search, true) || r.studentId.contains(search, true)
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text(course.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = search, onValueChange = { search = it },
            placeholder = { Text("Search student", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp), singleLine = true)
        Spacer(Modifier.height(8.dp))

        TableRow(listOf("S.No", "Name", "ID", "Attended", "%"), listOf(0.5f, 2f, 2f, 1f, 1f), GPurple, Color.White, bold = true)

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GPurple)
            }
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                filtered.forEachIndexed { i, r ->
                    val pct   = r.percentage
                    val color = when { pct >= 75 -> GGreen; pct >= 60 -> GOrange; else -> Color.Red }
                    Row(modifier = Modifier.fillMaxWidth()
                        .background(if (i % 2 == 0) rowBlue else rowPurple)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}",          modifier = Modifier.weight(0.5f), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text(r.name,              modifier = Modifier.weight(2f),   fontSize = 11.sp)
                        Text(r.studentId,         modifier = Modifier.weight(2f),   fontSize = 10.sp, color = Color.Gray)
                        Text("${r.attended}/${r.total}", modifier = Modifier.weight(1f), fontSize = 11.sp)
                        Text("${pct.toInt()}%",   modifier = Modifier.weight(1f),   fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color.White, thickness = 1.dp)
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No data yet.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ── Student attendance overview ───────────────────────────────────────────────
@Composable
private fun StudentAttendanceTable(token: String) {
    // Without a dedicated admin/students endpoint in the swagger,
    // show a placeholder directing admin to use the web portal
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Student Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Per-student analytics across all courses is available on the web portal:\nhttps://attendance-management-1-9wns.onrender.com",
                fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
            )
        }
    }
}

// ── Shared table row ──────────────────────────────────────────────────────────
@Composable
fun TableRow(cells: List<String>, weights: List<Float>, bg: Color, textColor: Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().background(bg).padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        cells.forEachIndexed { i, cell ->
            Text(cell, modifier = Modifier.weight(weights[i]), fontSize = 12.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                color = textColor, textAlign = if (i == 0) TextAlign.Center else TextAlign.Start, maxLines = 2)
        }
    }
    Divider(color = Color.White, thickness = 1.dp)
}

// ── Shared filter chip ────────────────────────────────────────────────────────
@Composable
fun FilterChipBtn(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) color else Color.LightGray.copy(alpha = 0.4f),
            contentColor   = if (selected) Color.White else Color.DarkGray),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(36.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
