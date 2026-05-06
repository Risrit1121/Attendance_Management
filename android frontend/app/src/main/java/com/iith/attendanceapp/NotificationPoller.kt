package com.iith.attendanceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Calendar

private const val POLL_INTERVAL_MS = 30_000L
private const val SOON_WINDOW_MIN  = 3

// ── Student notification poller ───────────────────────────────────────────────
// Polls active sessions every 30s.
// - If a new session appears → "session started" notification
// - If a scheduled session starts within 3 min → "session soon" notification
@Composable
fun StudentNotificationPoller(userId: String, token: String) {
    val context = LocalContext.current
    val seenSessionIds = remember { mutableSetOf<String>() }
    val notifiedSoon   = remember { mutableSetOf<String>() }

    RequestNotificationPermission()

    LaunchedEffect(userId) {
        while (true) {
            // Single call — handle both "new session" and "starting soon" notifications
            apiGetStudentSessions(userId, token) { sessions, _ ->
                val nowMs = System.currentTimeMillis()
                sessions?.forEach { s ->
                    // New session notification
                    if (s.sessionId !in seenSessionIds) {
                        seenSessionIds.add(s.sessionId)
                        notifyStudentSessionActive(context, s.courseName)
                    }
                    // "Starting soon" — session started less than 3 min ago
                    val ageMin = (nowMs - s.startedAt) / 60000
                    val soonKey = "soon_${s.sessionId}"
                    if (ageMin < SOON_WINDOW_MIN && soonKey !in notifiedSoon) {
                        notifiedSoon.add(soonKey)
                        notifyStudentSessionSoon(context, s.courseName, (SOON_WINDOW_MIN - ageMin).toInt().coerceAtLeast(1))
                    }
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}

// ── Professor notification poller ─────────────────────────────────────────────
// Polls professor courses every 30s.
// If any schedule with method=QRCode starts within 3 minutes → notify.
@Composable
fun ProfessorNotificationPoller(professorId: String, token: String) {
    val context      = LocalContext.current
    val notifiedKeys = remember { mutableSetOf<String>() }

    RequestNotificationPermission()

    LaunchedEffect(professorId) {
        while (true) {
            apiGetProfCourses(professorId, token) { courses, _ ->
                courses?.forEach { course ->
                    course.schedules.forEach { sched ->
                        if (sched.method != "QRCode") return@forEach
                        val minsUntil = minutesUntilSchedule(sched)
                        if (minsUntil in 1..SOON_WINDOW_MIN) {
                            val key = "${course.id}_${sched.scheduledDay}_${sched.startTime}"
                            if (key !in notifiedKeys) {
                                notifiedKeys.add(key)
                                notifyProfQrSoon(context, course.name, minsUntil)
                            }
                        }
                    }
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }
}

// Returns how many minutes until the next occurrence of this schedule
private fun minutesUntilSchedule(sched: ProfSchedule): Int {
    val dayMap = mapOf("Sunday" to 1, "Monday" to 2, "Tuesday" to 3, "Wednesday" to 4,
        "Thursday" to 5, "Friday" to 6, "Saturday" to 7)
    val now = Calendar.getInstance()
    val todayDow = now.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    val schedDow = dayMap[sched.scheduledDay] ?: return Int.MAX_VALUE

    val parts = sched.startTime.split(":").map { it.toIntOrNull() ?: 0 }
    if (parts.size < 2) return Int.MAX_VALUE
    val schedHour = parts[0]; val schedMin = parts[1]

    val nowMin   = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val schedTotalMin = schedHour * 60 + schedMin

    return if (todayDow == schedDow) {
        val diff = schedTotalMin - nowMin
        if (diff in 0..SOON_WINDOW_MIN) diff else Int.MAX_VALUE
    } else Int.MAX_VALUE
}

// ── Runtime notification permission (Android 13+) ────────────────────────────
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}
