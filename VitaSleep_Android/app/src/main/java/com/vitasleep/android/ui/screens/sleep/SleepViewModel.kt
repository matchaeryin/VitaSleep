package com.vitasleep.android.ui.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SleepUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalSleepHours: String? = null,
    val deepPct: Float = 0f,
    val lightPct: Float = 0f,
    val remPct: Float = 0f,
    val awakePct: Float = 0f,
    val qualityScore: Float? = null
)

@HiltViewModel
class SleepViewModel @Inject constructor(private val repository: HealthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState

    fun loadSleepData(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                repository.getSleepData(userId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val sleepMetrics = result.data
                            if (sleepMetrics.isEmpty()) {
                                _uiState.value = _uiState.value.copy(isLoading = false, error = "暂无睡眠数据")
                            } else {
                                val latestSleep = sleepMetrics.first()
                                val value = latestSleep.value
                                if (value is Map<*, *>) {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = value as Map<String, Any>
                                    val totalSleepMin = (map["total_sleep_min"] as? Number)?.toInt()
                                    val sleepHours = totalSleepMin?.let { String.format("%.1f", it / 60.0) }
                                    val deepPct = (map["deep_pct"] as? Number)?.toFloat() ?: 0f
                                    val lightPct = (map["light_pct"] as? Number)?.toFloat() ?: 0f
                                    val remPct = (map["rem_pct"] as? Number)?.toFloat() ?: 0f
                                    val awakePct = (map["awake_pct"] as? Number)?.toFloat() ?: 0f
                                    val qualityScore = (map["quality_score"] as? Number)?.toFloat()
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        totalSleepHours = sleepHours,
                                        deepPct = deepPct,
                                        lightPct = lightPct,
                                        remPct = remPct,
                                        awakePct = awakePct,
                                        qualityScore = qualityScore,
                                        error = null
                                    )
                                } else {
                                    _uiState.value = _uiState.value.copy(isLoading = false, error = "睡眠数据格式错误")
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                        }
                        ApiResult.Loading -> {}
                    }
                }
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
