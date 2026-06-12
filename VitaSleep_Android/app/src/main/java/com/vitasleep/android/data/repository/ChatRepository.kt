package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val api: VitaSleepApi
) {
    fun sendMessage(userId: String, content: String): Flow<ApiResult<ChatMessage>> = flow {
        emit(ApiResult.Loading)
        try {
            val request = SendMessageRequest(userId, content)
            val response = api.sendMessage(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(ApiResult.Success(ChatMessage(
                        id = it.id,
                        userId = it.userId,
                        role = it.role,
                        content = it.content,
                        agentType = it.agentType,
                        createdAt = it.createdAt
                    )))
                } ?: emit(ApiResult.Error("无响应"))
            } else {
                emit(ApiResult.Error("发送失败", response.code()))
            }
        } catch (e: Throwable) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    fun getChatHistory(userId: String): Flow<ApiResult<List<ChatMessage>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getChatHistory(userId)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body() ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取历史失败", response.code()))
            }
        } catch (e: Throwable) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}