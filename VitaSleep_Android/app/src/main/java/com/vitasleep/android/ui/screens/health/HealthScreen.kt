package com.vitasleep.android.ui.screens.health

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

private val CardShape = RoundedCornerShape(24.dp)
private val IconBoxShape = RoundedCornerShape(12.dp)

@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(userId) {
        viewModel.loadLatestMetrics(userId)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "身体状态",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading && uiState.battery == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IceBlue, strokeWidth = 2.dp)
            }
        } else if (uiState.battery == null && uiState.heartRate == null && uiState.steps == null) {
            EmptyHealthState()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                uiState.battery?.let { BatteryRingCard(it) }
                uiState.steps?.let { StepsCard(it) }
                uiState.heartRate?.let { bpm ->
                    MetricCard(
                        title = "心率",
                        value = "$bpm",
                        unit = "bpm",
                        accentColor = RoseRed
                    )
                }
                uiState.bloodPressure?.let { bp ->
                    BloodPressureCard(bp)
                }
                uiState.cardioIndex?.let { idx ->
                    CardioCard(idx, uiState.cardioRisk)
                }
                uiState.hrv?.let { hrvStr ->
                    HrvCard(hrvStr)
                }
            }
        }
    }
}

@Composable
private fun EmptyHealthState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(IceBlue.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = IceBlue.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("暂无健康数据", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("连接设备并读取数据后，这里将显示你的身体状态", color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassBg, CardShape)
            .border(1.dp, GlassBorder, CardShape)
            .padding(20.dp),
        content = content
    )
}

@Composable
private fun BatteryRingCard(battery: Int) {
    val animatedProgress = animateFloatAsState(
        targetValue = battery / 100f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "battery"
    ).value

    val color = when {
        battery >= 60 -> MintGreen
        battery >= 20 -> Amber
        else -> RoseRed
    }

    GlassCard {
        Text("电池电量", color = TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(88.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = TextDim.copy(alpha = 0.1f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$battery",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("%", color = TextDim, fontSize = 11.sp)
                }
            }
            Column {
                Text(
                    when {
                        battery >= 60 -> "电量充足"
                        battery >= 20 -> "电量一般"
                        else -> "电量不足"
                    },
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StepsCard(steps: Int) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MintGreen.copy(alpha = 0.1f), IconBoxShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MintGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("今日步数", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$steps", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("步", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.1f), IconBoxShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(value, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(unit, color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun BloodPressureCard(bp: String) {
    val parts = bp.split("/")
    val systolic = parts.getOrNull(0)?.toIntOrNull() ?: 120
    val diastolic = parts.getOrNull(1)?.toIntOrNull() ?: 80

    val statusColor = when {
        systolic > 140 || diastolic > 90 -> RoseRed
        systolic > 130 || diastolic > 85 -> Amber
        else -> MintGreen
    }

    val statusText = when {
        systolic > 140 || diastolic > 90 -> "偏高"
        systolic > 130 || diastolic > 85 -> "正常偏高"
        else -> "正常"
    }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(statusColor.copy(alpha = 0.1f), IconBoxShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("血压", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$systolic", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text("/", color = TextDim, fontSize = 18.sp, modifier = Modifier.padding(bottom = 2.dp))
                    Text("$diastolic", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text("mmHg", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .height(3.dp)
                    .background(TextDim.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((systolic.toFloat() / 180f).coerceIn(0f, 1f))
                        .background(statusColor, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun CardioCard(cardioIndex: String, cardioRisk: String?) {
    val value = cardioIndex.toDoubleOrNull() ?: 0.0
    val levelColor = when {
        value >= 4 -> RoseRed
        value >= 2 -> Amber
        else -> MintGreen
    }
    val level = when {
        value >= 4 -> "高强度"
        value >= 2 -> "中等"
        else -> "低强度"
    }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(levelColor.copy(alpha = 0.1f), IconBoxShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = levelColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("有氧运动", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cardioIndex, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(level, color = levelColor, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                cardioRisk?.let { risk ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(risk, color = TextDim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun HrvCard(hrvStr: String) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(IceBlue.copy(alpha = 0.1f), IconBoxShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = IceBlue, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("心率变异性", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(hrvStr, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
