package com.iith.attendance.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.iith.attendance.R
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.MainScaffold
import kotlinx.coroutines.delay

@Composable
fun ProfQRSessionScreen(session: UserSession?, onLogout: () -> Unit, paddingValues: PaddingValues) {
    var seconds by remember { mutableIntStateOf(120) }
    var token by remember { mutableStateOf("QR#001") }

    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
    }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(4000)
            token = "QR#${(100..999).random()}" // TODO: replace with API call
        }
    }

    MainScaffold("QR Session", session, onLogout) { modifier ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CountdownRing(seconds)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Live QR Token: $token")
                    Text("QR refreshes every few seconds")
                    Image(
                        painter = painterResource(id = R.drawable.iith_logo),
                        contentDescription = "QR placeholder",
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownRing(seconds: Int) {
    Box(
        modifier = Modifier
            .background(Color.White, CircleShape)
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("${seconds}s", style = MaterialTheme.typography.headlineMedium)
    }
}
