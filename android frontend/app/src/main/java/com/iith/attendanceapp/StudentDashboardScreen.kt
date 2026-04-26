package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val bannerColors = listOf(GBlue, GGreen, GOrange, GPurple)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(userId: String, token: String) {
    var courses   by remember { mutableStateOf<List<CourseAnalytic>>(emptyList()) }
    var loading   by remember { mutableStateOf(true) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }

    fun load() {
        apiGetStudentAnalytics(userId, token) { result, err ->
            loading = false; refreshing = false
            if (result != null) courses = result else errorMsg = err
        }
    }

    LaunchedEffect(userId) { load() }

    val overallAttended = courses.sumOf { it.attended }
    val overallTotal    = courses.sumOf { it.total }
    val overall         = if (overallTotal > 0) overallAttended.toDouble() / overallTotal * 100 else 0.0
    val overallColor    = if (overall >= 75) GGreen else Color.Red

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh    = { refreshing = true; load() }
    ) {
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
                if (loading) {
                    CircularProgressIndicator(color = GBlue, modifier = Modifier.size(48.dp))
                } else {
                    Text("${overall.toInt()}%", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = overallColor)
                    Spacer(Modifier.height(8.dp))
                    SolidProgressBar((overall / 100).toFloat(), overallColor, Modifier.fillMaxWidth().height(6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            if (errorMsg != null) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            courses.forEachIndexed { i, course ->
                BannerCard(
                    title       = course.name,
                    bannerColor = bannerColors[i % bannerColors.size],
                    modifier    = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row {
                        Text(course.code, fontSize = 12.sp, color = Color.Gray)
                        if (course.room.isNotBlank()) {
                            Text("  •  ${course.room}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AttendanceProgressRow(course.attended, course.total, course.percentage)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
