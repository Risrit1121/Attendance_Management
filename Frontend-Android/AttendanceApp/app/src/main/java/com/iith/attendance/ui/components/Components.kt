package com.iith.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iith.attendance.ui.theme.*

// ── Banner card (colored top strip + white body) ──────────────────────────────

@Composable
fun BannerCard(
    bannerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(bannerColor)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

// ── Attendance progress bar ───────────────────────────────────────────────────

@Composable
fun AttendanceBar(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        fraction >= 0.75f -> AttendanceGreen
        fraction >= 0.60f -> AttendanceOrange
        else              -> AttendanceRed
    }
    val track = color.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

fun attendanceColor(pct: Float) = when {
    pct >= 0.75f -> AttendanceGreen
    pct >= 0.60f -> AttendanceOrange
    else         -> AttendanceRed
}

// ── Step indicator dots ───────────────────────────────────────────────────────

@Composable
fun StepDots(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (index == current) IITHBlue else Color.LightGray)
            )
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

// ── Day chip ──────────────────────────────────────────────────────────────────

@Composable
fun DayChip(day: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = IITHBlueLight
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            color = IITHBlueDark,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ── Mode chip (QR / BLE / MANUAL) ─────────────────────────────────────────────

@Composable
fun ModeChip(mode: String, modifier: Modifier = Modifier) {
    val bg = when (mode.uppercase()) {
        "QR"     -> Color(0xFFE8F5E9)
        "BLE"    -> Color(0xFFE3F2FD)
        "MANUAL" -> Color(0xFFFFF3E0)
        else     -> Color(0xFFEEEEEE)
    }
    val fg = when (mode.uppercase()) {
        "QR"     -> Color(0xFF2E7D32)
        "BLE"    -> Color(0xFF1565C0)
        "MANUAL" -> Color(0xFFE65100)
        else     -> Color.DarkGray
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg, modifier = modifier) {
        Text(
            text = mode,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
