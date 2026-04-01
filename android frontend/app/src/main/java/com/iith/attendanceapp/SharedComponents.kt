package com.iith.attendanceapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── IITH Logo ────────────────────────────────────────────────────────────────
@Composable
fun IITHLogo(size: Int = 80) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.iithlogo),
            contentDescription = "IITH Logo",
            modifier = Modifier.size(size.dp)
        )
    }
}

// ── Banner Card (colored top strip + white body) ──────────────────────────────
@Composable
fun BannerCard(
    title: String,
    bannerColor: Color,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(bannerColor),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(14.dp),
            content = body
        )
    }
}

// ── Profile Sheet ─────────────────────────────────────────────────────────────
@Composable
fun ProfileSheet(onLogout: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 28.dp, end = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            tint = GBlue,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Sreehith Sanam", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("sreehith@iith.ac.in", fontSize = 14.sp, color = Color.Gray)
        Text("CS22BTECH11050", fontSize = 14.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFF0000), contentColor = Color.Red)
        ) {
            Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClose) {
            Text("Close", color = Color.Gray)
        }
    }
}

// ── Solid continuous progress bar (no gap, no dot) ───────────────────────────
@Composable
fun SolidProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color.LightGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

// ── Attendance progress bar row ───────────────────────────────────────────────
@Composable
fun AttendanceProgressRow(attended: Int, total: Int, percentage: Double) {
    val color = when {
        percentage >= 75 -> GGreen
        percentage >= 60 -> GOrange
        else             -> Color.Red
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SolidProgressBar(
            progress = (percentage / 100).toFloat(),
            color = color,
            modifier = Modifier.weight(1f).height(6.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${percentage.toInt()}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text("$attended/$total classes", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
