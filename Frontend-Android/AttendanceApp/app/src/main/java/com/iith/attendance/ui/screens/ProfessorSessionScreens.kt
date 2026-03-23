package com.iith.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.iith.attendance.data.models.ProfMode
import com.iith.attendance.data.models.StudentRecord
import com.iith.attendance.ui.theme.*
import kotlinx.coroutines.delay

// ── Shared session countdown timer ────────────────────────────────────────────
// 2-minute session (matches iOS ProfQRSessionView / ProfBLESessionView)

@Composable
fun rememberSessionCountdown(totalSeconds: Int = 120): Int {
    var remaining by remember { mutableIntStateOf(totalSeconds) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000L)
            remaining--
        }
    }
    return remaining
}

@Composable
fun TimerDisplay(remaining: Int) {
    val mins = remaining / 60
    val secs = remaining % 60
    val fraction = remaining / 120f
    val color = when {
        fraction > 0.5f -> AttendanceGreen
        fraction > 0.25f -> AttendanceOrange
        else -> AttendanceRed
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%d:%02d".format(mins, secs),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text("Session time remaining", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// QR Session
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfQRSessionScreen(onStop: () -> Unit) {
    val remaining = rememberSessionCountdown()
    var qrToken   by remember { mutableStateOf("QR-TOKEN-${System.currentTimeMillis()}") }

    // Refresh QR every 4 seconds (matches iOS ProfQRSessionView)
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L)
            // TODO: GET dynamic QR token from backend
            qrToken = "QR-TOKEN-${System.currentTimeMillis()}"
        }
    }

    if (remaining == 0) {
        SessionEndedScreen(onDone = onStop)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Session") },
                navigationIcon = { IconButton(onClick = onStop) { Icon(Icons.Default.Close, null) } },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            TimerDisplay(remaining)

            // QR code placeholder (real app: use ZXing to generate bitmap)
            Card(
                modifier  = Modifier.size(220.dp),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: Replace with actual QR bitmap generated from qrToken via ZXing
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(100.dp), tint = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = qrToken.takeLast(12),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, null, tint = AttendanceGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("QR refreshes every 4 seconds", style = MaterialTheme.typography.bodySmall, color = AttendanceGreen)
                }
            }

            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("End Session")
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// BLE Session
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfBLESessionScreen(onStop: () -> Unit) {
    val remaining = rememberSessionCountdown()

    if (remaining == 0) {
        SessionEndedScreen(onDone = onStop)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Session") },
                navigationIcon = { IconButton(onClick = onStop) { Icon(Icons.Default.Close, null) } },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            TimerDisplay(remaining)

            // Beacon pulsing indicator
            Surface(
                shape    = CircleShape,
                color    = IITHBlue,
                modifier = Modifier.size(140.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Bluetooth,
                        null,
                        tint     = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = IITHBlueLight
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BluetoothSearching, null, tint = IITHBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("BLE beacon is active", style = MaterialTheme.typography.bodySmall, color = IITHBlueDark, fontWeight = FontWeight.Medium)
                }
            }

            Text(
                "UUID: 550e8400-e29b-41d4-a716-446655440000",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("End Session")
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Manual Session
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfManualSessionScreen(initialStudents: List<StudentRecord>, onStop: () -> Unit) {
    var students by remember { mutableStateOf(initialStudents) }
    var submitted by remember { mutableStateOf(false) }

    if (submitted) {
        SessionEndedScreen(message = "Attendance submitted!", onDone = onStop)
        return
    }

    val presentCount = students.count { it.isPresent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Attendance") },
                navigationIcon = { IconButton(onClick = onStop) { Icon(Icons.Default.Close, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IITHBlue, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$presentCount / ${students.size} present",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { students = students.map { it.copy(isPresent = true) } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Mark All") }
                        Button(
                            onClick  = {
                                // TODO: POST manual attendance list to backend
                                submitted = true
                            },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = IITHBlue)
                        ) { Text("Submit") }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(SurfaceGray).padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students, key = { it.id }) { student ->
                StudentManualRow(
                    student   = student,
                    onToggle  = { present ->
                        students = students.map { if (it.id == student.id) it.copy(isPresent = present) else it }
                    }
                )
            }
        }
    }
}

@Composable
private fun StudentManualRow(student: StudentRecord, onToggle: (Boolean) -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (student.isPresent) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape    = CircleShape,
                color    = if (student.isPresent) AttendanceGreen else Color(0xFFEEEEEE),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        student.name.first().uppercaseChar().toString(),
                        fontWeight = FontWeight.Bold,
                        color      = if (student.isPresent) Color.White else Color.Gray
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(student.rollNumber, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Switch(
                checked         = student.isPresent,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = Color.White,
                    checkedTrackColor  = AttendanceGreen
                )
            )
        }
    }
}

// ── Shared session ended screen ───────────────────────────────────────────────

@Composable
fun SessionEndedScreen(message: String = "Session has ended.", onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(SurfaceGray).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = AttendanceGreen, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(20.dp))
        Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick  = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp)
        ) { Text("Done") }
    }
}
