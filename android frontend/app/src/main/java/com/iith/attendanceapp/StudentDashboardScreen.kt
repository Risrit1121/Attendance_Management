package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Subject(
    val name: String,
    val code: String,
    val bannerColor: Color,
    val attended: Int,
    val total: Int
) {
    val percentage: Double get() = if (total > 0) attended.toDouble() / total * 100 else 0.0
}

@Composable
fun StudentDashboardScreen() {
    val subjects = remember {
        listOf(
            Subject("Swift App Dev",    "CS5.401", GBlue,   10, 10),
            Subject("Machine Learning", "CS5.301", GGreen,  18, 20),
            Subject("Backend Dev",      "CS5.501", GOrange, 15, 20),
        )
    }
    val overallAttended = subjects.sumOf { it.attended }
    val overallTotal    = subjects.sumOf { it.total }
    val overall         = if (overallTotal > 0) overallAttended.toDouble() / overallTotal * 100 else 0.0
    val overallColor    = if (overall >= 75) GGreen else Color.Red

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        // Overall card
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .shadow(4.dp, RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Overall Attendance", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text("${overall.toInt()}%", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = overallColor)
            Spacer(Modifier.height(8.dp))
            SolidProgressBar(
                progress = (overall / 100).toFloat(),
                color = overallColor,
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        subjects.forEach { subject ->
            SubjectCard(subject)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SubjectCard(subject: Subject) {
    BannerCard(
        title = subject.name,
        bannerColor = subject.bannerColor,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(subject.code, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        AttendanceProgressRow(subject.attended, subject.total, subject.percentage)
    }
}
