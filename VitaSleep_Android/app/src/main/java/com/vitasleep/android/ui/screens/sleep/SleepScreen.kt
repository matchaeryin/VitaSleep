package com.vitasleep.android.ui.screens.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

@Composable
fun SleepScreen(viewModel: SleepViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(userId) { viewModel.loadSleepData(userId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "\u7761\u7720\u5206\u6790",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IceBlue)
            }
        } else if (uiState.error != null && uiState.totalSleepHours == null) {
            SleepEmptyState(error = uiState.error) {
                viewModel.loadSleepData(userId)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SleepTotalCard(
                    hours = uiState.totalSleepHours,
                    qualityScore = uiState.qualityScore
                )
                SleepStagesCard(
                    deepPct = uiState.deepPct,
                    lightPct = uiState.lightPct,
                    remPct = uiState.remPct,
                    awakePct = uiState.awakePct
                )
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
private fun SleepEmptyState(error: String?, onRetry: () -> Unit) {
    GlassCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        RemSleepColor.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Nightlight, null,
                    tint = RemSleepColor,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "\u6682\u65e0\u7761\u7720\u6570\u636e",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error ?: "\u8bbe\u5907\u5c1a\u672a\u8bb0\u5f55\u7761\u7720\u4fe1\u606f",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = IceBlue)
            ) {
                Text("\u5237\u65b0", color = OnPrimary)
            }
        }
    }
}

@Composable
private fun SleepTotalCard(hours: String?, qualityScore: Float?) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "\u6628\u665a\u7761\u7720",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RemSleepColor.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension
                        )
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    hours ?: "--",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                "\u5c0f\u65f6",
                color = TextDim,
                fontSize = 14.sp
            )
            qualityScore?.let { score ->
                Spacer(modifier = Modifier.height(12.dp))
                val scoreColor = when {
                    score >= 80 -> MintGreen
                    score >= 60 -> Amber
                    else -> RoseRed
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "\u8d28\u91cf\u8bc4\u5206 ",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "${score.toInt()}",
                        color = scoreColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepStagesCard(
    deepPct: Float,
    lightPct: Float,
    remPct: Float,
    awakePct: Float
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "\u7761\u7720\u9636\u6bb5\u5206\u5e03",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))

            SleepStageBar(
                "\u6df1\u7761",
                deepPct,
                DeepSleepColor
            )
            Spacer(modifier = Modifier.height(14.dp))
            SleepStageBar(
                "\u6d45\u7761",
                lightPct,
                LightSleepColor
            )
            Spacer(modifier = Modifier.height(14.dp))
            SleepStageBar(
                "REM",
                remPct,
                RemSleepColor
            )
            Spacer(modifier = Modifier.height(14.dp))
            SleepStageBar(
                "\u6e05\u9192",
                awakePct,
                AwakeColor
            )
        }
    }
}

@Composable
private fun SleepStageBar(
    name: String,
    pct: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                name,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Text(
                "${pct.toInt()}%",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(TextDim.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (pct > 0f) pct / 100f else 0f)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}
