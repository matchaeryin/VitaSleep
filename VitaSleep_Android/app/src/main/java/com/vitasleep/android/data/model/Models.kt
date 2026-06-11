package com.vitasleep.android.data.model

import com.google.gson.annotations.SerializedName

// ─── 健康指标 ───

data class HealthMetric(
    val id: Int,
    val userId: String,
    val metricType: String,
    val value: Any,
    val computedAt: String,
    val validUntil: String? = null
)

data class HealthMetricsResponse(
    val metrics: List<HealthMetric>,
    val total: Int
)

// ─── 日程 ───

data class Schedule(
    val id: Int,
    val userId: String,
    val title: String,
    val eventType: String,
    val startTime: String,
    val endTime: String,
    val source: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class CreateScheduleRequest(
    val userId: String,
    val title: String,
    val eventType: String = "flexible",
    val startTime: String,
    val endTime: String
)

// ─── 聊天 ───

data class ChatMessage(
    val id: Int? = null,
    val userId: String,
    val role: String,
    val content: String,
    val agentType: String? = null,
    val createdAt: String? = null
)

data class SendMessageRequest(
    val userId: String,
    val content: String,
    val agentType: String = "vitasleep-agent"
)

// ─── Veepoo 设备数据 ───

data class VeepooOriginDataRequest(
    val userId: String,
    val deviceId: String? = null,
    val deviceModel: String? = null,
    val records: List<VeepooOriginRecord>,
    val dataDate: String? = null
)

data class VeepooOriginRecord(
    val timestamp: String,
    val heartRate: Int,
    val heartRateArray: List<Int>? = null,
    val systolic: Int,
    val diastolic: Int,
    val steps: Int = 0,
    val spo2: Int? = null
)

data class VeepooSleepDataRequest(
    val userId: String,
    val deviceId: String? = null,
    val sleepDate: String,
    val sleepStart: String,
    val sleepEnd: String,
    val totalSleepMin: Int,
    val deepSleepMin: Int = 0,
    val lightSleepMin: Int = 0,
    val remSleepMin: Int = 0,
    val awakeMin: Int = 0,
    val deepPct: Double = 0.0,
    val lightPct: Double = 0.0,
    val remPct: Double = 0.0,
    val awakePct: Double = 0.0,
    val qualityScore: Double? = null
)

data class VeepooUploadResponse(
    val status: String,
    val message: String,
    val recordsProcessed: Int = 0,
    val metricIds: List<Int> = emptyList(),
    val errors: List<String> = emptyList()
)
