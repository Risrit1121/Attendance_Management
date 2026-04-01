package com.iith.attendanceapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ── Shared countdown timer ────────────────────────────────────────────────────
@Composable
fun CountdownTimer(totalSeconds: Int, color: Color) {
    var timeLeft by remember { mutableStateOf(totalSeconds) }
    LaunchedEffect(Unit) {
        while (timeLeft > 0) { delay(1000); timeLeft-- }
    }
    val progress = timeLeft.toFloat() / totalSeconds

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(110.dp),
            color = color,
            trackColor = Color.LightGray,
            strokeWidth = 8.dp
        )
        Text(
            "%d:%02d".format(timeLeft / 60, timeLeft % 60),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── QR Session ────────────────────────────────────────────────────────────────
@Composable
fun ProfQRSessionScreen(cls: ScheduledClass, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(BGGray).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("← Back", color = GBlue)
        }
        Text("QR Attendance", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        CountdownTimer(totalSeconds = 120, color = GBlue)
        Spacer(Modifier.height(8.dp))
        Text("QR refreshes every 3–5 seconds", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
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

// ── BLE Session ───────────────────────────────────────────────────────────────
@Composable
fun ProfBLESessionScreen(cls: ScheduledClass, onBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ble_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier.fillMaxSize().background(BGGray).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("← Back", color = GBlue)
        }
        Text("BLE Attendance", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        CountdownTimer(totalSeconds = 120, color = GPurple)
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(GPurple.copy(alpha = alpha)),
            contentAlignment = Alignment.Center
        ) {
            Text("BLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "BLE beacon is active\nStudents will be detected automatically",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

        // Summary bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
