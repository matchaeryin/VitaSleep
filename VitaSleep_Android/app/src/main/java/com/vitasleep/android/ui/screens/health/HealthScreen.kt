package com.vitasleep.android.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.components.BatteryRing
import com.vitasleep.android.ui.components.GlassCard
import com.vitasleep.android.ui.components.MetricCard
import com.vitasleep.android.ui.components.formatMetricValue
import com.vitasleep.android.ui.theme.DeepBg
import com.vitasleep.android.ui.theme.Dimens
import com.vitasleep.android.ui.theme.IceBlue
import com.vitasleep.android.ui.theme.MintGreen
import com.vitasleep.android.ui.theme.RoseRed
import com.vitasleep.android.ui.theme.SkyBlue
import com.vitasleep.android.ui.theme.TextPrimary
import com.vitasleep.android.ui.theme.TextSecondary
import com.vitasleep.android.ui.theme.TextTertiary
import com.vitasleep.android.ui.theme.TextDark
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(Unit) {
        try {
            viewModel.loadLatestMetrics(userId)
            viewModel.startSseListening(userId)
        } catch (e: Exception) {
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSseListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.padScreen, vertical = Dimens.spaceMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "身体状态",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            if (uiState.isSseConnected) {
                Text(
                    text = "● 实时同步",
                    style = MaterialTheme.typography.labelSmall,
                    color = MintGreen
                )
            } else {
                Text(
                    text = "● 离线",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IceBlue)
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(Dimens.padScreen), contentAlignment = Alignment.Center) {
                    GlassCard(accent = RoseRed) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(Dimens.spaceXl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = RoseRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Text("无法连接到服务器", color = TextPrimary, modifier = Modifier.padding(top = Dimens.spaceMd))
                            Text(uiState.error ?: "", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { viewModel.loadLatestMetrics(userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = IceBlue),
                                modifier = Modifier.padding(top = Dimens.spaceLg)
                            ) {
                                Text("重试", color = TextDark)
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.padScreen,
                        end = Dimens.padScreen,
                        bottom = Dimens.space2Xl
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)
                ) {
                    item {
                        HeroCard(
                            battery = uiState.battery,
                            statusText = uiState.statusText,
                            steps = uiState.steps
                        )
                    }
                    item {
                        CardioSection(
                            score = uiState.cardioScore,
                            level = uiState.cardioLevel,
                            risk = uiState.cardioRisk
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                            MetricCard(
                                name = "心率",
                                value = formatMetricValue(uiState.heartRate),
                                unit = "bpm",
                                icon = Icons.Default.Favorite,
                                accentColor = RoseRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                name = "血压",
                                value = uiState.bloodPressure ?: "—",
                                unit = "mmHg",
                                icon = Icons.Default.Speed,
                                accentColor = SkyBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                            MetricCard(
                                name = "HRV",
                                value = uiState.hrv ?: "—",
                                unit = "心率变异性",
                                icon = Icons.Default.Timeline,
                                accentColor = IceBlue,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                name = "血氧",
                                value = formatMetricValue(uiState.spo2),
                                unit = "%",
                                icon = Icons.Default.Air,
                                accentColor = MintGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(battery: Int?, statusText: String, steps: Int?) {
    GlassCard(accent = IceBlue) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryRing(
                percentage = battery ?: 0,
                size = Dimens.ringSizeLg,
                strokeWidth = 4.dp,
                label = "身体电量"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = Dimens.spaceMd)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (steps != null) {
                    Text(
                        text = "$steps 步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        modifier = Modifier.padding(top = Dimens.spaceXs)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardioSection(score: Int?, level: String?, risk: String?) {
    GlassCard(accent = if (score != null && score >= 60) MintGreen else RoseRed) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "心血管健康",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = level ?: "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = Dimens.spaceXs)
                )
            }
            Text(
                text = score?.let { "$it" } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }
        if (risk != null) {
            Text(
                text = risk,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.padding(top = Dimens.spaceXs)
            )
        }
    }
}
