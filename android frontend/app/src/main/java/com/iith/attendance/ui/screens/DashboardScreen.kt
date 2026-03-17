package com.iith.attendance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.BannerCard
import com.iith.attendance.ui.components.MainScaffold

data class SubjectAttendance(val name: String, val code: String, val percent: Int)

@Composable
fun DashboardScreen(session: UserSession?, onLogout: () -> Unit, paddingValues: PaddingValues) {
    val subjects = listOf(
        SubjectAttendance("Data Structures", "CS201", 82),
        SubjectAttendance("Operating Systems", "CS301", 66),
        SubjectAttendance("Computer Networks", "CS305", 58)
    ) // TODO: replace with API call

    MainScaffold(title = "Student Dashboard", session = session, onLogout = onLogout) { modifier ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Overall Attendance", style = MaterialTheme.typography.titleMedium)
                    Text("74%", style = MaterialTheme.typography.displaySmall)
                }
            }
            subjects.forEach { subject -> SubjectCard(subject) }
        }
    }
}

@Composable
fun SubjectCard(subject: SubjectAttendance) {
    val progress = subject.percent / 100f
    val color = when {
        subject.percent >= 75 -> Color(0xFF34A853)
        subject.percent >= 60 -> Color(0xFFFB8C00)
        else -> Color(0xFFEA4335)
    }
    BannerCard(title = subject.name, bannerColor = color) {
        Text(subject.code)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = color)
        Spacer(Modifier.height(6.dp))
        Text("Attendance: ${subject.percent}%", color = color)
    }
}
