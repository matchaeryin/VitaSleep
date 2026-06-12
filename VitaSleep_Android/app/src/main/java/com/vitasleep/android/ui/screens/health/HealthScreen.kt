package com.vitasleep.android.ui.screens.health

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitasleep.android.data.model.HealthMetric
import com.vitasleep.android.ui.theme.*
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    metrics: Map<String, HealthMetric>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = DeepBg,
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        "身体状态",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = DeepBg
                )
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DeepBg)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = IceBlue
                )
            } else if (metrics.isEmpty()) {
                EmptyHealthState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val batteryMetric = metrics["battery"]
                    if (batteryMetric != null) {
                        BatteryRingCard(batteryMetric)
                    }

                    val stepsMetric = metrics["steps"]
                    if (stepsMetric != null) {
                        StepsCard(stepsMetric)
                    }

                    val hrMetric = metrics["heart_rate"]
                    if (hrMetric != null) {
                        MetricGlowCard(
                            title = "心率",
                            value = formatMetricValue(hrMetric),
                            unit = "bpm",
                            icon = Icons.Filled.Favorite,
                            glowColor = RoseRed,
                            textColor = RoseRed
                        )
                    }

                    val bpMetric = metrics["blood_pressure"]
                    if (bpMetric != null) {
                        BloodPressureCard(bpMetric)
                    }

                    val spo2Metric = metrics["spo2"]
                    if (spo2Metric != null) {
                        MetricGlowCard(
                            title = "血氧",
                            value = formatMetricValue(spo2Metric),
                            unit = "%",
                            icon = Icons.Filled.Favorite,
                            glowColor = IceBlue,
                            textColor = IceBlue
                        )
                    }

                    val tempMetric = metrics["body_temp"]
                    if (tempMetric != null) {
                        MetricGlowCard(
                            title = "体温",
                            value = formatMetricValue(tempMetric),
                            unit = "°C",
                            icon = Icons.Filled.Favorite,
                            glowColor = Amber,
                            textColor = Amber
                        )
                    }

                    val cardioMetric = metrics["cardio"]
                    if (cardioMetric != null) {
                        CardioCard(cardioMetric)
                    }

                    val hrvMetric = metrics["hrv"]
                    if (hrvMetric != null) {
                        HrvCard(hrvMetric)
                    }

                    val stressMetric = metrics["stress"]
                    if (stressMetric != null) {
                        MetricGlowCard(
                            title = "压力",
                            value = formatMetricValue(stressMetric),
                            unit = "",
                            icon = Icons.Filled.Favorite,
                            glowColor = MintGreen,
                            textColor = MintGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHealthState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = IceBlueGlow,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = IceBlue,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无健康数据",
            color = TextSecondary,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请先连接设备并读取健康数据",
            color = TextDim,
            fontSize = 13.sp
        )
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
            .background(
                color = GlassBg,
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                drawRect(
                    color = GlassBorder,
                    size = size
                )
            }
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun BatteryRingCard(metric: HealthMetric) {
    val batteryValue = metric.value.toFloatOrNull() ?: 0f
    val animatedProgress = animateFloatAsState(
        targetValue = batteryValue / 100f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "battery"
    ).value

    val color = when {
        batteryValue >= 60 -> MintGreen
        batteryValue >= 20 -> Amber
        else -> RoseRed
    }

    GlassCard {
        Text(
            text = "电池电量",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = TextDim.copy(alpha = 0.15f),
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
                        text = "${batteryValue.toInt()}",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Column {
                Text(
                    text = when {
                        batteryValue >= 60 -> "电量充足"
                        batteryValue >= 20 -> "电量一般"
                        else -> "电量不足"
                    },
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metric.timestamp?.take(16) ?: "",
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StepsCard(metric: HealthMetric) {
    val steps = metric.value.toIntOrNull() ?: 0

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MintGreenGlow,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = MintGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日步数",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$steps",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "步",
                color = TextDim,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MetricGlowCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glowColor: Color,
    textColor: Color
) {
    GlassCard {
        Column {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = 0.3f), Color.Transparent),
                                center = Offset(size.width * 0.3f, size.height * 0.5f),
                                radius = size.width * 0.6f
                            )
                        )
                    }
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = value,
                        color = textColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = unit,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BloodPressureCard(metric: HealthMetric) {
    val parts = metric.value.split("/")
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
        Text(
            text = "血压",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$systolic",
                color = TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "/",
                color = TextDim,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "$diastolic",
                color = TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "mmHg",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = (systolic.toFloat() / 180f).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = statusColor,
            trackColor = TextDim.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun CardioCard(metric: HealthMetric) {
    val value = metric.value.toIntOrNull() ?: 0
    val level = when {
        value >= 4 -> "高强度"
        value >= 2 -> "中等强度"
        else -> "低强度"
    }
    val levelColor = when {
        value >= 4 -> RoseRed
        value >= 2 -> Amber
        else -> MintGreen
    }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = levelColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "有氧运动",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$value",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = level,
                        color = levelColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HrvCard(metric: HealthMetric) {
    val value = metric.value.toIntOrNull() ?: 0
    val status = when {
        value >= 60 -> "优秀"
        value >= 40 -> "良好"
        value >= 20 -> "一般"
        else -> "偏低"
    }
    val statusColor = when {
        value >= 60 -> MintGreen
        value >= 40 -> IceBlue
        value >= 20 -> Amber
        else -> RoseRed
    }

    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "心率变异性 (HRV)",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$value",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ms · $status",
                        color = statusColor,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

private fun formatMetricValue(metric: HealthMetric): String {
    val v = metric.value.toFloatOrNull()
    return if (v != null && v == v.toInt().toFloat()) {
        "${v.toInt()}"
    } else {
        metric.value
    }
}
