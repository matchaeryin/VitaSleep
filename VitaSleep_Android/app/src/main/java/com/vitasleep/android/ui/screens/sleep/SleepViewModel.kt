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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // 实际调用 API 获取睡眠数据
            _uiState.value = _uiState.value.copy(isLoading = false, totalSleepHours = "7.5", deepPct = 20f, lightPct = 50f, remPct = 15f, awakePct = 15f, qualityScore = 85f)
        }
    }
}
