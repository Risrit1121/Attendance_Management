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

// ── Shared mock data (TODO: replace with ViewModel / API) ────────────────────
// Defined here and reused by ProfessorCoursesScreen & ProfessorAnalyticsScreens

internal val mockClasses = listOf(
    ScheduledClass("c1", "Signals & Systems",       "EE2030", "Room 201", "09:00 – 10:00 AM", listOf("Mon","Wed","Fri"), 0xFF1A73E8),
    ScheduledClass("c2", "Digital Electronics",     "EC2010", "Room 305", "10:30 – 11:30 AM", listOf("Tue","Thu"),       0xFF34A853),
    ScheduledClass("c3", "VLSI Design",             "EC4050", "Lab 102",  "02:00 – 04:00 PM", listOf("Wed"),            0xFFE91E63),
    ScheduledClass("c4", "Embedded Systems",        "EE3040", "Room 408", "11:00 – 12:00 PM", listOf("Mon","Thu"),      0xFFFB8C00),
)

private val mockStudents = listOf(
    StudentRecord("1", "Arjun Sharma",    "ES22BTECH11001"),
    StudentRecord("2", "Priya Nair",      "ES22BTECH11002"),
    StudentRecord("3", "Rahul Verma",     "ES22BTECH11003"),
    StudentRecord("4", "Sneha Reddy",     "ES22BTECH11004"),
    StudentRecord("5", "Kiran Kumar",     "ES22BTECH11005"),
    StudentRecord("6", "Anika Patel",     "ES22BTECH11006"),
)

// ── Schedule screen ───────────────────────────────────────────────────────────

@Composable
fun ProfessorScheduleScreen() {
    var selectedClass by remember { mutableStateOf<ScheduledClass?>(null) }
    var sessionMode   by remember { mutableStateOf<ProfMode?>(null) }

    when {
        sessionMode == ProfMode.QR     -> ProfQRSessionScreen   { sessionMode = null }
        sessionMode == ProfMode.BLE    -> ProfBLESessionScreen  { sessionMode = null }
        sessionMode == ProfMode.MANUAL -> ProfManualSessionScreen(mockStudents) { sessionMode = null }
        selectedClass != null          -> ClassDetailScreen(
            cls       = selectedClass!!,
            onBack    = { selectedClass = null },
            onStartSession = { mode -> sessionMode = mode }
        )
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceGray)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("My Classes")
            // TODO: Replace with API/ViewModel data
            mockClasses.forEach { cls ->
                ScheduleCard(cls) { selectedClass = cls }
            }
        }
    }
}

@Composable
private fun ScheduleCard(cls: ScheduledClass, onClick: () -> Unit) {
    BannerCard(bannerColor = Color(cls.bannerColor), modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cls.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(cls.code, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Room, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(" ${cls.room}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(" ${cls.time}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    cls.days.forEach { DayChip(it) }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

// ── Class detail ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    cls: ScheduledClass,
    onBack: () -> Unit,
    onStartSession: (ProfMode) -> Unit
) {
    var schedules by remember { mutableStateOf(listOf(
        AttendanceSchedule("a1", "Morning Check", "09:00 AM", ProfMode.QR,  true),
        AttendanceSchedule("a2", "Mid-class",     "09:30 AM", ProfMode.BLE, false),
    )) }
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cls.name) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IITHBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceGray)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start session buttons
            SectionHeader("Start Session")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SessionStartButton("QR", Icons.Default.QrCode, Color(0xFF34A853), Modifier.weight(1f)) {
                    onStartSession(ProfMode.QR)
                }
                SessionStartButton("BLE", Icons.Default.Bluetooth, IITHBlue, Modifier.weight(1f)) {
                    onStartSession(ProfMode.BLE)
                }
                SessionStartButton("Manual", Icons.Default.EditNote, Color(0xFFE65100), Modifier.weight(1f)) {
                    onStartSession(ProfMode.MANUAL)
                }
            }

            // Scheduled attendances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Scheduled Attendances")
                IconButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, null, tint = IITHBlue)
                }
            }

            schedules.forEach { sched ->
                ScheduleRow(
                    schedule  = sched,
                    onToggle  = { enabled ->
                        schedules = schedules.map { if (it.id == sched.id) it.copy(enabled = enabled) else it }
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddScheduleSheet(
            onAdd     = { label, time, mode ->
                val newId = System.currentTimeMillis().toString()
                schedules = schedules + AttendanceSchedule(newId, label, time, mode, true)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun SessionStartButton(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape  = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ScheduleRow(schedule: AttendanceSchedule, onToggle: (Boolean) -> Unit) {
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
            Icon(Icons.Default.Alarm, null, tint = IITHBlue, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(schedule.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(schedule.time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    ModeChip(schedule.mode.name)
                }
            }
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
        }
    }
}

// ── Add schedule bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleSheet(
    onAdd: (String, String, ProfMode) -> Unit,
    onDismiss: () -> Unit
) {
    var label    by remember { mutableStateOf("") }
    var time     by remember { mutableStateOf("") }
    var mode     by remember { mutableStateOf(ProfMode.QR) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add Scheduled Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value         = label,
                onValueChange = { label = it },
                label         = { Text("Label") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )
            OutlinedTextField(
                value         = time,
                onValueChange = { time = it },
                label         = { Text("Time (e.g. 09:30 AM)") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfMode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick  = { mode = m },
                        label    = { Text(m.name) }
                    )
                }
            }
            Button(
                onClick  = { if (label.isNotBlank() && time.isNotBlank()) onAdd(label, time, mode) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp)
            ) { Text("Add") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
