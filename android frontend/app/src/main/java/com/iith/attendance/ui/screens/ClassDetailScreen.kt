package com.iith.attendance.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.MainScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classCode: String,
    session: UserSession?,
    onLogout: () -> Unit,
    navController: NavController,
    paddingValues: PaddingValues
) {
    val showSheet = remember { mutableStateOf(false) }
    val schedules = remember {
        mutableStateListOf(
            Triple("Morning Attendance", "09:55", "QR"),
            Triple("Mid Lecture", "10:25", "BLE"),
            Triple("Closing", "10:55", "Manual")
        )
    } // TODO: replace with API call

    MainScaffold("Class Detail", session, onLogout) { modifier ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Operating Systems ($classCode)", style = MaterialTheme.typography.titleLarge)
                    Text("Room LH-201 • Mon Wed Fri • 10:00 AM")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { navController.navigate("prof_qr_session") }, modifier = Modifier.weight(1f)) { Text("QR") }
                Button(onClick = { navController.navigate("prof_ble_session") }, modifier = Modifier.weight(1f)) { Text("BLE") }
                Button(onClick = { navController.navigate("prof_manual_session") }, modifier = Modifier.weight(1f)) { Text("Manual") }
            }
            Text("Scheduled Attendances", style = MaterialTheme.typography.titleMedium)
            Box {
                LazyColumn(
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(schedules) { (label, time, mode) ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(label)
                                    Text("$time • $mode")
                                }
                                Switch(checked = true, onCheckedChange = {})
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showSheet.value = true },
                    modifier = Modifier.padding(top = 170.dp, start = 280.dp)
                ) { androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Add") }
            }
        }
    }

    if (showSheet.value) {
        AddScheduleBottomSheet(onDismiss = { showSheet.value = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add Attendance Schedule", style = MaterialTheme.typography.titleLarge)
            Text("Use this sheet to configure alarm-like scheduled attendance windows.")
            Text("// TODO: replace with API call")
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Save (Mock)") }
        }
    }
}
