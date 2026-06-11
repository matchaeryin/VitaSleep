package com.vitasleep.android.ui.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.data.model.Schedule
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { viewModel.loadSchedules(userId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("日程管理", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
            Button(onClick = { showAddDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("添加") }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        else if (uiState.schedules.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) { Icon(Icons.Default.EventBusy, null, tint = OnSurfaceVariant, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text("暂无日程", color = OnSurface) }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.schedules) { schedule -> ScheduleCard(schedule, onDelete = { viewModel.deleteSchedule(schedule.id) }) }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("添加日程") }, confirmButton = { Button(onClick = { showAddDialog = false }) { Text("添加") } }, dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = "", onValueChange = {}, label = { Text("标题") }, modifier = Modifier.fillMaxWidth()) } })
    }
}

@Composable
fun ScheduleCard(schedule: Schedule, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column { Text(schedule.title, style = MaterialTheme.typography.bodyLarge, color = OnSurface); Text(schedule.startTime.takeLast(8).take(5), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Error) }
        }
    }
}
