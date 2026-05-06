package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val courseBannerColors = listOf(GBlue, GGreen, GOrange, GPurple)

@Composable
fun ProfCourseCard(course: ProfCourse, index: Int, onClick: () -> Unit) {
    val color = courseBannerColors[index % courseBannerColors.size]
    BannerCard(
        title       = course.name,
        bannerColor = color,
        modifier    = Modifier.padding(horizontal = 16.dp).clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(course.venue, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            Text("Slot ${course.slot}", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Schedule management screen ────────────────────────────────────────────────
@Composable
fun ProfScheduleScreen(course: ProfCourse, token: String, professorId: String, onBack: () -> Unit) {
    var schedules  by remember { mutableStateOf<List<ProfScheduleItem>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var showForm   by remember { mutableStateOf(false) }

    // New schedule form state
    var newDay     by remember { mutableStateOf("Monday") }
    var newStart   by remember { mutableStateOf("09:00") }
    var newEnd     by remember { mutableStateOf("09:55") }
    var newMethod  by remember { mutableStateOf("BLE") }
    var saving     by remember { mutableStateOf(false) }

    val days    = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
    val methods = listOf("BLE","QRCode","Manual")

    fun reload() {
        loading = true
        apiGetSchedules(professorId, course.id, token) { result, err ->
            loading = false
            if (result != null) schedules = result else errorMsg = err
        }
    }

    LaunchedEffect(course.courseCode) { reload() }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Schedules", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { showForm = !showForm }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = GBlue)
            }
        }
        Text(course.name, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        if (errorMsg != null) {
            Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }

        // Add schedule form
        if (showForm) {
            Column(modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.White).padding(16.dp)) {
                Text("New Schedule", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))

                Text("Day", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                // Simple day selector as scrollable row
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    days.forEach { day ->
                        val sel = day == newDay
                        Box(modifier = Modifier.padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) GBlue else Color(0xFFF0F0F0))
                            .clickable { newDay = day }
                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(day.take(3), fontSize = 12.sp,
                                color = if (sel) Color.White else Color.Black,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start (HH:MM)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(value = newStart, onValueChange = { newStart = it },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End (HH:MM)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(value = newEnd, onValueChange = { newEnd = it },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(10.dp))

                Text("Method", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { m ->
                        val sel = m == newMethod
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (sel) GBlue else Color(0xFFF0F0F0))
                            .clickable { newMethod = m }
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(m, fontSize = 12.sp,
                                color = if (sel) Color.White else Color.Black,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        saving = true; errorMsg = null
                        apiAddSchedule(course.courseCode, newDay, newStart, newEnd, newMethod, token) { ok, err ->
                            saving = false
                            if (ok) { showForm = false; reload() }
                            else errorMsg = err
                        }
                    }, enabled = !saving, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                        if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Save", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { showForm = false }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GBlue)
            }
            schedules.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No schedules yet. Tap + to add one.", color = Color.Gray, fontSize = 13.sp)
            }
            else -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                schedules.forEach { sch ->
                    ScheduleRow(
                        sch     = sch,
                        onToggle = {
                            apiToggleSchedule(course.courseCode, sch.index, !sch.switch, token) { reload() }
                        },
                        onDelete = {
                            apiDeleteSchedule(course.courseCode, sch.index, token) { reload() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(sch: ProfScheduleItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    val methodColor = when (sch.method) { "BLE" -> GPurple; "QRCode" -> GBlue; else -> GOrange }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sch.scheduledDay, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("${sch.startTime} – ${sch.endTime}", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(methodColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(sch.method, fontSize = 11.sp, color = methodColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        // Auto-start toggle
        Switch(checked = sch.switch, onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = GBlue, checkedTrackColor = GBlue.copy(alpha = 0.4f)))
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp))
        }
    }
}
