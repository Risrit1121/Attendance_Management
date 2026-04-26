package com.iith.attendanceapp

// Slot options for course creation
val allSlots = listOf("A","B","C","D","E","F","G","P","Q","R","S")

// Professor type for analytics filter
enum class ProfType(val label: String) {
    ASSISTANT("Asst. Professor"),
    ASSOCIATE("Assoc. Professor"),
    PROFESSOR("Professor")
}
