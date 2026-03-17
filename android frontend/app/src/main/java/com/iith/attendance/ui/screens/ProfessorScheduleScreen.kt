package com.iith.attendance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.BannerCard
import com.iith.attendance.ui.components.MainScaffold

data class ScheduledClass(
    val name: String,
    val code: String,
    val room: String,
    val timing: String,
    val days: List<String>
)

@Composable
fun ProfessorScheduleScreen(
    session: UserSession?,
    onLogout: () -> Unit,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val list = listOf(
        ScheduledClass("Operating Systems", "CS301", "LH-201", "10:00-11:00", listOf("Mon", "Wed", "Fri")),
        ScheduledClass("Data Mining", "CS402", "LH-105", "14:00-15:00", listOf("Tue", "Thu"))
    ) // TODO: replace with API call

    MainScaffold("Professor Schedule", session, onLogout) { modifier ->
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list) { item ->
                BannerCard(
                    title = item.name,
                    bannerColor = Color(0xFF1A73E8),
                    modifier = Modifier.clickable { navController.navigate("class_detail/${item.code}") }
                ) {
                        Text("${item.code} • ${item.room}")
                        Text(item.timing)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.days.forEach { day -> AssistChip(onClick = {}, label = { Text(day) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessorCoursesPlaceholder(session: UserSession?, onLogout: () -> Unit, paddingValues: PaddingValues) {
    MainScaffold("Courses", session, onLogout) { modifier ->
        androidx.compose.foundation.layout.Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Courses tab ready for API-backed data.", style = MaterialTheme.typography.titleMedium)
        }
    }
}
