package com.vitasleep.android.ui.screens.health

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(userId) { viewModel.loadLatestMetrics(userId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("身体状态", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        else {
            uiState.battery?.let { BatteryCard(level = it) }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.heartRate?.let { HealthMetricCard(title = "心率", value = "${it} bpm", icon = Icons.Default.Favorite, color = Error, modifier = Modifier.weight(1f)) }
                uiState.bloodPressure?.let { HealthMetricCard(title = "血压", value = it, icon = Icons.Default.Speed, color = Warning, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun BatteryCard(level: Int) {
    val color = when { level >= 80 -> BatteryHigh; level >= 40 -> BatteryMedium; else -> BatteryLow }
    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.BatteryChargingFull, null, tint = color, modifier = Modifier.size(32.dp)); Spacer(Modifier.width(12.dp)); Column { Text("身体电量", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant); Text("$level%", style = MaterialTheme.typography.headlineMedium, color = color) } }
                Text(if (level >= 80) "充沛" else if (level >= 40) "正常" else "偏低", color = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { level / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = SurfaceVariant)
        }
    }
}

@Composable
fun HealthMetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = OnSurface)
        }
    }
}
