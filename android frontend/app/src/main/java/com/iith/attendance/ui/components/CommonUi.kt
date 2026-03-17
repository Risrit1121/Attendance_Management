package com.iith.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iith.attendance.UserSession
import com.iith.attendance.ui.theme.ScaffoldGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    title: String,
    session: UserSession?,
    onLogout: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val openSheet = remember { mutableStateOf(false) }
    Scaffold(
        containerColor = ScaffoldGray,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = { openSheet.value = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding -> content(Modifier.padding(padding)) }

    if (openSheet.value) {
        ModalBottomSheet(onDismissRequest = { openSheet.value = false }) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Profile", style = MaterialTheme.typography.titleLarge)
                Text("Name: ${session?.name ?: "Unknown"}")
                Text("Email: ${session?.email ?: "Unknown"}")
                Text("ID: ${session?.id ?: "Unknown"}")
                Button(
                    onClick = {
                        openSheet.value = false
                        onLogout()
                    },
                    modifier = modifier.fillMaxWidth()
                ) { Text("Log Out") }
            }
        }
    }
}

@Composable
fun BannerCard(title: String, bannerColor: Color, modifier: Modifier = Modifier, subtitle: @Composable () -> Unit) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerColor)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.padding(14.dp)) {
                subtitle()
            }
        }
    }
}
