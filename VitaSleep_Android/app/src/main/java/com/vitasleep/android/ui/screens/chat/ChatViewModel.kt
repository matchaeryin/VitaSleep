package com.vitasleep.android.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.model.ChatMessage
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.ChatRepository
import com.vitasleep.android.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val battery: Int = 0,
    val statusText: String = "身体状态",
    val metricsSummary: String = "暂无数据"
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun loadHistory(userId: String) {
        viewModelScope.launch {
            try {
                repository.getChatHistory(userId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val sorted = result.data.sortedBy { it.id ?: 0 }
                            _uiState.value = _uiState.value.copy(messages = sorted)
                        }
                        is ApiResult.Error -> {}
                        ApiResult.Loading -> {}
                    }
                }
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadOverview(userId: String) {
        viewModelScope.launch {
            try {
                healthRepository.getLatestMetrics(userId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val metrics = result.data
                            val battery = try {
                                val v = metrics["battery"]?.value
                                if (v is Number) v.toInt()
                                else if (v is Map<*, *>) (v as Map<*, *>)["level"]?.toString()?.toIntOrNull()
                                else null
                            } catch (e: Exception) { null } ?: 0

                            val hr = try {
                                val v = metrics["heart_rate"]?.value
                                if (v is Number) v.toInt()
                                else if (v is Map<*, *>) (v as Map<*, *>)["bpm"]?.toString()?.toIntOrNull()
                                else null
                            } catch (e: Exception) { null }

                            val bp = metrics["blood_pressure"]?.value
                            val bpStr = if (bp is Map<*, *>) {
                                val sys = bp["systolic"]?.toString()
                                val dia = bp["diastolic"]?.toString()
                                if (sys != null && dia != null) "血压 $sys/$dia" else null
                            } else null

                            val summary = listOfNotNull(
                                hr?.let { "心率 $it" },
                                bpStr
                            ).joinToString(" · ").ifEmpty { "暂无数据" }

                            _uiState.value = _uiState.value.copy(
                                battery = battery,
                                metricsSummary = summary
                            )
                        }
                        else -> {}
                    }
                }
            } catch (e: Throwable) {
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(userId: String, content: String) {
        // 先添加用户消息到本地
        val userMsg = ChatMessage(
            userId = userId,
            role = "user",
            content = content
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isLoading = true,
            error = null,
            inputText = ""
        )

        viewModelScope.launch {
            repository.sendMessage(userId, content).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            messages = _uiState.value.messages + result.data
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
}
