package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val api: VitaSleepApi
) {
    fun getSchedules(userId: String): Flow<ApiResult<List<Schedule>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.getSchedules(userId)
            if (response.isSuccessful) {
                emit(ApiResult.Success(response.body() ?: emptyList()))
            } else {
                emit(ApiResult.Error("获取日程失败", response.code()))
            }
        } catch (e: Throwable) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    suspend fun createSchedule(request: CreateScheduleRequest): ApiResult<Schedule> {
        return try {
            val response = api.createSchedule(request)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("创建失败", response.code())
            }
        } catch (e: Throwable) {
            ApiResult.Error(e.message ?: "网络错误")
        }
    }

    suspend fun deleteSchedule(id: Int): ApiResult<Unit> {
        return try {
            val response = api.deleteSchedule(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("删除失败", response.code())
            }
        } catch (e: Throwable) {
            ApiResult.Error(e.message ?: "网络错误")
        }
    }
}