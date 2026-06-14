package com.vitasleep.android.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.BuildConfig
import com.vitasleep.android.data.model.HealthMetric
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.HealthRepository
import com.vitasleep.android.data.repository.SseClient
import com.vitasleep.android.data.repository.SseHealthEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val battery: Int? = null,
    val heartRate: Int? = null,
    val bloodPressure: String? = null,
    val cardioIndex: String? = null,
    val cardioScore: Int? = null,
    val cardioLevel: String? = null,
    val cardioRisk: String? = null,
    val hrv: String? = null,
    val spo2: Int? = null,
    val steps: Int? = null,
    val statusText: String = "等待数据",
    val recentMetrics: List<HealthMetric> = emptyList(),
    val lastUpdateTime: Long = 0L,
    val isSseConnected: Boolean = false
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repository: HealthRepository,
    private val sseClient: SseClient
) : ViewModel() {

    private val _uiState: MutableStateFlow<HealthUiState> = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun loadLatestMetrics(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.getLatestMetrics(userId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val metrics = result.data
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                battery = parseBattery(metrics["battery"]?.value),
                                heartRate = parseHeartRate(metrics["heart_rate"]?.value),
                                bloodPressure = parseBloodPressure(metrics["blood_pressure"]?.value),
                                cardioIndex = parseCardioIndex(metrics["cardio_index"]?.value),
                                cardioScore = parseCardioScore(metrics["cardio_index"]?.value),
                                cardioLevel = parseCardioLevel(metrics["cardio_index"]?.value),
                                cardioRisk = parseCardioRisk(metrics["cardio_index"]?.value),
                                hrv = parseHrv(metrics["hrv"]?.value),
                                spo2 = parseSpo2(metrics["spo2"]?.value),
                                steps = parseSteps(metrics["steps"]?.value),
                                statusText = parseCardioLevel(metrics["cardio_index"]?.value) ?: "等待数据",
                                recentMetrics = metrics.values.toList(),
                                lastUpdateTime = System.currentTimeMillis(),
                                error = null
                            )
                        }
                        is ApiResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        ApiResult.Loading -> {}
                    }
                }
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * 启动 SSE 连接，接收实时数据推送
     * 当 Veepoo 设备上传新数据时，自动刷新页面
     */
    fun startSseListening(userId: String) {
        currentUserId = userId
        val baseUrl = BuildConfig.BACKEND_BASE_URL.removeSuffix("/")

        viewModelScope.launch {
            // 监听 SSE 事件
            sseClient.events.collect { event ->
                when (event) {
                    is SseHealthEvent.NewMetric -> {
                        // Veepoo 上传了新数据，自动刷新
                        println("[HealthVM] 收到新指标推送: ${event.type}, count=${event.count}")
                        refreshLatestMetrics()
                    }
                    is SseHealthEvent.Heartbeat -> {
                        // 心跳，不做处理
                    }
                    is SseHealthEvent.Disconnected -> {
                        println("[HealthVM] SSE 断开: ${event.message}")
                    }
                    is SseHealthEvent.Message -> {
                        // 其他消息
                    }
                }
            }
        }

        // 建立 SSE 连接
        sseClient.connectSafely(userId, baseUrl)
        _uiState.value = _uiState.value.copy(isSseConnected = true)
        println("[HealthVM] SSE 连接已建立: userId=$userId")
    }

    fun stopSseListening() {
        sseClient.disconnect()
        _uiState.value = _uiState.value.copy(isSseConnected = false)
        println("[HealthVM] SSE 连接已断开")
    }

    private fun refreshLatestMetrics() {
        if (currentUserId.isNotEmpty()) {
            loadLatestMetrics(currentUserId)
        }
    }

    private fun parseBattery(value: Any?): Int? {
        return try {
            if (value is Number) value.toInt()
            else if (value is Map<*, *>) (value as Map<*, *>)["level"]?.toString()?.toIntOrNull()
            else null
        } catch (e: Exception) { null }
    }

    private fun parseHeartRate(value: Any?): Int? {
        return try {
            if (value is Number) value.toInt()
            else if (value is Map<*, *>) (value as Map<*, *>)["bpm"]?.toString()?.toIntOrNull()
            else null
        } catch (e: Exception) { null }
    }

    private fun parseBloodPressure(value: Any?): String? {
        return try {
            if (value is Map<*, *>) {
                val sys = (value as Map<*, *>)["systolic"]?.toString() ?: return null
                val dia = value["diastolic"]?.toString() ?: return null
                "$sys/$dia"
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseCardioIndex(value: Any?): String? {
        return try {
            if (value is Map<*, *>) {
                (value as Map<*, *>)["score"]?.toString()?.let { "${it}/100" }
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseCardioRisk(value: Any?): String? {
        return try {
            if (value is Map<*, *>) {
                when ((value as Map<*, *>)["risk_level"]?.toString()) {
                    "low" -> "低风险"
                    "moderate" -> "中风险"
                    "high" -> "高风险"
                    else -> null
                }
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseCardioScore(value: Any?): Int? {
        return try {
            if (value is Map<*, *>) {
                (value as Map<*, *>)["score"]?.toString()?.toIntOrNull()
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseCardioLevel(value: Any?): String? {
        return try {
            val score = parseCardioScore(value) ?: return null
            when {
                score >= 80 -> "优秀"
                score >= 60 -> "良好"
                score >= 40 -> "一般"
                else -> "风险"
            }
        } catch (e: Exception) { null }
    }

    private fun parseHrv(value: Any?): String? {
        return try {
            if (value is Map<*, *>) {
                (value as Map<*, *>)["rmssd"]?.toString()?.let { "${it} ms" }
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseSpo2(value: Any?): Int? {
        return try {
            if (value is Number) value.toInt()
            else if (value is Map<*, *>) (value as Map<*, *>)["spo2"]?.toString()?.toIntOrNull()
            else value?.toString()?.toIntOrNull()
        } catch (e: Exception) { null }
    }

    private fun parseSteps(value: Any?): Int? {
        return try {
            if (value is Number) value.toInt()
            else if (value is Map<*, *>) (value as Map<*, *>)["count"]?.toString()?.toIntOrNull()
            else value?.toString()?.toIntOrNull()
        } catch (e: Exception) { null }
    }

    override fun onCleared() {
        super.onCleared()
        stopSseListening()
    }
}
