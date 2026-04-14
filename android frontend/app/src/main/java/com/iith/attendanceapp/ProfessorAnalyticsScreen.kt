package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class StudentRecord(val name: String, val roll: String, val attended: Int, val total: Int) {
    val percentage: Double get() = if (total > 0) attended.toDouble() / total * 100 else 0.0
}

data class CourseAnalytics(val cls: ScheduledClass, val students: List<StudentRecord>) {
    val overallPercentage: Double get() =
        if (students.isEmpty()) 0.0 else students.sumOf { it.percentage } / students.size
}

val sampleCourseAnalytics = listOf(
    CourseAnalytics(
        cls = sampleClasses[0],
        students = listOf(
            StudentRecord("Sreehith Sanam", "CS22BTECH11050", 9,  10),
            StudentRecord("Arjun Reddy",    "CS22BTECH11023", 7,  10),
            StudentRecord("Priya Sharma",   "CS22BTECH11031", 10, 10),
            StudentRecord("Kiran Kumar",    "CS22BTECH11044", 6,  10),
        )
    ),
    CourseAnalytics(
        cls = sampleClasses[1],
        students = listOf(
            StudentRecord("Sreehith Sanam", "CS22BTECH11050", 18, 20),
            StudentRecord("Arjun Reddy",    "CS22BTECH11023", 14, 20),
            StudentRecord("Priya Sharma",   "CS22BTECH11031", 20, 20),
        )
    ),
)

@Composable
fun ProfessorAnalyticsScreen() {
    var selectedCourse by remember { mutableStateOf<CourseAnalytics?>(null) }

    if (selectedCourse != null) {
        CourseAnalyticsDetailScreen(course = selectedCourse!!, onBack = { selectedCourse = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        sampleCourseAnalytics.forEach { course ->
            CourseAnalyticsCard(course = course, onClick = { selectedCourse = course })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun CourseAnalyticsCard(course: CourseAnalytics, onClick: () -> Unit) {
    val pct = course.overallPercentage
    val color = when {
        pct >= 75 -> GGreen
        pct >= 60 -> GOrange
        else      -> Color.Red
    }
    BannerCard(
        title = course.cls.name,
        bannerColor = course.cls.bannerColor,
        modifier = Modifier.padding(horizontal = 16.dp).clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.cls.code, fontSize = 12.sp, color = Color.Gray)
                Text("${course.students.size} students", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                SolidProgressBar(
                    progress = (pct / 100).toFloat(),
                    color = color,
                    modifier = Modifier.width(160.dp).height(6.dp)
                )
            }
            Text("${pct.toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun CourseAnalyticsDetailScreen(course: CourseAnalytics, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Text("← Back", color = GBlue)
        }
        Text(course.cls.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            course.students.forEach { student ->
                val pct = student.percentage
                val color = when {
                    pct >= 75 -> GGreen
                    pct >= 60 -> GOrange
                    else      -> Color.Red
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(student.roll, fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        SolidProgressBar(
                            progress = (pct / 100).toFloat(),
                            color = color,
                            modifier = Modifier.width(140.dp).height(5.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${pct.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                        Text("${student.attended}/${student.total}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
