package com.vitasleep.android.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val healthRepository: HealthRepository
) {
    private val gson = Gson()
    private var eventSource: EventSource? = null

    private val _events = MutableSharedFlow<SseHealthEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<SseHealthEvent> = _events.asSharedFlow()

    fun connectSafely(userId: String, baseUrl: String) {
        try {
            connect(userId, baseUrl)
        } catch (e: Throwable) {
            println("[SSE] 安全连接失败: ${e.message}")
            _events.tryEmit(SseHealthEvent.Disconnected(e.message ?: "SSE connection unavailable"))
        }
    }

    fun connect(userId: String, baseUrl: String) {
        disconnect()

        val url = "$baseUrl/api/health/stream/$userId"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                println("[SSE] 连接建立: $url")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                val jsonStr = data.trim()
                if (jsonStr.startsWith("data:")) {
                    val payload = jsonStr.removePrefix("data:").trim()
                    try {
                        val event = gson.fromJson(payload, JsonObject::class.java)
                        val eventType = event.get("event")?.asString ?: "message"
                        val eventData = event.getAsJsonObject("data")

                        when (eventType) {
                            "heartbeat" -> {
                                _events.tryEmit(SseHealthEvent.Heartbeat)
                            }
                            "new_metric" -> {
                                val metricType = eventData?.get("type")?.asString
                                val count = eventData?.get("count")?.asInt
                                _events.tryEmit(
                                    SseHealthEvent.NewMetric(
                                        type = metricType ?: "unknown",
                                        count = count ?: 0
                                    )
                                )
                            }
                            else -> {
                                _events.tryEmit(
                                    SseHealthEvent.Message(
                                        eventType = eventType,
                                        data = eventData?.toString()
                                    )
                                )
                            }
                        }
                    } catch (e: Throwable) {
                        println("[SSE] 解析失败: $e, data=$payload")
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                println("[SSE] 连接关闭")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                println("[SSE] 连接失败: ${t?.message ?: "unknown"}, response=$response")
                _events.tryEmit(SseHealthEvent.Disconnected(t?.message ?: "SSE 连接失败"))
            }
        }

        try {
            eventSource = EventSources.createFactory(okHttpClient)
                .newEventSource(request, listener)
        } catch (e: Throwable) {
            println("[SSE] 创建连接失败: ${e.message}")
            _events.tryEmit(SseHealthEvent.Disconnected(e.message ?: "SSE 连接创建失败"))
        }
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
        println("[SSE] 已断开")
    }

    fun isConnected(): Boolean = eventSource != null
}

sealed class SseHealthEvent {
    object Heartbeat : SseHealthEvent()

    data class NewMetric(
        val type: String,
        val count: Int
    ) : SseHealthEvent()

    data class Message(
        val eventType: String,
        val data: String?
    ) : SseHealthEvent()

    data class Disconnected(val message: String) : SseHealthEvent()
}