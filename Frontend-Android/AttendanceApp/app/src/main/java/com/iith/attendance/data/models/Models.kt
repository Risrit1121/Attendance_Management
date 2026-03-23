package com.iith.attendance.data.models

// ── Auth / Users ──────────────────────────────────────────────────────────────

enum class UserRole { STUDENT, PROFESSOR, ADMIN }

data class User(
    val name: String  = "",
    val email: String = "",
    val id: String    = "",
    val role: UserRole = UserRole.STUDENT
)

// ── Student-side models ───────────────────────────────────────────────────────

data class Subject(
    val id: String,
    val name: String,
    val code: String,
    val attended: Int,
    val total: Int,
    val bannerColor: Long          // ARGB Long for Compose Color
) {
    val percentage: Float get() = if (total == 0) 0f else attended.toFloat() / total
}

enum class AttendanceMode { QR, BLE }

data class ActiveSession(
    val id: String,
    val courseName: String,
    val courseCode: String,
    val room: String,
    val professorName: String,
    val mode: AttendanceMode,
    val endsAt: String             // e.g. "02:30 PM"
)

// ── Professor-side models ─────────────────────────────────────────────────────

data class ScheduledClass(
    val id: String,
    val name: String,
    val code: String,
    val room: String,
    val time: String,
    val days: List<String>,
    val bannerColor: Long
)

enum class ProfMode { QR, BLE, MANUAL }

data class AttendanceSchedule(
    val id: String,
    val label: String,
    val time: String,
    val mode: ProfMode,
    val enabled: Boolean = true
)

data class StudentRecord(
    val id: String,
    val name: String,
    val rollNumber: String,
    val isPresent: Boolean = false
)

data class CourseAnalytics(
    val id: String,
    val name: String,
    val code: String,
    val bannerColor: Long,
    val overallPct: Float,
    val students: List<StudentAnalytics>
)

data class StudentAnalytics(
    val name: String,
    val rollNumber: String,
    val attended: Int,
    val total: Int
) {
    val percentage: Float get() = if (total == 0) 0f else attended.toFloat() / total
}

// ── BLE scan result ───────────────────────────────────────────────────────────

data class BeaconResult(
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val uuid: String
)

// ── Network ───────────────────────────────────────────────────────────────────

data class MinorResponse(val status: String, val minor: Int)
data class ValidateResponse(val valid: Boolean, val error: String? = null)
