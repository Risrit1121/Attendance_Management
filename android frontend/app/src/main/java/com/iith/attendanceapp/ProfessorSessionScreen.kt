package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfessorSessionScreen() {
    val todayClasses = listOf(
        sampleClasses[0], // Swift App Dev
        sampleClasses[2], // Backend Dev
    )
    var selectedClass by remember { mutableStateOf<ScheduledClass?>(null) }

    if (selectedClass != null) {
        ClassDetailScreen(cls = selectedClass!!, onBack = { selectedClass = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            "Today's Classes",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(12.dp))
        todayClasses.forEach { cls ->
            ScheduleCard(cls = cls, onClick = { selectedClass = cls })
            Spacer(Modifier.height(16.dp))
        }
    }
}
