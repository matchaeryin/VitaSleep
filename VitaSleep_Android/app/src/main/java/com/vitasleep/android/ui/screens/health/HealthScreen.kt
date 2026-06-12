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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
        onDispose { viewModel.stopSseListening() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u8eab\u4f53\u72b6\u6001",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (uiState.isSseConnected) MintGreen else TextDim,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (uiState.isSseConnected) "\u5b9e\u65f6\u540c\u6b65" else "\u79bb\u7ebf",
                    fontSize = 12.sp,
                    color = if (uiState.isSseConnected) MintGreen else TextDim
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IceBlue)
                }
            }
            uiState.error != null -> {
                GlassCard {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff, null, tint = RoseRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("\u65e0\u6cd5\u8fde\u63a5\u5230\u670d\u52a1\u5668", color = TextPrimary)
                        Text(uiState.error ?: "", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadLatestMetrics(userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = IceBlue)
                        ) { Text("\u91cd\u8bd5", color = OnPrimary) }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    uiState.battery?.let { bat ->
                        item { BatteryRingCard(bat) }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            uiState.heartRate?.let { hr ->
                                MetricGlowCard(
                                    title = "\u5fc3\u7387",
                                    value = "$hr",
                                    unit = "bpm",
                                    icon = Icons.Default.Favorite,
                                    glowColor = RoseRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            uiState.steps?.let { s ->
                                MetricGlowCard(
                                    title = "\u6b65\u6570",
                                    value = "$s",
                                    unit = "\u6b65",
                                    icon = Icons.Default.DirectionsWalk,
                                    glowColor = MintGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    item {
                        uiState.bloodPressure?.let { bp ->
                            BloodPressureCard(bp)
                        }
                    }
                    item {
                        uiState.cardioIndex?.let { idx ->
                            CardioCard(idx, uiState.cardioRisk)
                        }
                    }
                    item {
                        uiState.hrv?.let { hrv ->
                            HrvCard(hrv)
                        }
                    }
                    if (uiState.battery == null && uiState.heartRate == null
                        && uiState.bloodPressure == null && uiState.hrv == null
                    ) {
                        item {
                            GlassCard {
                                Column(
                                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Sensors, null, tint = TextDim,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "\u6682\u65e0\u5065\u5eb7\u6570\u636e",
                                        color = TextSecondary,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "\u8bf7\u8fde\u63a5\u8bbe\u5907\u5e76\u4e0a\u4f20\u6570\u636e",
                                        color = TextDim,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(GlassBg, RoundedCornerShape(28.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
    ) {
        content()
    }
}

@Composable
private fun BatteryRingCard(level: Int) {
    val color = when {
        level >= 80 -> BatteryHigh
        level >= 40 -> BatteryMedium
        else -> BatteryLow
    }
    val statusText = when {
        level >= 80 -> "\u5145\u6c9b"
        level >= 60 -> "\u6b63\u5e38"
        level >= 40 -> "\u504f\u4f4e"
        else -> "\u9700\u8981\u4f11\u606f"
    }

    GlassCard {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                val sweepAngle = 360f * (level / 100f)
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2 - strokeWidth / 2,
                        (size.height - radius * 2) / 2 - strokeWidth / 2
                    )
                    drawArc(
                        color = TextDim.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(color, color.copy(alpha = 0.6f)),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$level",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text("%", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    "\u8eab\u4f53\u7535\u91cf",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    statusText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when {
                        level >= 80 -> "\u72b6\u6001\u826f\u597d"
                        level >= 60 -> "\u7565\u6709\u6d88\u8017"
                        level >= 40 -> "\u5efa\u8bae\u4f11\u606f"
                        else -> "\u5efa\u8bae\u6df1\u5ea6\u6062\u590d"
                    },
                    fontSize = 12.sp,
                    color = TextDim
                )
            }
        }
    }
}

@Composable
private fun MetricGlowCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, null, tint = glowColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.2f), Color.Transparent),
                            center = center,
                            radius = size.minDimension
                        )
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = glowColor
                    )
                }
            }
            Text(unit, color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BloodPressureCard(bp: String) {
    val parts = bp.split("/")
    val systolic = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val diastolic = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val statusColor = when {
        systolic > 140 || diastolic > 90 -> RoseRed
        systolic > 130 || diastolic > 85 -> Amber
        else -> MintGreen
    }
    val statusText = when {
        systolic > 140 || diastolic > 90 -> "\u504f\u9ad8"
        systolic > 130 || diastolic > 85 -> "\u4e34\u754c"
        else -> "\u6b63\u5e38"
    }

    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed, null, tint = Amber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "\u8840\u538b",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Text(
                    statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        parts.getOrNull(0) ?: "--",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "\u6536\u7f29\u538b",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
                Text(
                    "/",
                    fontSize = 28.sp,
                    color = TextDim,
                    fontWeight = FontWeight.Light
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        parts.getOrNull(1) ?: "--",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "\u8212\u5f20\u538b",
                        color = TextDim,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (systolic.toFloat() / 180f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = statusColor,
                trackColor = TextDim.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun CardioCard(index: String, risk: String?) {
    val riskColor = when {
        risk?.contains("\u4f4e") == true -> MintGreen
        risk?.contains("\u9ad8") == true -> RoseRed
        else -> Amber
    }

    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(IceBlueGlow, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder, null,
                        tint = IceBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        "\u5fc3\u8840\u7ba1\u6307\u6570",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        index,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
            risk?.let {
                Text(
                    it,
                    color = riskColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HrvCard(hrv: String) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(RemSleepColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ShowChart, null,
                        tint = RemSleepColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        "HRV \u5fc3\u7387\u53d8\u5f02\u6027",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        hrv,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
