package com.vitasleep.android.ui.screens.sleep

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager

private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun SleepScreen(viewModel: SleepViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID

    LaunchedEffect(userId) {
        try {
            viewModel.loadSleepData(userId)
        } catch (e: Exception) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "睡眠分析",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IceBlue, strokeWidth = 2.dp)
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
            .background(GlassBg, CardShape)
            .border(1.dp, GlassBorder, CardShape)
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
                        RemSleepColor.copy(alpha = 0.08f),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Nightlight, null,
                    tint = RemSleepColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "暂无睡眠数据",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error ?: "设备尚未记录睡眠信息",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = IceBlue),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("刷新", color = OnPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                "昨晚睡眠",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                hours ?: "--",
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "小时",
                color = TextDim,
                fontSize = 13.sp
            )
            qualityScore?.let { score ->
                Spacer(modifier = Modifier.height(16.dp))
                val scoreColor = when {
                    score >= 80 -> MintGreen
                    score >= 60 -> Amber
                    else -> RoseRed
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("质量评分", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        "${score.toInt()}",
                        color = scoreColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
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
                "睡眠阶段分布",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(18.dp))

            SleepStageRow("深睡", deepPct, DeepSleepColor)
            Spacer(modifier = Modifier.height(14.dp))
            SleepStageRow("浅睡", lightPct, LightSleepColor)
            Spacer(modifier = Modifier.height(14.dp))
            SleepStageRow("REM", remPct, RemSleepColor)
            Spacer(modifier = Modifier.height(14.dp))
            SleepStageRow("清醒", awakePct, AwakeColor)
        }
    }
}

@Composable
private fun SleepStageRow(
    name: String,
    pct: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = TextSecondary, fontSize = 13.sp)
            Text(
                "${pct.toInt()}%",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(TextDim.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (pct > 0f) pct / 100f else 0f)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}
