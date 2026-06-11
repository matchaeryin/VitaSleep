package com.vitasleep.android.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.model.CreateScheduleRequest
import com.vitasleep.android.data.model.Schedule
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val schedules: List<Schedule> = emptyList()
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(private val repository: ScheduleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState

    fun loadSchedules(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getSchedules(userId).collect { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, schedules = result.data, error = null)
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                    ApiResult.Loading -> {}
                }
            }
        }
    }

    fun createSchedule(userId: String, title: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            val request = CreateScheduleRequest(userId, title, startTime = startTime, endTime = endTime)
            when (val result = repository.createSchedule(request)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(schedules = _uiState.value.schedules + result.data)
                else -> {}
            }
        }
    }

    fun deleteSchedule(id: Int) {
        viewModelScope.launch {
            when (repository.deleteSchedule(id)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(schedules = _uiState.value.schedules.filter { it.id != id })
                else -> {}
            }
        }
    }
}
