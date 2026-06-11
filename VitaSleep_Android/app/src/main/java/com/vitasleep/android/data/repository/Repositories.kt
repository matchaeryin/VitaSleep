package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Suppress("unused")
class HealthRepositoryImpl(
    private val api: VitaSleepApi
) {
    fun getHealthMetrics(userId: String): Flow<ApiResult<List<HealthMetric>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getHealthMetrics(userId, "heart_rate")
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body()?.metrics ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取失败"))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}

@Suppress("unused")
class ScheduleRepositoryImpl(
    private val api: VitaSleepApi
) {
    fun getSchedules(userId: String): Flow<ApiResult<List<Schedule>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getSchedules(userId)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body() ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取失败"))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}

@Suppress("unused")
class ChatRepositoryImpl(
    private val api: VitaSleepApi
) {
    fun getChatHistory(userId: String): Flow<ApiResult<List<ChatMessage>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getChatHistory(userId)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body() ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取失败"))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}