package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val api: VitaSleepApi
) {
    fun getLatestMetrics(userId: String): Flow<ApiResult<Map<String, HealthMetric>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getLatestMetrics(userId)
            if (response.isSuccessful) {
                val metricsList = response.body() ?: emptyList()
                val metricsMap = metricsList.associateBy { it.metricType }
                emit(ApiResult.Success(metricsMap))
            } else {
                emit(ApiResult.Error("获取数据失败: ${response.code()}", response.code()))
            }
        } catch (e: Throwable) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    fun getSleepData(userId: String, days: Int = 7): Flow<ApiResult<List<HealthMetric>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getSleep(userId, days)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body() ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取睡眠数据失败: ${response.code()}", response.code()))
            }
        } catch (e: Throwable) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}
