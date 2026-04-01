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

// Tracks which screen the professor is on inside the Courses tab
private sealed class ProfCourseScreen {
    object List : ProfCourseScreen()
    data class ModeSelect(val cls: ScheduledClass) : ProfCourseScreen()
    data class QR(val cls: ScheduledClass) : ProfCourseScreen()
    data class BLE(val cls: ScheduledClass) : ProfCourseScreen()
    data class Manual(val cls: ScheduledClass) : ProfCourseScreen()
}

@Composable
fun ProfessorSessionScreen() {
    var screen by remember { mutableStateOf<ProfCourseScreen>(ProfCourseScreen.List) }

    when (val s = screen) {
        is ProfCourseScreen.List       -> CourseListScreen    { screen = ProfCourseScreen.ModeSelect(it) }
        is ProfCourseScreen.ModeSelect -> ModeSelectScreen(s.cls,
            onQR     = { screen = ProfCourseScreen.QR(s.cls) },
            onBLE    = { screen = ProfCourseScreen.BLE(s.cls) },
            onManual = { screen = ProfCourseScreen.Manual(s.cls) },
            onBack   = { screen = ProfCourseScreen.List }
        )
        is ProfCourseScreen.QR     -> ProfQRSessionScreen(s.cls)     { screen = ProfCourseScreen.ModeSelect(s.cls) }
        is ProfCourseScreen.BLE    -> ProfBLESessionScreen(s.cls)    { screen = ProfCourseScreen.ModeSelect(s.cls) }
        is ProfCourseScreen.Manual -> ProfManualSessionScreen(s.cls) { screen = ProfCourseScreen.ModeSelect(s.cls) }
    }
}

// ── Course list ───────────────────────────────────────────────────────────────
@Composable
private fun CourseListScreen(onSelect: (ScheduledClass) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        sampleClasses.forEach { cls ->
            ScheduleCard(cls = cls, onClick = { onSelect(cls) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Mode selection (QR / BLE / Manual) ───────────────────────────────────────
@Composable
private fun ModeSelectScreen(
    cls: ScheduledClass,
    onQR: () -> Unit,
    onBLE: () -> Unit,
    onManual: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
            Text("← Back", color = GBlue)
        }

        BannerCard(cls.name, cls.bannerColor, Modifier.padding(horizontal = 16.dp)) {
            Text("${cls.code}  •  ${cls.room}", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(cls.timing, fontSize = 12.sp, color = Color.Gray)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Choose Attendance Mode",
            fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(14.dp))

        ModeCard(
            title = "QR Code",
            description = "Students scan a dynamic QR code displayed by you",
            color = GBlue,
            onClick = onQR
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            title = "BLE",
            description = "Bluetooth beacon detects students automatically in the classroom",
            color = GPurple,
            onClick = onBLE
        )
        Spacer(Modifier.height(12.dp))
        ModeCard(
            title = "Manual",
            description = "Mark attendance manually from the student list",
            color = GOrange,
            onClick = onManual
        )
    }
}

@Composable
private fun ModeCard(title: String, description: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(title.take(3), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
        Text("›", fontSize = 22.sp, color = Color.LightGray)
    }
}
