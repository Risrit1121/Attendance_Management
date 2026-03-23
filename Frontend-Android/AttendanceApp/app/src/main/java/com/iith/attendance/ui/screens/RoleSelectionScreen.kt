package com.iith.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iith.attendance.data.models.UserRole
import com.iith.attendance.ui.theme.*

@Composable
fun RoleSelectionScreen(onContinue: (UserRole) -> Unit) {
    var selected by remember { mutableStateOf(UserRole.STUDENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(72.dp))

        // Logo + Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = IITHBlue,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Attendance Portal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "IIT Hyderabad",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Role buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "I am a...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            RoleButton(
                title    = "Student",
                icon     = Icons.Default.School,
                selected = selected == UserRole.STUDENT
            ) { selected = UserRole.STUDENT }

            RoleButton(
                title    = "Professor",
                icon     = Icons.Default.Person,
                selected = selected == UserRole.PROFESSOR
            ) { selected = UserRole.PROFESSOR }

            RoleButton(
                title    = "Admin",
                icon     = Icons.Default.Shield,
                selected = selected == UserRole.ADMIN
            ) { selected = UserRole.ADMIN }
        }

        // Continue button
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick    = { onContinue(selected) },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape      = RoundedCornerShape(12.dp),
                colors     = ButtonDefaults.buttonColors(containerColor = IITHBlue)
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RoleButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg     = if (selected) IITHBlue else Color.White
    val fg     = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val iconFg = if (selected) Color.White else IITHBlue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = Color(0xFFDDDDDD),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconFg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = fg, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}
