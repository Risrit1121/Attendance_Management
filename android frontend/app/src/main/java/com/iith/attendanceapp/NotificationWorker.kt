package com.iith.attendanceapp

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val creds = CredentialStore.load(context) ?: return Result.success()
        createNotificationChannels(context)

        when (creds.role) {
            "student" -> checkStudentNotifications(creds)
            "prof"    -> checkProfessorNotifications(creds)
        }
        return Result.success()
    }

    private suspend fun checkStudentNotifications(creds: SavedCredentials) {
        val sessions = fetchStudentSessions(creds.userId, creds.token) ?: return
        val prefs    = context.getSharedPreferences("diams_notif", Context.MODE_PRIVATE)
        val nowMs    = System.currentTimeMillis()

        sessions.forEach { s ->
            val seenKey = "seen_${s.sessionId}"
            if (!prefs.getBoolean(seenKey, false)) {
                prefs.edit().putBoolean(seenKey, true).apply()
                notifyStudentSessionActive(context, s.courseName)
            }
            // "Starting soon" — session started less than 3 min ago
            val ageMin = (nowMs - s.startedAt) / 60000
            val soonKey = "soon_${s.sessionId}"
            if (ageMin < 3 && !prefs.getBoolean(soonKey, false)) {
                prefs.edit().putBoolean(soonKey, true).apply()
                notifyStudentSessionSoon(context, s.courseName, (3 - ageMin).toInt().coerceAtLeast(1))
            }
        }
    }

    private suspend fun checkProfessorNotifications(creds: SavedCredentials) {
        val courses  = fetchProfCourses(creds.userId, creds.token) ?: return
        val prefs    = context.getSharedPreferences("diams_notif", Context.MODE_PRIVATE)

        courses.forEach { course ->
            course.schedules.forEach { sched ->
                if (sched.method != "QRCode") return@forEach
                val minsUntil = minutesUntilSchedule(sched)
                if (minsUntil in 1..3) {
                    val key = "prof_${course.id}_${sched.scheduledDay}_${sched.startTime}"
                    if (!prefs.getBoolean(key, false)) {
                        prefs.edit().putBoolean(key, true).apply()
                        notifyProfQrSoon(context, course.name, minsUntil)
                    }
                }
            }
        }
    }

    // Suspend wrappers around the callback-based API functions
    private suspend fun fetchStudentSessions(userId: String, token: String) =
        suspendCancellableCoroutine<List<ActiveSessionItem>?> { cont ->
            apiGetStudentSessions(userId, token) { result, _ -> cont.resume(result) }
        }

    private suspend fun fetchProfCourses(professorId: String, token: String) =
        suspendCancellableCoroutine<List<ProfCourse>?> { cont ->
            apiGetProfCourses(professorId, token) { result, _ -> cont.resume(result) }
        }

    private fun minutesUntilSchedule(sched: ProfSchedule): Int {
        val dayMap = mapOf("Sunday" to 1, "Monday" to 2, "Tuesday" to 3,
            "Wednesday" to 4, "Thursday" to 5, "Friday" to 6, "Saturday" to 7)
        val now      = Calendar.getInstance()
        val todayDow = now.get(Calendar.DAY_OF_WEEK)
        val schedDow = dayMap[sched.scheduledDay] ?: return Int.MAX_VALUE
        if (todayDow != schedDow) return Int.MAX_VALUE
        val parts = sched.startTime.split(":").map { it.toIntOrNull() ?: 0 }
        if (parts.size < 2) return Int.MAX_VALUE
        val diff = (parts[0] * 60 + parts[1]) - (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
        return if (diff in 0..3) diff else Int.MAX_VALUE
    }

    companion object {
        private const val WORK_NAME = "diams_notification_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
