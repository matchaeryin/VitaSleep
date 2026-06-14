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
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("records") val records: List<VeepooOriginRecord> = emptyList(),
    @SerializedName("data_date") val dataDate: String? = null
)

data class VeepooOriginRecord(
    @SerializedName("timestamp") val timestamp: String = "",
    @SerializedName("heart_rate") val heartRate: Int = 0,
    @SerializedName("heart_rate_array") val heartRateArray: List<Int>? = null,
    @SerializedName("systolic") val systolic: Int = 0,
    @SerializedName("diastolic") val diastolic: Int = 0,
    @SerializedName("steps") val steps: Int = 0,
    @SerializedName("spo2") val spo2: Int? = null
)

data class VeepooSleepDataRequest(
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("sleep_date") val sleepDate: String = "",
    @SerializedName("sleep_start") val sleepStart: String = "",
    @SerializedName("sleep_end") val sleepEnd: String = "",
    @SerializedName("total_sleep_min") val totalSleepMin: Int = 0,
    @SerializedName("deep_sleep_min") val deepSleepMin: Int = 0,
    @SerializedName("light_sleep_min") val lightSleepMin: Int = 0,
    @SerializedName("rem_sleep_min") val remSleepMin: Int = 0,
    @SerializedName("awake_min") val awakeMin: Int = 0,
    @SerializedName("deep_pct") val deepPct: Double = 0.0,
    @SerializedName("light_pct") val lightPct: Double = 0.0,
    @SerializedName("rem_pct") val remPct: Double = 0.0,
    @SerializedName("awake_pct") val awakePct: Double = 0.0,
    @SerializedName("quality_score") val qualityScore: Double? = null
)

data class VeepooUploadResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("records_processed") val recordsProcessed: Int = 0,
    @SerializedName("metric_ids") val metricIds: List<Int> = emptyList(),
    @SerializedName("errors") val errors: List<String> = emptyList()
)
