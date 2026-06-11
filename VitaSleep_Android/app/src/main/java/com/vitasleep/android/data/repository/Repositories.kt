package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

@Singleton
class HealthRepository @Inject constructor(private val api: VitaSleepApi) {
    fun getLatestMetrics(userId: String) = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getHealthMetrics(userId, limit = 10)
            if (response.isSuccessful) emit(ApiResult.Success(response.body() ?: emptyList()))
            else emit(ApiResult.Error("获取失败", response.code()))
        } catch (e: Exception) { emit(ApiResult.Error(e.message ?: "网络错误")) }
    }
}

@Singleton
class ScheduleRepository @Inject constructor(private val api: VitaSleepApi) {
    fun getSchedules(userId: String) = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getSchedules(userId)
            if (response.isSuccessful) emit(ApiResult.Success(response.body() ?: emptyList()))
            else emit(ApiResult.Error("获取失败", response.code()))
        } catch (e: Exception) { emit(ApiResult.Error(e.message ?: "网络错误")) }
    }

    suspend fun createSchedule(request: CreateScheduleRequest): ApiResult<Schedule> {
        return try {
            val response = api.createSchedule(request)
            if (response.isSuccessful) ApiResult.Success(response.body()!!) else ApiResult.Error("创建失败", response.code())
        } catch (e: Exception) { ApiResult.Error(e.message ?: "网络错误") }
    }

    suspend fun deleteSchedule(id: Int): ApiResult<Unit> {
        return try {
            val response = api.deleteSchedule(id)
            if (response.isSuccessful) ApiResult.Success(Unit) else ApiResult.Error("删除失败", response.code())
        } catch (e: Exception) { ApiResult.Error(e.message ?: "网络错误") }
    }
}

@Singleton
class ChatRepository @Inject constructor(private val api: VitaSleepApi) {
    fun sendMessage(userId: String, content: String) = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.sendMessage(SendMessageRequest(userId, content))
            if (response.isSuccessful) emit(ApiResult.Success(response.body()!!)) else emit(ApiResult.Error("发送失败", response.code()))
        } catch (e: Exception) { emit(ApiResult.Error(e.message ?: "网络错误")) }
    }

    fun getChatHistory(userId: String) = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getChatHistory(userId)
            if (response.isSuccessful) emit(ApiResult.Success(response.body() ?: emptyList())) else emit(ApiResult.Error("获取失败", response.code()))
        } catch (e: Exception) { emit(ApiResult.Error(e.message ?: "网络错误")) }
    }
}

@Singleton
class VeepooRepository @Inject constructor(private val api: VitaSleepApi) {
    fun uploadOrigin5min(userId: String, deviceId: String?, records: List<VeepooOriginRecord>) = flow {
        emit(ApiResult.Loading)
        try {
            val request = VeepooOriginDataRequest(userId, deviceId, records = records)
            val response = api.uploadVeepooOrigin5min(request)
            if (response.isSuccessful) emit(ApiResult.Success(response.body()!!)) else emit(ApiResult.Error("上传失败", response.code()))
        } catch (e: Exception) { emit(ApiResult.Error(e.message ?: "网络错误")) }
    }

    fun uploadSleep(userId: String, request: VeepooSleepDataRequest) = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.uploadVeepooSleep(request)
            if (response.isSuccessful) emit(ApiResult.Success(response.body()!!)) else emit(ApiResult.Error("上传失败", response.code()))
        } catch (e: Exception) { emit(ApiResult.Error(e.message ?: "网络错误")) }
    }
}
