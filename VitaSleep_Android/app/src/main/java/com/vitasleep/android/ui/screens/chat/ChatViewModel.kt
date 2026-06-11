package com.vitasleep.android.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.model.ChatMessage
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(private val repository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun loadChatHistory(userId: String) {
        viewModelScope.launch {
            repository.getChatHistory(userId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _uiState.value = _uiState.value.copy(messages = result.data, error = null)
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                    ApiResult.Loading -> {}
                }
            }
        }
    }

    fun sendMessage(userId: String, content: String) {
        val userMsg = ChatMessage(userId = userId, role = "user", content = content)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + userMsg, isLoading = true)
        viewModelScope.launch {
            repository.sendMessage(userId, content).collect { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, messages = _uiState.value.messages + result.data)
                    is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                    ApiResult.Loading -> {}
                }
            }
        }
    }
}
