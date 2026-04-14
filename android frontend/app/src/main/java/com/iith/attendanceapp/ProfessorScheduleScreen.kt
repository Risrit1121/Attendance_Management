package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ScheduledClass(
    val name: String,
    val code: String,
    val room: String,
    val timing: String,
    val days: List<String>,
    val bannerColor: Color
)

val sampleClasses = listOf(
    ScheduledClass("Swift App Dev",    "CS5.401", "Room 304", "10:00 – 11:00 AM", listOf("Mon","Wed","Fri"), GBlue),
    ScheduledClass("Machine Learning", "CS5.301", "Room 101", "11:30 – 12:30 PM", listOf("Tue","Thu"),       GGreen),
    ScheduledClass("Backend Dev",      "CS5.501", "Room 202", "02:00 – 03:00 PM", listOf("Mon","Thu"),       GOrange),
)

@Composable
fun ScheduleCard(cls: ScheduledClass, onClick: () -> Unit) {
    BannerCard(
        title = cls.name,
        bannerColor = cls.bannerColor,
        modifier = Modifier.padding(horizontal = 16.dp).clickable(onClick = onClick)
    ) {
        Text(cls.code, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(cls.timing, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Text(cls.room, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            cls.days.forEach { day ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(cls.bannerColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(day, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = cls.bannerColor)
                }
            }
        }
    }
}
