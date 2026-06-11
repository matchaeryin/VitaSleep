package com.vitasleep.android.data.repository

import com.vitasleep.android.data.api.VitaSleepApi
import com.vitasleep.android.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VeepooRepository @Inject constructor(
    private val api: VitaSleepApi
) {
    fun uploadOrigin5min(
        userId: String,
        deviceId: String?,
        records: List<VeepooOriginRecord>
    ): Flow<ApiResult<VeepooUploadResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val request = VeepooOriginDataRequest(
                userId = userId,
                deviceId = deviceId,
                records = records
            )
            val response = api.uploadVeepooOrigin5min(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(ApiResult.Success(it))
                } ?: emit(ApiResult.Error("无响应"))
            } else {
                emit(ApiResult.Error("上传失败: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    fun uploadSleep(
        userId: String,
        deviceId: String?,
        request: VeepooSleepDataRequest
    ): Flow<ApiResult<VeepooUploadResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = api.uploadVeepooSleep(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(ApiResult.Success(it))
                } ?: emit(ApiResult.Error("无响应"))
            } else {
                emit(ApiResult.Error("上传失败", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }

    fun syncAllData(
        userId: String,
        deviceId: String?,
        records: List<VeepooOriginRecord>
    ): Flow<ApiResult<VeepooUploadResponse>> = flow {
        emit(ApiResult.Loading)
        try {
            val request = VeepooOriginDataRequest(
                userId = userId,
                deviceId = deviceId,
                records = records,
                dataDate = records.firstOrNull()?.timestamp?.take(10)
            )
            val response = api.syncVeepooData(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(ApiResult.Success(it))
                } ?: emit(ApiResult.Error("无响应"))
            } else {
                emit(ApiResult.Error("同步失败", response.code()))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "网络错误"))
        }
    }
}