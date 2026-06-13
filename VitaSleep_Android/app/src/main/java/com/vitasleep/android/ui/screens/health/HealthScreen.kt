package com.vitasleep.android.ui.screens.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
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
                                .size(6.dp)
                                .background(MintGreen, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("实时同步", fontSize = 11.sp, color = MintGreen)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(TextDim, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("离线", fontSize = 11.sp, color = TextDim)
                    }
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IceBlue)
                }
            }
            uiState.error != null -> {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = RoseRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("无法连接到服务器", color = TextPrimary)
                        Text(uiState.error ?: "", color = TextDim, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadLatestMetrics(userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = IceBlue)
                        ) {
                            Text("重试", color = TextDark)
                        }
                    }
                }
            }
            else -> {
                val hasData = uiState.battery != null || uiState.heartRate != null
                        || uiState.bloodPressure != null || uiState.cardioScore != null
                        || uiState.hrv != null

                if (hasData) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            uiState.battery?.let { BatteryRingCard(level = it) }
                        }

                        item {
                            uiState.cardioScore?.let { score ->
                                CardioIndexCard(
                                    score = score,
                                    level = uiState.cardioLevel ?: ""
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                uiState.heartRate?.let {
                                    MetricCard(
                                        title = "心率",
                                        value = "$it",
                                        unit = "bpm",
                                        icon = Icons.Default.Favorite,
                                        color = RoseRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                uiState.bloodPressure?.let { bp ->
                                    MetricCard(
                                        title = "血压",
                                        value = bp,
                                        unit = "mmHg",
                                        icon = Icons.Default.Speed,
                                        color = SkyBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            uiState.hrv?.let { hrv ->
                                MetricCard(
                                    title = "HRV 心率变异性",
                                    value = hrv,
                                    unit = "",
                                    icon = Icons.Default.ShowChart,
                                    color = IceBlue,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint = TextDim,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "暂无健康数据",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "请先在设备页连接手表并同步数据",
                                fontSize = 14.sp,
                                color = TextDim
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = Surface.copy(alpha = 0.6f)
    ) {
        content()
    }
}

@Composable
private fun BatteryRingCard(level: Int) {
    val ringColor = when {
        level >= 70 -> MintGreen
        level >= 40 -> IceBlue
        else -> RoseRed
    }
    val status = when {
        level >= 80 -> "充沛"
        level >= 60 -> "正常"
        level >= 40 -> "偏低"
        else -> "需要恢复"
    }
    val desc = when {
        level >= 70 -> "精力充沛，适合重要事务"
        level >= 40 -> "电量适中，注意休息"
        else -> "电量偏低，建议恢复"
    }

    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(108.dp)
            ) {
                Canvas(modifier = Modifier.size(108.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )

                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    val sweepAngle = (level / 100f) * 360f
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$level",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = ringColor
                    )
                    Text(
                        text = "%",
                        fontSize = 11.sp,
                        color = TextDim
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = "身体电量",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "状态：",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = status,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ringColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = TextDim,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CardioIndexCard(score: Int, level: String) {
    val accentColor = when {
        score >= 80 -> MintGreen
        score >= 60 -> IceBlue
        else -> RoseRed
    }

    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                accentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "心血管健康指数",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = level,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$score",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = "/100",
                            fontSize = 14.sp,
                            color = TextDim,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(score / 100f)
                        .background(accentColor, RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("风险 <60", fontSize = 11.sp, color = TextDim)
                Text("良好 60-80", fontSize = 11.sp, color = TextDim)
                Text("优秀 ≥80", fontSize = 11.sp, color = TextDim)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard {
        Column(
            modifier = modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = TextDim,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}
