package com.vitasleep.android.veepoo

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
    private val _isScanning = MutableStateFlow(false)

    val connectionState: StateFlow<ConnectionState> = _connectionState
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices
    val deviceBattery: StateFlow<Int?> = _deviceBattery
    val latestOriginData: StateFlow<List<Map<String, Any>>> = _latestOriginData
    val latestSleepData: StateFlow<Map<String, Any>?> = _latestSleepData
    val isScanning: StateFlow<Boolean> = _isScanning

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val bleScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private val scannedMap = mutableMapOf<String, ScannedDevice>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Device"
            val mac = device.address
            val rssi = result.rssi

            val existing = scannedMap[mac]
            if (existing == null || existing.rssi != rssi) {
                scannedMap[mac] = ScannedDevice(name = name, mac = mac, rssi = rssi)
                _scannedDevices.value = scannedMap.values.sortedByDescending { it.rssi }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    fun initialize() {}

    fun hasBluetooth(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return

        val scanner = bleScanner ?: return
        if (bluetoothAdapter?.isEnabled != true) return

        scannedMap.clear()
        _scannedDevices.value = emptyList()
        _isScanning.value = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        bleScanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    fun connectDevice(mac: String) {
        stopScan()
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
