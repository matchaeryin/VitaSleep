package com.vitasleep.android.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(Unit) {
        viewModel.loadLatestMetrics(userId)
        viewModel.startSseListening(userId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSseListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "身体状态",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isSseConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Success, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "实时同步",
                            fontSize = 11.sp,
                            color = Success
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OnSurfaceVariant, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "离线",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            uiState.error != null -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("无法连接到服务器", color = OnSurface)
                        Text(uiState.error ?: "", color = OnSurfaceVariant, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadLatestMetrics(userId) }) {
                            Text("重试")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        uiState.battery?.let { BatteryCard(level = it) }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.heartRate?.let {
                                HealthMetricCard(
                                    title = "心率",
                                    value = "${it}bpm",
                                    icon = Icons.Default.Favorite,
                                    color = Error,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            uiState.bloodPressure?.let { bp ->
                                val parts = bp.split("/")
                                HealthMetricCard(
                                    title = "血压",
                                    value = bp,
                                    subtext = if (parts.size == 2) "收缩/舒张" else null,
                                    icon = Icons.Default.Speed,
                                    color = Warning,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    item {
                        uiState.cardioIndex?.let { cardio ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("心血管指数", color = OnSurfaceVariant, fontSize = 13.sp)
                                            Text(cardio, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                        }
                                    }
                                    Text(
                                        uiState.cardioRisk ?: "",
                                        color = when {
                                            uiState.cardioRisk?.contains("低") == true -> Success
                                            uiState.cardioRisk?.contains("高") == true -> Error
                                            else -> Warning
                                        },
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    item {
                        uiState.hrv?.let { hrv ->
                            HealthMetricCard(
                                title = "HRV（心率变异性）",
                                value = hrv,
                                icon = Icons.Default.ShowChart,
                                color = Secondary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryCard(level: Int) {
    val color = when {
        level >= 80 -> BatteryHigh
        level >= 40 -> BatteryMedium
        else -> BatteryLow
    }
    val status = when {
        level >= 80 -> "充沛"
        level >= 60 -> "正常"
        level >= 40 -> "偏低"
        else -> "需要恢复"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("身体电量", color = OnSurfaceVariant, fontSize = 13.sp)
                        Text("$level%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(status, color = color, fontWeight = FontWeight.Medium)
                    Text(
                        when {
                            level >= 80 -> "状态良好"
                            level >= 60 -> "略有消耗"
                            level >= 40 -> "建议休息"
                            else -> "建议深度恢复"
                        },
                        fontSize = 11.sp,
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = level / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = SurfaceVariant,
            )
        }
    }
}

@Composable
fun HealthMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    subtext: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = OnSurfaceVariant, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            subtext?.let {
                Text(it, color = OnSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}