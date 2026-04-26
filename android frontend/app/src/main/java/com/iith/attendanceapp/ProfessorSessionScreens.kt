package com.iith.attendanceapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay

// ── QR Session Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfQRSessionScreen(course: ProfCourse, token: String, onBack: () -> Unit) {
    var session    by remember { mutableStateOf<ProfSession?>(null) }
    var qrContent  by remember { mutableStateOf<String?>(null) }
    var attendance by remember { mutableStateOf<List<LiveAttendanceEntry>>(emptyList()) }
    var loading    by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var timeLeft   by remember { mutableStateOf(0) }

    LaunchedEffect(course.id) {
        loading = true
        apiStartSession(course.id, "QRCode", token) { sess, err ->
            loading = false
            if (sess != null) { session = sess; timeLeft = sess.durationSeconds }
            else errorMsg = err
        }
    }

    LaunchedEffect(session?.sessionId) {
        if (session == null) return@LaunchedEffect
        while (true) {
            apiQrGenerate(course.venue, token) { result ->
                if (result.hash.isNotBlank()) qrContent = "${course.venue}|${result.hash}"
            }
            delay(5000)
        }
    }

    LaunchedEffect(session?.sessionId) {
        val sid = session?.sessionId ?: return@LaunchedEffect
        while (true) {
            apiGetSessionSummary(sid, token) { list, _ -> if (list != null) attendance = list }
            delay(3000)
        }
    }

    LaunchedEffect(session) {
        if (session == null) return@LaunchedEffect
        while (timeLeft > 0) { delay(1000); timeLeft-- }
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = {
            session?.sessionId?.let { sid -> apiEndSession(sid, token) { _, _ -> } }
            onBack()
        }, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← End Session", color = Color.Red)
        }
        Text("QR Attendance", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text(course.name, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GBlue)
            }
        } else if (errorMsg != null) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Error: $errorMsg", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                    CircularProgressIndicator(progress = { timeLeft.toFloat() / session!!.durationSeconds },
                        modifier = Modifier.size(110.dp), color = GBlue, trackColor = Color.LightGray, strokeWidth = 8.dp)
                    Text("%d:%02d".format(timeLeft / 60, timeLeft % 60), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("QR refreshes every 5 seconds", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp)).background(Color.White),
                    contentAlignment = Alignment.Center) {
                    if (qrContent != null) Text(qrContent!!, fontSize = 10.sp, color = Color.Black,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
                    else Text("Generating QR...", color = Color.Gray)
                }
                Spacer(Modifier.height(8.dp))
                Text("Display this to students", fontSize = 13.sp, color = Color.Gray)

                Spacer(Modifier.height(24.dp))
                Text("Live Attendance (${attendance.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                attendance.forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(GGreen))
                        Spacer(Modifier.width(8.dp))
                        Text(entry.name.ifBlank { entry.studentId }, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(entry.verifiedVia, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ── BLE Session Screen ────────────────────────────────────────────────────────
@Composable
fun ProfBLESessionScreen(course: ProfCourse, token: String, onBack: () -> Unit) {
    var session    by remember { mutableStateOf<ProfSession?>(null) }
    var attendance by remember { mutableStateOf<List<LiveAttendanceEntry>>(emptyList()) }
    var loading    by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var timeLeft   by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "ble")
    val alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")

    LaunchedEffect(course.id) {
        loading = true
        apiStartSession(course.id, "BLE", token) { sess, err ->
            loading = false
            if (sess != null) { session = sess; timeLeft = sess.durationSeconds }
            else errorMsg = err
        }
    }

    LaunchedEffect(session?.sessionId) {
        val sid = session?.sessionId ?: return@LaunchedEffect
        while (true) {
            apiGetSessionSummary(sid, token) { list, _ -> if (list != null) attendance = list }
            delay(3000)
        }
    }

    LaunchedEffect(session) {
        if (session == null) return@LaunchedEffect
        while (timeLeft > 0) { delay(1000); timeLeft-- }
    }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = {
            session?.sessionId?.let { sid -> apiEndSession(sid, token) { _, _ -> } }
            onBack()
        }, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← End Session", color = Color.Red)
        }
        Text("BLE Attendance", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text(course.name, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GPurple)
            }
        } else if (errorMsg != null) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Error: $errorMsg", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    CircularProgressIndicator(progress = { timeLeft.toFloat() / session!!.durationSeconds },
                        modifier = Modifier.size(120.dp), color = GPurple, trackColor = Color.LightGray, strokeWidth = 8.dp)
                    Text("%d:%02d".format(timeLeft / 60, timeLeft % 60), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(GPurple.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center) {
                    Text("BLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("BLE beacon is active\nStudents will be detected automatically",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

                Spacer(Modifier.height(24.dp))
                Text("Live Attendance (${attendance.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                attendance.forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(GGreen))
                        Spacer(Modifier.width(8.dp))
                        Text(entry.name.ifBlank { entry.studentId }, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(entry.verifiedVia, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ── Manual Session Screen ─────────────────────────────────────────────────────
data class StudentEntry(val id: String, val name: String, val roll: String, var present: Boolean = false)

@Composable
fun ProfManualSessionScreen(course: ProfCourse, token: String, onBack: () -> Unit) {
    var session     by remember { mutableStateOf<ProfSession?>(null) }
    var students    by remember { mutableStateOf<List<StudentEntry>>(emptyList()) }
    var loading     by remember { mutableStateOf(true) }
    var submitting  by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var successMsg  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(course.id) {
        apiStartSession(course.id, "Manual", token) { sess, err ->
            if (sess != null) session = sess else errorMsg = err
        }
        apiGetCourseStudents(token, course.id) { list, err ->
            loading = false
            if (list != null) students = list.map { StudentEntry(it.id, it.name, it.email) }
            else errorMsg = err
        }
    }

    val presentCount = students.count { it.present }

    Column(modifier = Modifier.fillMaxSize().background(BGGray)) {
        TextButton(onClick = {
            session?.sessionId?.let { sid -> apiEndSession(sid, token) { _, _ -> } }
            onBack()
        }, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← End Session", color = Color.Red)
        }
        Text("Manual Attendance", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text(course.name, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GBlue)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("$presentCount / ${students.size} present", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { students = students.map { it.copy(present = true) } }) {
                    Text("Mark All", color = GBlue)
                }
            }

            if (errorMsg != null) Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            if (successMsg != null) Text(successMsg!!, color = GGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(students) { index, student ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(student.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(student.roll, fontSize = 12.sp, color = Color.Gray)
                        }
                        Checkbox(checked = student.present,
                            onCheckedChange = { students = students.toMutableList().also { list -> list[index] = student.copy(present = it) } },
                            colors = CheckboxDefaults.colors(checkedColor = GGreen))
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            Box(modifier = Modifier.background(Color.White).padding(16.dp)) {
                Button(
                    onClick = {
                        val sid = session?.sessionId
                        if (sid == null) { errorMsg = "Session not started yet."; return@Button }
                        val presentIds = students.filter { it.present }.map { it.id }
                        if (presentIds.isEmpty()) { errorMsg = "No students marked present."; return@Button }
                        submitting = true; errorMsg = null
                        apiManualAttendanceBulk(token, sid, presentIds) { ok, err ->
                            submitting = false
                            if (ok) successMsg = "Attendance submitted for ${presentIds.size} students."
                            else errorMsg = err
                        }
                    },
                    enabled  = !submitting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GBlue)
                ) {
                    if (submitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Submit Attendance", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
