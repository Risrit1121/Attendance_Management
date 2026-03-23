package com.iith.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iith.attendance.data.models.UserRole
import com.iith.attendance.ui.theme.*

@Composable
fun LoginScreen(role: UserRole, onLoginSuccess: () -> Unit) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }
    var loading  by remember { mutableStateOf(false) }

    val title = when (role) {
        UserRole.STUDENT   -> "Sign In as Student"
        UserRole.PROFESSOR -> "Sign In as Professor"
        UserRole.ADMIN     -> "Sign In as Admin"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IITHBlue,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("IIT Hyderabad", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value          = email,
            onValueChange  = { email = it },
            label          = { Text("Email") },
            leadingIcon    = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine     = true,
            modifier       = Modifier.fillMaxWidth(),
            shape          = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value            = password,
            onValueChange    = { password = it },
            label            = { Text("Password") },
            leadingIcon      = { Icon(Icons.Default.Lock, null) },
            trailingIcon     = {
                IconButton(onClick = { showPwd = !showPwd }) {
                    Icon(
                        if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (showPwd) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            singleLine       = true,
            modifier         = Modifier.fillMaxWidth(),
            shape            = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                // TODO: POST credentials to backend, receive JWT
                loading = true
                onLoginSuccess()
            },
            enabled   = email.isNotBlank() && password.isNotBlank() && !loading,
            modifier  = Modifier.fillMaxWidth().height(52.dp),
            shape     = RoundedCornerShape(12.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = IITHBlue)
        ) {
            if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            else Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
