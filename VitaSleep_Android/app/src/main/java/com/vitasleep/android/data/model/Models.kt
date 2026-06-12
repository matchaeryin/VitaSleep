package com.vitasleep.android.data.model

import com.google.gson.annotations.SerializedName

data class HealthMetric(
    val id: Int = 0,
    val userId: String = "",
    val metricType: String = "",
    val value: Any? = null,
    val computedAt: String = "",
    val validUntil: String? = null
)

data class HealthMetricsResponse(
    val metrics: List<HealthMetric> = emptyList(),
    val total: Int = 0
)

data class Schedule(
    val id: Int = 0,
    val userId: String = "",
    val title: String = "",
    val eventType: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val source: String = "",
    val status: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class CreateScheduleRequest(
    val userId: String = "",
    val title: String = "",
    val eventType: String = "flexible",
    val startTime: String = "",
    val endTime: String = ""
)

data class ChatMessage(
    val id: Int = 0,
    val userId: String = "",
    val role: String = "",
    val content: String = "",
    val agentType: String = "",
    val createdAt: String = ""
)

data class SendMessageRequest(
    val userId: String = "",
    val content: String = "",
    val agentType: String = "vitasleep-agent"
)

data class VeepooOriginDataRequest(
    val userId: String = "",
    val deviceId: String? = null,
    val deviceModel: String? = null,
    val records: List<VeepooOriginRecord> = emptyList(),
    val dataDate: String? = null
)

data class VeepooOriginRecord(
    val timestamp: String = "",
    val heartRate: Int = 0,
    val heartRateArray: List<Int>? = null,
    val systolic: Int = 0,
    val diastolic: Int = 0,
    val steps: Int = 0,
    val spo2: Int? = null
)

data class VeepooSleepDataRequest(
    val userId: String = "",
    val deviceId: String? = null,
    val sleepDate: String = "",
    val sleepStart: String = "",
    val sleepEnd: String = "",
    val totalSleepMin: Int = 0,
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
    val status: String = "",
    val message: String = "",
    val recordsProcessed: Int = 0,
    val metricIds: List<Int> = emptyList(),
    val errors: List<String> = emptyList()
)
