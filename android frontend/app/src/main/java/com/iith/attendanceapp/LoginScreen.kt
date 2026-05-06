package com.iith.attendanceapp

import android.content.Context
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    role: UserRole,
    onLogin: (userId: String, userName: String, email: String, token: String, role: String) -> Unit
) {
    var username      by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading       by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }
    var showSlowHint  by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Show "server waking up" hint after 6 seconds
    LaunchedEffect(loading) {
        if (loading) { delay(6000); if (loading) showSlowHint = true }
        else showSlowHint = false
    }

    val roleLabel = when (role) {
        UserRole.STUDENT   -> "Student"
        UserRole.PROFESSOR -> "Professor"
    }

    fun doLogin() {
        if (username.isBlank() || password.isBlank()) { errorMsg = "Please enter username and password."; return }
        loading = true; errorMsg = null
        apiLogin(username.trim(), password.trim()) { result ->
            loading = false
            if (result.success) {
                // Trigger Android autofill save credentials dialog
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val afm = context.getSystemService(AutofillManager::class.java)
                    afm?.commit()
                }
                onLogin(result.userId, result.name, result.email, result.token, result.role)
            } else {
                errorMsg = result.error.ifBlank { "Login failed. Check credentials." }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BGGray).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        IITHLogo()
        Spacer(Modifier.height(8.dp))
        Text("Sign In as $roleLabel", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        // Username / Email field
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text(if (role == UserRole.STUDENT) "Username / Roll Number" else "Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, enabled = !loading
        )
        Spacer(Modifier.height(14.dp))

        // Password field with eye toggle
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            },
            singleLine = true, enabled = !loading
        )

        // Error message
        if (errorMsg != null) {
            Spacer(Modifier.height(10.dp))
            Text(errorMsg!!, color = Color.Red, fontSize = 12.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        // Slow server hint
        if (showSlowHint) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Server is waking up (Render free tier).\nThis may take up to 60 seconds on first login...",
                fontSize = 12.sp, color = Color(0xFFFF8C00), textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))

        // Single Sign In button for all roles
        Button(
            onClick = { doLogin() }, enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GBlue)
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Signing in...", fontWeight = FontWeight.Bold)
            } else {
                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.weight(1f))
    }
}
