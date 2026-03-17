package com.iith.attendance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.iith.attendance.UserRole
import com.iith.attendance.ui.theme.GoogleBlue
import com.iith.attendance.ui.theme.ScaffoldGray

@Composable
fun RoleSelectionScreen(onContinue: (UserRole) -> Unit) {
    var selected by remember { mutableStateOf(UserRole.STUDENT) }
    val roles = listOf(
        Triple(UserRole.STUDENT, "Student", Icons.Default.School),
        Triple(UserRole.PROFESSOR, "Professor", Icons.Default.Person),
        Triple(UserRole.ADMIN, "Admin", Icons.Default.AdminPanelSettings)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScaffoldGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select Your Role", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        roles.forEach { (role, label, icon) ->
            RoleCard(label, icon, selected == role) { selected = role }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { onContinue(selected) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}

@Composable
private fun RoleCard(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (selected) BorderStroke(2.dp, GoogleBlue) else null,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = if (selected) GoogleBlue else Color.Gray)
            Text(label, color = if (selected) GoogleBlue else Color.Black)
        }
    }
}
