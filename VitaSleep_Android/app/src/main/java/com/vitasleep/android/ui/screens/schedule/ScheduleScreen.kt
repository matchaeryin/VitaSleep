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
import com.vitasleep.android.data.model.Schedule
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
        viewModel.loadSchedules(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "日程管理",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
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

        val fixedCount = filteredSchedules.count { it.eventType == "fixed" }
        val healthCount = filteredSchedules.count {
            it.eventType == "health" || it.eventType == "health_intervention"
        }
        val flexibleCount = filteredSchedules.count { it.eventType == "flexible" }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(IceBlue, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text("固定 $fixedCount", fontSize = 11.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(SkyBlue, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text("弹性 $flexibleCount", fontSize = 11.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(MintGreen, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text("健康干预 $healthCount", fontSize = 11.sp, color = TextSecondary)
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IceBlue)
                }
            }
            uiState.error != null -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Surface.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error ?: "加载失败", color = RoseRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadSchedules(userId) },
                            colors = ButtonDefaults.buttonColors(containerColor = IceBlue)
                        ) {
                            Text("重试", color = TextDark)
                        }
                    }
                }
            }
            filteredSchedules.isEmpty() -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Surface.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("今天没有日程安排", color = TextPrimary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("享受自由时光", color = TextDim, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        TimelineEventCard(
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
            .padding(vertical = 8.dp),
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
                else -> TextDim
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
                    fontSize = 11.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${day.dayOfMonth}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

private data class EventTypeConfig(
    val label: String,
    val color: Color,
    val bgColor: Color
)

private fun getEventTypeConfig(eventType: String): EventTypeConfig {
    return when (eventType) {
        "fixed" -> EventTypeConfig(
            label = "固定",
            color = IceBlue,
            bgColor = IceBlue.copy(alpha = 0.08f)
        )
        "health", "health_intervention" -> EventTypeConfig(
            label = "健康干预",
            color = MintGreen,
            bgColor = MintGreen.copy(alpha = 0.08f)
        )
        else -> EventTypeConfig(
            label = "弹性",
            color = SkyBlue,
            bgColor = SkyBlue.copy(alpha = 0.08f)
        )
    }
}

@Composable
private fun TimelineEventCard(
    schedule: Schedule,
    onDelete: () -> Unit
) {
    val config = getEventTypeConfig(schedule.eventType)

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(config.bgColor, CircleShape)
                    .border(2.dp, config.color, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.06f))
            )
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 0.dp, bottom = 8.dp)
                .border(1.dp, config.color.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = config.bgColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = schedule.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = config.color.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = config.label,
                                fontSize = 10.sp,
                                color = config.color,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatTime(schedule.startTime)} - ${formatTime(schedule.endTime)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        tint = TextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
        containerColor = Surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("日程标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedLabelColor = IceBlue
                    )
                )
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("开始时间 (ISO格式)") },
                    placeholder = { Text("2025-01-15T09:00:00Z") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedLabelColor = IceBlue
                    )
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("结束时间 (ISO格式)") },
                    placeholder = { Text("2025-01-15T10:00:00Z") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedLabelColor = IceBlue
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
                Text("取消", color = TextSecondary)
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
