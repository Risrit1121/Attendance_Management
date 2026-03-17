package com.iith.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iith.attendance.UserRole
import com.iith.attendance.UserSession
import com.iith.attendance.ui.theme.GoogleBlue
import com.iith.attendance.ui.theme.ScaffoldGray

@Composable
fun LoginScreen(role: UserRole, onLogin: (UserSession) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScaffoldGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign In as ${role.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEDEFF1),
                unfocusedContainerColor = Color(0xFFEDEFF1),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
        )
        Spacer(Modifier.height(12.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFEDEFF1),
                unfocusedContainerColor = Color(0xFFEDEFF1),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onLogin(
                    UserSession(
                        role = role,
                        name = if (role == UserRole.STUDENT) "Ananya Reddy" else "Dr. Ravi Kumar",
                        email = email.ifBlank { "user@iith.ac.in" },
                        id = if (role == UserRole.STUDENT) "CS22BTECH11010" else "FAC102"
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Sign In", color = Color.White)
        }
    }
}
