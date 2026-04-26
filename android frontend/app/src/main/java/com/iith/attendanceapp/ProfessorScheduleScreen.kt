package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Banner colors cycle for courses
val courseBannerColors = listOf(GBlue, GGreen, GOrange, GPurple)

@Composable
fun ProfCourseCard(course: ProfCourse, index: Int, onClick: () -> Unit) {
    val color = courseBannerColors[index % courseBannerColors.size]
    BannerCard(
        title       = course.name,
        bannerColor = color,
        modifier    = Modifier.padding(horizontal = 16.dp).clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(course.venue, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.weight(1f))
            Text("Slot ${course.slot}", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
        if (course.schedules.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            course.schedules.take(2).forEach { sched ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${sched.scheduledDay}  ${sched.startTime}–${sched.endTime}  (${sched.method})",
                        fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
