package com.vitasleep.android.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.data.model.Schedule
import com.vitasleep.android.ui.components.EventCard
import com.vitasleep.android.ui.components.GlassCard
import com.vitasleep.android.ui.components.getEventTypeConfig
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.VeepooManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = VeepooManager.DEFAULT_USER_ID
    var showAddDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    var activeDate by remember { mutableStateOf(today) }

    LaunchedEffect(userId) {
        try {
            viewModel.loadSchedules(userId)
        } catch (e: Exception) {
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
                text = "日程",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .background(IceBlue, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加",
                    tint = TextDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DaySwiper(
            activeDate = activeDate,
            onDateChange = { activeDate = it }
        )

        val filteredSchedules = uiState.schedules.filter { schedule ->
            try {
                val startDate = LocalDate.parse(schedule.startTime.substring(0, 10))
                startDate == activeDate
            } catch (e: Exception) {
                false
            }
        }

        ScheduleLegend(
            fixedCount = filteredSchedules.count { it.eventType == "fixed" },
            flexibleCount = filteredSchedules.count { it.eventType == "flexible" },
            healthCount = filteredSchedules.count {
                it.eventType == "health" || it.eventType == "health_intervention"
            }
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IceBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(Dimens.padScreen),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(accent = RoseRed) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(uiState.error ?: "加载失败", color = RoseRed)
                            Spacer(modifier = Modifier.height(Dimens.spaceSm))
                            Button(
                                onClick = { viewModel.loadSchedules(userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = IceBlue)
                            ) {
                                Text("重试", color = TextDark)
                            }
                        }
                    }
                }
            }
            filteredSchedules.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(Dimens.padScreen),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(Dimens.spaceMd))
                            Text("今天没有日程安排", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(Dimens.spaceXs))
                            Text("享受自由时光", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
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
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)
                ) {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        TimelineEventItem(
                            schedule = schedule,
                            onDelete = { viewModel.deleteSchedule(schedule.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, startTime, endTime ->
                viewModel.createSchedule(userId, title, startTime, endTime)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleLegend(
    fixedCount: Int,
    flexibleCount: Int,
    healthCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.padScreen, vertical = Dimens.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg)
    ) {
        LegendItem(color = IceBlue, label = "固定 $fixedCount")
        LegendItem(color = MintGreen, label = "弹性 $flexibleCount")
        LegendItem(color = RoseRed, label = "健康 $healthCount")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
private fun DaySwiper(
    activeDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val days = remember {
        (-3..3).map { offset ->
            today.plusDays(offset.toLong())
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.padScreen, vertical = Dimens.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val isActive = day == activeDate
            val isToday = day == today

            val bgColor = if (isActive) IceBlue else Color.Transparent
            val textColor = when {
                isActive -> TextDark
                isToday -> IceBlue
                else -> TextTertiary
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDateChange(day) }
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun TimelineEventItem(
    schedule: Schedule,
    onDelete: () -> Unit
) {
    val config = getEventTypeConfig(schedule.eventType)
    val timeRange = "${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}"

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(config.color.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, config.color, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            EventCard(
                title = schedule.title,
                timeRange = timeRange,
                description = "",
                type = schedule.eventType
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(top = Dimens.spaceMd, end = Dimens.spaceMd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "删除",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加日程", color = TextPrimary) },
        containerColor = Bg1,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("日程标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = IceBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = IceBlue
                    )
                )
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("开始时间 (ISO格式)") },
                    placeholder = { Text("2026-01-15T09:00:00Z") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = IceBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = IceBlue
                    )
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("结束时间 (ISO格式)") },
                    placeholder = { Text("2026-01-15T10:00:00Z") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = IceBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = IceBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, startTime, endTime) },
                enabled = title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IceBlue)
            ) {
                Text("添加", color = TextDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextTertiary)
            }
        }
    )
}

private fun formatTime(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString.replace("Z", ""))
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        isoString.takeLast(8).take(5)
    }
}
