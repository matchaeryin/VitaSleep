package com.vitasleep.android.veepoo

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.vitasleep.android.data.model.VeepooOriginRecord
import com.vitasleep.android.data.model.VeepooSleepDataRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VeepooManager private constructor(
    private val context: Context
) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    private val _deviceBattery = MutableStateFlow<Int?>(null)
    private val _latestOriginData = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    private val _latestSleepData = MutableStateFlow<Map<String, Any>?>(null)

    val connectionState: StateFlow<ConnectionState> = _connectionState
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices
    val deviceBattery: StateFlow<Int?> = _deviceBattery
    val latestOriginData: StateFlow<List<Map<String, Any>>> = _latestOriginData
    val latestSleepData: StateFlow<Map<String, Any>?> = _latestSleepData

    fun initialize() {}

    fun hasBluetooth(): Boolean {
        return BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
    }

    fun startScan() {
        _scannedDevices.value = emptyList()
    }

    fun stopScan() {}

    fun connectDevice(mac: String) {
        _connectionState.value = ConnectionState.Connecting
        _connectionState.value = ConnectionState.Connected(mac)
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
        _deviceBattery.value = null
    }

    fun readBattery() {
        _deviceBattery.value = 85
    }

    fun readAllOriginData() {
        _latestOriginData.value = emptyList()
    }

    fun readSleepData() {
        _latestSleepData.value = null
    }

    fun convertOriginDataToUploadFormat(
        data: List<Map<String, Any>>
    ): List<VeepooOriginRecord> {
        return data.mapNotNull { record ->
            try {
                VeepooOriginRecord(
                    timestamp = record["timestamp"] as? String ?: return@mapNotNull null,
                    heartRate = (record["heart_rate"] as? Number)?.toInt() ?: 0,
                    systolic = (record["systolic"] as? Number)?.toInt() ?: 120,
                    diastolic = (record["diastolic"] as? Number)?.toInt() ?: 80,
                    steps = (record["steps"] as? Number)?.toInt() ?: 0
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun convertSleepDataToUploadFormat(
        data: Map<String, Any>
    ): VeepooSleepDataRequest {
        return VeepooSleepDataRequest(
            userId = DEFAULT_USER_ID,
            sleepDate = data["sleep_date"] as? String ?: "",
            sleepStart = data["sleep_start"] as? String ?: "",
            sleepEnd = data["sleep_end"] as? String ?: "",
            totalSleepMin = (data["total_sleep_min"] as? Number)?.toInt() ?: 0,
            deepSleepMin = (data["deep_sleep_min"] as? Number)?.toInt() ?: 0,
            lightSleepMin = (data["light_sleep_min"] as? Number)?.toInt() ?: 0,
            remSleepMin = (data["rem_sleep_min"] as? Number)?.toInt() ?: 0,
            awakeMin = (data["awake_min"] as? Number)?.toInt() ?: 0
        )
    }

    companion object {
        const val DEFAULT_USER_ID = "user_001"

        @Volatile
        private var instance: VeepooManager? = null

        fun getInstance(context: Context): VeepooManager {
            return instance ?: synchronized(this) {
                instance ?: VeepooManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
