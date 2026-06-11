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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val battery: Int? = null,
    val heartRate: Int? = null,
    val bloodPressure: String? = null,
    val cardioIndex: String? = null,
    val cardioRisk: String? = null,
    val hrv: String? = null,
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
                            cardioRisk = parseCardioRisk(metrics["cardio_index"]?.value),
                            hrv = parseHrv(metrics["hrv"]?.value),
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
        }
    }

    fun startSseListening(userId: String) {
        currentUserId = userId
        val baseUrl = BuildConfig.BACKEND_BASE_URL.removeSuffix("/")

        viewModelScope.launch {
            sseClient.events.collect { event ->
                when (event) {
                    is SseHealthEvent.NewMetric -> refreshLatestMetrics()
                    is SseHealthEvent.Heartbeat -> {}
                    is SseHealthEvent.Error -> {}
                    is SseHealthEvent.Message -> {}
                }
            }
        }

        sseClient.connect(userId, baseUrl)
        _uiState.value = _uiState.value.copy(isSseConnected = true)
    }

    fun stopSseListening() {
        sseClient.disconnect()
        _uiState.value = _uiState.value.copy(isSseConnected = false)
    }

    private fun refreshLatestMetrics() {
        if (currentUserId.isNotEmpty()) {
            loadLatestMetrics(currentUserId)
        }
    }

    private fun toInt(value: Any?): Int? = when (value) {
        is Number -> value.toInt()
        is String -> value.toDoubleOrNull()?.toInt()
        else -> null
    }

    private fun toDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseBattery(value: Any?): Int? = try {
        when (value) {
            is Number -> value.toInt()
            is Map<*, *> -> toInt((value as Map<String, Any>)["level"])
            else -> null
        }
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun parseHeartRate(value: Any?): Int? = try {
        when (value) {
            is Number -> value.toInt()
            is Map<*, *> -> toInt((value as Map<String, Any>)["bpm"])
            else -> null
        }
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun parseBloodPressure(value: Any?): String? {
        return try {
            if (value is Map<*, *>) {
                val map = value as Map<String, Any>
                val systolic = toInt(map["systolic"])
                val diastolic = toInt(map["diastolic"])
                if (systolic != null && diastolic != null) "$systolic/$diastolic" else null
            } else null
        } catch (e: Exception) { null }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCardioIndex(value: Any?): String? = try {
        if (value is Map<*, *>) {
            val map = value as Map<String, Any>
            val index = toDouble(map["index"]) ?: toDouble(map["cardio_index"]) ?: toDouble(map["value"])
            index?.let { String.format("%.1f", it) }
        } else if (value is Number) String.format("%.1f", value.toDouble())
        else null
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun parseCardioRisk(value: Any?): String? = try {
        if (value is Map<*, *>) {
            val map = value as Map<String, Any>
            val risk = toDouble(map["risk"]) ?: toDouble(map["cardio_risk"])
            risk?.let { String.format("%.1f", it) }
        } else null
    } catch (e: Exception) { null }

    @Suppress("UNCHECKED_CAST")
    private fun parseHrv(value: Any?): String? = try {
        if (value is Map<*, *>) {
            val map = value as Map<String, Any>
            val rmssd = toDouble(map["rmssd"])
            val score = toDouble(map["score"])
            if (rmssd != null && score != null) {
                "RMSSD: ${String.format("%.1f", rmssd)} Score: ${String.format("%.0f", score)}"
            } else rmssd?.let { "RMSSD: ${String.format("%.1f", it)}" }
        } else if (value is Number) String.format("%.1f", value.toDouble())
        else null
    } catch (e: Exception) { null }

    override fun onCleared() {
        super.onCleared()
        stopSseListening()
    }
}
