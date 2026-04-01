package com.iith.attendanceapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleSelectionScreen(onContinue: (UserRole) -> Unit) {
    var selected by remember { mutableStateOf(UserRole.STUDENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        // Logo + Title
        IITHLogo()
        Spacer(Modifier.height(8.dp))
        Text("Attendance Portal", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("IIT Hyderabad", fontSize = 14.sp, color = Color.Gray)

        Spacer(Modifier.height(32.dp))

        Text("I am a...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Spacer(Modifier.height(14.dp))

        RoleButton(
            title = "Student",
            icon = Icons.Default.School,
            selected = selected == UserRole.STUDENT,
            onClick = { selected = UserRole.STUDENT }
        )
        Spacer(Modifier.height(14.dp))
        RoleButton(
            title = "Professor",
            icon = Icons.Default.Person,
            selected = selected == UserRole.PROFESSOR,
            onClick = { selected = UserRole.PROFESSOR }
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onContinue(selected) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GBlue)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun RoleButton(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) GBlue else Color.White
    val fg = if (selected) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = if (selected) Color.White else GBlue, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
}
