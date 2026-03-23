package com.iith.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iith.attendance.data.models.Subject
import com.iith.attendance.ui.components.*
import com.iith.attendance.ui.theme.*
import kotlin.math.roundToInt

// ── Hardcoded mock data (replace with API calls) ──────────────────────────────

private val mockSubjects = listOf(
    Subject("1", "Signals & Systems",         "EE2030", 18, 22, 0xFF1A73E8),
    Subject("2", "Digital Electronics",       "EC2010", 14, 22, 0xFF34A853),
    Subject("3", "Engineering Mathematics",   "MA2100", 10, 22, 0xFFE91E63),
    Subject("4", "Data Structures & Algo",    "CS2020", 20, 22, 0xFFFB8C00),
    Subject("5", "Physics Lab",               "PH2001", 8,  10, 0xFF9C27B0),
)

// ── Dashboard screen ──────────────────────────────────────────────────────────

@Composable
fun StudentDashboardScreen() {
    val totalAttended = mockSubjects.sumOf { it.attended }
    val totalClasses  = mockSubjects.sumOf { it.total }
    val overallPct    = if (totalClasses == 0) 0f else totalAttended.toFloat() / totalClasses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overall card
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = IITHBlue),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Overall Attendance", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${(overallPct * 100).roundToInt()}%",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$totalAttended / $totalClasses classes",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(overallPct)
                            .fillMaxHeight()
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        SectionHeader("Subjects")

        // TODO: Replace mockSubjects with LiveData from ViewModel / API
        mockSubjects.forEach { subject ->
            SubjectCard(subject)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SubjectCard(subject: Subject) {
    val pct   = subject.percentage
    val color = attendanceColor(pct)

    BannerCard(bannerColor = Color(subject.bannerColor)) {
        Text(subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(subject.code, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(10.dp))
        AttendanceBar(fraction = pct)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${subject.attended} / ${subject.total} classes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                "${(pct * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
