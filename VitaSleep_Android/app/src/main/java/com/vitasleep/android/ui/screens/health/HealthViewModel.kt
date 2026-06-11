package com.vitasleep.android.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.model.HealthMetric
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.HealthRepository
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
    val isSseConnected: Boolean = false
)

@HiltViewModel
class HealthViewModel @Inject constructor(private val repository: HealthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState

    fun loadLatestMetrics(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getLatestMetrics(userId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val metrics = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            battery = parseBattery(metrics.firstOrNull()?.value),
                            heartRate = parseHeartRate(metrics.firstOrNull()?.value),
                            error = null
                        )
                    }
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                    ApiResult.Loading -> {}
                }
            }
        }
    }

    private fun parseBattery(value: Any?): Int? = try { if (value is Number) value.toInt() else if (value is Map<*, *>) (value as Map<*, *>)["level"]?.toString()?.toIntOrNull() else null } catch (e: Exception) { null }
    private fun parseHeartRate(value: Any?): Int? = try { if (value is Number) value.toInt() else if (value is Map<*, *>) (value as Map<*, *>)["bpm"]?.toString()?.toIntOrNull() else null } catch (e: Exception) { null }
}
