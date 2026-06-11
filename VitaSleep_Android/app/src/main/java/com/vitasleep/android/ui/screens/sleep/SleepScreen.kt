package com.vitasleep.android.ui.screens.sleep

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun SleepScreen(viewModel: SleepViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(userId) { viewModel.loadSleepData(userId) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("睡眠分析", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.error != null && uiState.totalSleepHours == null) {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("暂无睡眠数据", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadSleepData(userId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("重试") }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("昨晚睡眠", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    Text(
                        uiState.totalSleepHours ?: "--",
                        style = MaterialTheme.typography.headlineLarge,
                        color = OnSurface
                    )
                    uiState.qualityScore?.let {
                        Text(
                            "质量评分: ${it.toInt()}",
                            color = if (it >= 80) Success else Warning
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Surface), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("睡眠阶段分布", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SleepStageItem("深睡", uiState.deepPct, DeepSleepColor, Modifier.weight(1f))
                        SleepStageItem("浅睡", uiState.lightPct, LightSleepColor, Modifier.weight(1f))
                        SleepStageItem("REM", uiState.remPct, RemSleepColor, Modifier.weight(1f))
                        SleepStageItem("清醒", uiState.awakePct, AwakeColor, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SleepStageItem(
    name: String,
    pct: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text("${pct.toInt()}%", style = MaterialTheme.typography.headlineSmall, color = color)
        Text(name, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
    }
}
