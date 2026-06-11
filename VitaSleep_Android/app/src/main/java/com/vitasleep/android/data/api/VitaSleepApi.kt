package com.vitasleep.android.data.api

import com.vitasleep.android.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface VitaSleepApi {

    @GET("api/health/metrics")
    suspend fun getHealthMetrics(
        @Query("user_id") userId: String,
        @Query("metric_type") metricType: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<List<HealthMetric>>

    @GET("api/health/battery")
    suspend fun getBattery(@Query("user_id") userId: String): Response<List<HealthMetric>>

    @GET("api/health/sleep")
    suspend fun getSleep(@Query("user_id") userId: String, @Query("days") days: Int = 7): Response<List<HealthMetric>>

    @GET("api/schedules")
    suspend fun getSchedules(
        @Query("user_id") userId: String,
        @Query("start_time") startTime: String? = null,
        @Query("end_time") endTime: String? = null
    ): Response<List<Schedule>>

    @POST("api/schedules")
    suspend fun createSchedule(@Body request: CreateScheduleRequest): Response<Schedule>

    @DELETE("api/schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: Int): Response<Unit>

    @POST("api/chat/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<ChatMessage>

    @GET("api/chat/history")
    suspend fun getChatHistory(
        @Query("user_id") userId: String,
        @Query("limit") limit: Int = 50
    ): Response<List<ChatMessage>>

    @POST("api/data/veepoo/origin5min")
    suspend fun uploadVeepooOrigin5min(@Body request: VeepooOriginDataRequest): Response<VeepooUploadResponse>

    @POST("api/data/veepoo/sleep")
    suspend fun uploadVeepooSleep(@Body request: VeepooSleepDataRequest): Response<VeepooUploadResponse>

    @POST("api/data/veepoo/sync")
    suspend fun syncVeepooData(@Body request: VeepooOriginDataRequest): Response<VeepooUploadResponse>
}
