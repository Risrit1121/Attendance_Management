package com.iith.attendanceapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_STUDENT = "diams_student"
private const val CHANNEL_PROF    = "diams_professor"

fun createNotificationChannels(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_STUDENT, "Session Alerts", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Notifies when an attendance session is active or starting soon" }
    )
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_PROF, "Schedule Reminders", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Reminds professor to show QR before scheduled session" }
    )
}

private fun pendingLaunch(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

fun notifyStudentSessionActive(context: Context, courseName: String) {
    val n = NotificationCompat.Builder(context, CHANNEL_STUDENT)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Attendance session started")
        .setContentText("$courseName — mark your attendance now")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingLaunch(context))
        .build()
    NotificationManagerCompat.from(context).notify(courseName.hashCode(), n)
}

fun notifyStudentSessionSoon(context: Context, courseName: String, minsLeft: Int) {
    val n = NotificationCompat.Builder(context, CHANNEL_STUDENT)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Session starting in $minsLeft min")
        .setContentText("$courseName — be ready to mark attendance")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingLaunch(context))
        .build()
    NotificationManagerCompat.from(context).notify(("soon_$courseName").hashCode(), n)
}

fun notifyProfQrSoon(context: Context, courseName: String, minsLeft: Int) {
    val n = NotificationCompat.Builder(context, CHANNEL_PROF)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("QR session in $minsLeft min")
        .setContentText("$courseName — open the app to display the QR code")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingLaunch(context))
        .build()
    NotificationManagerCompat.from(context).notify(("prof_$courseName").hashCode(), n)
}
