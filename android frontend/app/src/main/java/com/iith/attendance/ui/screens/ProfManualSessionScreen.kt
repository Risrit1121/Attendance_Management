package com.iith.attendance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.MainScaffold

data class StudentMark(val name: String, val roll: String, var present: Boolean)

@Composable
fun ProfManualSessionScreen(session: UserSession?, onLogout: () -> Unit, paddingValues: PaddingValues) {
    val students = remember {
        mutableStateListOf(
            StudentMark("Ananya Reddy", "CS22BTECH11010", false),
            StudentMark("Rahul N", "CS22BTECH11022", true),
            StudentMark("Ishita M", "CS22BTECH11031", false)
        )
    } // TODO: replace with API call

    MainScaffold("Manual Attendance", session, onLogout) { modifier ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Present Count: ${students.count { it.present }} / ${students.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { students.indices.forEach { i -> students[i] = students[i].copy(present = true) } }) {
                    Text("Mark All")
                }
                Button(onClick = { /* TODO: POST to backend */ }) { Text("Submit") }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(students) { index, student ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(student.name, style = MaterialTheme.typography.titleMedium)
                                Text(student.roll)
                            }
                            Button(onClick = {
                                students[index] = student.copy(present = !student.present)
                            }) {
                                Text(if (student.present) "Present" else "Absent")
                            }
                        }
                    }
                }
            }
        }
    }
}
