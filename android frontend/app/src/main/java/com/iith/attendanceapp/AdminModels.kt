package com.iith.attendanceapp

// ── Slots ─────────────────────────────────────────────────────────────────────
val allSlots = listOf("A","B","C","D","E","F","G","P","Q","R","S")

// ── Course ────────────────────────────────────────────────────────────────────
data class AdminCourse(
    val id: Int,
    val name: String,
    val code: String,
    val professors: List<String>,
    val slot: String,
    val room: String,
    val archived: Boolean = false,
    val students: MutableList<AdminStudent> = mutableListOf()
)

// ── Student (inside a course) ─────────────────────────────────────────────────
data class AdminStudent(
    val id: Int,
    val name: String,
    val roll: String
)

// ── Professor (analytics) ─────────────────────────────────────────────────────
enum class ProfType(val label: String) {
    ASSISTANT("Asst. Professor"),
    ASSOCIATE("Assoc. Professor"),
    PROFESSOR("Professor")
}

data class AdminProfessor(
    val name: String,
    val code: String,          // 4-digit unique code
    val type: ProfType,
    val classesTaken: Int,
    val avgAttendance: Double  // percentage
)

// ── Student (analytics) ───────────────────────────────────────────────────────
data class AdminStudentAnalytics(
    val name: String,
    val roll: String,
    val avgAttendance: Double
)

// ── Sample data ───────────────────────────────────────────────────────────────
val sampleAdminCourses = mutableListOf(
    AdminCourse(1, "Swift App Dev",    "CS5.401", listOf("Dr. Smith"),          "A", "Room 304",
        students = mutableListOf(
            AdminStudent(1, "Sreehith Sanam", "CS22BTECH11050"),
            AdminStudent(2, "Arjun Reddy",    "CS22BTECH11023"),
            AdminStudent(3, "Priya Sharma",   "CS22BTECH11031"),
        )
    ),
    AdminCourse(2, "Machine Learning", "CS5.301", listOf("Dr. Rao"),            "B", "Room 101",
        students = mutableListOf(
            AdminStudent(1, "Sreehith Sanam", "CS22BTECH11050"),
            AdminStudent(2, "Kiran Kumar",    "CS22BTECH11044"),
        )
    ),
    AdminCourse(3, "Backend Dev",      "CS5.501", listOf("Dr. Patel","Dr. Roy"),"C", "Room 202",
        students = mutableListOf(
            AdminStudent(1, "Ananya Singh",  "CS22BTECH11012"),
            AdminStudent(2, "Rahul Verma",   "CS22BTECH11067"),
        )
    ),
)

val sampleAdminProfessors = listOf(
    AdminProfessor("Dr. Smith", "1042", ProfType.ASSISTANT, 28, 87.5),
    AdminProfessor("Dr. Rao",   "2031", ProfType.ASSOCIATE, 35, 74.2),
    AdminProfessor("Dr. Patel", "3018", ProfType.PROFESSOR, 42, 91.0),
    AdminProfessor("Dr. Roy",   "4055", ProfType.ASSISTANT, 20, 65.3),
)

val sampleAdminStudents = listOf(
    AdminStudentAnalytics("Sreehith Sanam", "CS22BTECH11050", 88.0),
    AdminStudentAnalytics("Arjun Reddy",    "CS22BTECH11023", 72.5),
    AdminStudentAnalytics("Priya Sharma",   "CS22BTECH11031", 95.0),
    AdminStudentAnalytics("Kiran Kumar",    "CS22BTECH11044", 60.0),
    AdminStudentAnalytics("Ananya Singh",   "CS22BTECH11012", 55.5),
    AdminStudentAnalytics("Rahul Verma",    "CS22BTECH11067", 78.3),
)
