package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

@Singleton
class HealthRepository @Inject constructor(
    private val api: VitaSleepApi
) {
    fun getLatestMetrics(userId: String): Flow<ApiResult<Map<String, HealthMetric>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getLatestMetrics(userId)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body() ?: emptyMap()))
            } else {
                emit(ApiResult.Error("获取数据失败: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    fun getMetricsByType(
        userId: String,
        metricType: String,
        limit: Int = 100
    ): Flow<ApiResult<List<HealthMetric>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getHealthMetrics(userId, metricType, limit = limit)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body()?.metrics ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取数据失败", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    fun getBattery(userId: String): Flow<ApiResult<HealthMetric>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getBattery(userId)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(ApiResult.Success(it))
                } ?: emit(ApiResult.Error("无数据"))
            } else {
                emit(ApiResult.Error("获取失败", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}