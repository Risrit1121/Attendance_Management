package com.iith.attendanceapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

// ── Data models ───────────────────────────────────────────────────────────────

enum class AttendanceMode { QR, BLE }

data class ScheduledTime(
    val id: Int,
    val minutesAfterStart: Int,   // 0–60
    val mode: AttendanceMode,
    val archived: Boolean = false
)

// ── Shared countdown timer ────────────────────────────────────────────────────
@Composable
fun CountdownTimer(totalSeconds: Int, color: Color) {
    var timeLeft by remember { mutableStateOf(totalSeconds) }
    LaunchedEffect(Unit) {
        while (timeLeft > 0) { delay(1000); timeLeft-- }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
        CircularProgressIndicator(
            progress = { timeLeft.toFloat() / totalSeconds },
            modifier = Modifier.size(110.dp),
            color = color,
            trackColor = Color.LightGray,
            strokeWidth = 8.dp
        )
        Text("%d:%02d".format(timeLeft / 60, timeLeft % 60), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Circular time picker (0–60 min knob) ─────────────────────────────────────
@Composable
fun CircularTimePicker(minutes: Int, onMinutesChange: (Int) -> Unit, color: Color) {
    val sizeDp = 200.dp
    val sweepAngle = (minutes / 60f) * 360f

    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(sizeDp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pos = change.position
                        val angle = Math.toDegrees(
                            atan2((pos.y - center.y).toDouble(), (pos.x - center.x).toDouble())
                        ).toFloat() + 90f
                        val normalized = ((angle % 360) + 360) % 360
                        val newMinutes = ((normalized / 360f) * 60).roundToInt().coerceIn(0, 60)
                        onMinutesChange(newMinutes)
                    }
                }
        ) {
            val stroke = Stroke(width = 20f, cap = StrokeCap.Round)
            // Track
            drawArc(
                color = Color.LightGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            // Progress
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = stroke
                )
            }
            // Knob dot
            val rad = Math.toRadians((sweepAngle - 90).toDouble())
            val radius = size.minDimension / 2f
            val knobX = center.x + radius * cos(rad).toFloat()
            val knobY = center.y + radius * sin(rad).toFloat()
            drawCircle(color = color, radius = 14f, center = Offset(knobX, knobY))
            drawCircle(color = Color.White, radius = 7f, center = Offset(knobX, knobY))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$minutes", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
            Text("min", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

// ── Add / Edit scheduled time sheet ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScheduleSheet(
    existing: ScheduledTime?,
    mode: AttendanceMode,
    onSave: (ScheduledTime) -> Unit,
    onDismiss: () -> Unit,
    nextId: Int
) {
    var minutes by remember { mutableStateOf(existing?.minutesAfterStart ?: 15) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (existing == null) "Add Scheduled Time" else "Edit Scheduled Time",
                fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Drag the knob to set minutes after class starts",
                fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            CircularTimePicker(
                minutes = minutes,
                onMinutesChange = { minutes = it },
                color = if (mode == AttendanceMode.QR) GBlue else GPurple
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "$minutes minutes after class starts",
                fontSize = 14.sp, color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        ScheduledTime(
                            id = existing?.id ?: nextId,
                            minutesAfterStart = minutes,
                            mode = mode,
                            archived = existing?.archived ?: false
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mode == AttendanceMode.QR) GBlue else GPurple
                )
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }
    }
}

// ── Scheduled time card ───────────────────────────────────────────────────────
@Composable
fun ScheduledTimeCard(
    item: ScheduledTime,
    mode: AttendanceMode,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchiveToggle: () -> Unit,
    onGenerateQR: (() -> Unit)? = null   // only for QR mode
) {
    val color = if (mode == AttendanceMode.QR) GBlue else GPurple
    val alpha = if (item.archived) 0.45f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${item.minutesAfterStart}m",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = color.copy(alpha = alpha)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${item.minutesAfterStart} min after class starts",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = alpha)
            )
            if (item.archived) {
                Text("Archived", fontSize = 12.sp, color = Color.Gray)
            }
        }

        // Generate QR button (QR mode only, not archived)
        if (mode == AttendanceMode.QR && !item.archived && onGenerateQR != null) {
            TextButton(onClick = onGenerateQR) {
                Text("Generate QR", fontSize = 12.sp, color = GBlue, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(4.dp))
        }

        // Action icons
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onArchiveToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                if (item.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                null, tint = Color.Gray, modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

// ── QR Scheduled Times Screen ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfQRSessionScreen(cls: ScheduledClass, onBack: () -> Unit) {
    var schedules by remember {
        mutableStateOf(
            listOf(
                ScheduledTime(1, 15, AttendanceMode.QR),
                ScheduledTime(2, 45, AttendanceMode.QR),
            )
        )
    }
    var showAddEdit by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScheduledTime?>(null) }
    var showQRFor by remember { mutableStateOf<ScheduledTime?>(null) }

    if (showQRFor != null) {
        QRDisplayScreen(
            cls = cls,
            scheduledTime = showQRFor!!,
            onBack = { showQRFor = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text(
            "QR Scheduled Times",
            fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            cls.name,
            fontSize = 13.sp, color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (schedules.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No scheduled times yet. Tap + to add one.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            schedules.forEach { item ->
                ScheduledTimeCard(
                    item = item,
                    mode = AttendanceMode.QR,
                    onEdit = { editingItem = item; showAddEdit = true },
                    onDelete = { schedules = schedules.filter { it.id != item.id } },
                    onArchiveToggle = {
                        schedules = schedules.map {
                            if (it.id == item.id) it.copy(archived = !it.archived) else it
                        }
                    },
                    onGenerateQR = { showQRFor = item }
                )
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = { editingItem = null; showAddEdit = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Scheduled Time", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddEdit) {
        AddEditScheduleSheet(
            existing = editingItem,
            mode = AttendanceMode.QR,
            onSave = { newItem ->
                schedules = if (editingItem == null) {
                    schedules + newItem
                } else {
                    schedules.map { if (it.id == newItem.id) newItem else it }
                }
            },
            onDismiss = { showAddEdit = false; editingItem = null },
            nextId = (schedules.maxOfOrNull { it.id } ?: 0) + 1
        )
    }
}

// ── QR Display Screen (shown when professor taps Generate QR) ─────────────────
@Composable
fun QRDisplayScreen(cls: ScheduledClass, scheduledTime: ScheduledTime, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(BGGray).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("← Back", color = GBlue)
        }
        Text("QR Attendance", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(cls.name, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Text(
            "Scheduled at ${scheduledTime.minutesAfterStart} min after class starts",
            fontSize = 13.sp, color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        CountdownTimer(totalSeconds = 120, color = GBlue)
        Spacer(Modifier.height(8.dp))
        Text("QR refreshes every 3–5 seconds", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        // TODO: replace with actual dynamic QR from backend
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("[ QR Code ]", color = Color.Gray, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Display this to students", fontSize = 14.sp, color = Color.Gray)
    }
}

// ── BLE Scheduled Times Screen ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfBLESessionScreen(cls: ScheduledClass, onBack: () -> Unit) {
    var schedules by remember {
        mutableStateOf(
            listOf(
                ScheduledTime(1, 15, AttendanceMode.BLE),
                ScheduledTime(2, 45, AttendanceMode.BLE),
            )
        )
    }
    var showAddEdit by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ScheduledTime?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text(
            "BLE Scheduled Times",
            fontWeight = FontWeight.Bold, fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            cls.name,
            fontSize = 13.sp, color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (schedules.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No scheduled times yet. Tap + to add one.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            schedules.forEach { item ->
                ScheduledTimeCard(
                    item = item,
                    mode = AttendanceMode.BLE,
                    onEdit = { editingItem = item; showAddEdit = true },
                    onDelete = { schedules = schedules.filter { it.id != item.id } },
                    onArchiveToggle = {
                        schedules = schedules.map {
                            if (it.id == item.id) it.copy(archived = !it.archived) else it
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = { editingItem = null; showAddEdit = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GPurple)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Scheduled Time", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddEdit) {
        AddEditScheduleSheet(
            existing = editingItem,
            mode = AttendanceMode.BLE,
            onSave = { newItem ->
                schedules = if (editingItem == null) {
                    schedules + newItem
                } else {
                    schedules.map { if (it.id == newItem.id) newItem else it }
                }
            },
            onDismiss = { showAddEdit = false; editingItem = null },
            nextId = (schedules.maxOfOrNull { it.id } ?: 0) + 1
        )
    }
}

// ── Manual Session ────────────────────────────────────────────────────────────
data class StudentEntry(val name: String, val roll: String, var present: Boolean = false)

@Composable
fun ProfManualSessionScreen(cls: ScheduledClass, onBack: () -> Unit) {
    val students = remember {
        mutableStateListOf(
            StudentEntry("Sreehith Sanam", "CS22BTECH11050"),
            StudentEntry("Arjun Reddy",    "CS22BTECH11023"),
            StudentEntry("Priya Sharma",   "CS22BTECH11031"),
            StudentEntry("Kiran Kumar",    "CS22BTECH11044"),
            StudentEntry("Ananya Singh",   "CS22BTECH11012"),
            StudentEntry("Rahul Verma",    "CS22BTECH11067"),
        )
    }
    val presentCount = students.count { it.present }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text("Manual Attendance", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$presentCount / ${students.size} present", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { students.indices.forEach { students[it] = students[it].copy(present = true) } }) {
                Text("Mark All", color = GBlue)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(students) { index, student ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(student.roll, fontSize = 12.sp, color = Color.Gray)
                    }
                    Checkbox(
                        checked = student.present,
                        onCheckedChange = { students[index] = student.copy(present = it) },
                        colors = CheckboxDefaults.colors(checkedColor = GGreen)
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
            Button(
                onClick = { /* TODO: POST to backend */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) { Text("Submit Attendance", fontWeight = FontWeight.Bold) }
        }
    }
}
