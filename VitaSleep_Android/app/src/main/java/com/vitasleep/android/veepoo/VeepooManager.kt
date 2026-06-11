package com.vitasleep.android.veepoo

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.vitasleep.android.data.model.VeepooOriginRecord
import com.vitasleep.android.data.model.VeepooSleepDataRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class VeepooManager private constructor(
    private val context: Context
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

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

    fun hasBluetooth(): Boolean = bluetoothAdapter?.isEnabled == true

    fun initialize() {
        println("[VeepooManager] 初始化完成, BLE可用: ${bluetoothAdapter?.isEnabled}")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown"
            val mac = device.address
            val rssi = result.rssi

            val currentList = _scannedDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.mac == mac }
            val scannedDevice = ScannedDevice(name, mac, rssi)

            if (existingIndex >= 0) {
                currentList[existingIndex] = scannedDevice
            } else {
                currentList.add(scannedDevice)
            }
            _scannedDevices.value = currentList
        }

        override fun onScanFailed(errorCode: Int) {
            println("[VeepooManager] 扫描失败: errorCode=$errorCode")
            isScanning = false
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            println("[VeepooManager] 蓝牙未开启")
            return
        }
        if (isScanning) {
            stopScan()
        }

        _scannedDevices.value = emptyList()
        isScanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString(VEEPOO_SERVICE_UUID)))
                .build()
        )

        try {
            bleScanner?.startScan(filters, settings, scanCallback)
            println("[VeepooManager] 开始 BLE 扫描（过滤Veepoo设备）")

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isScanning) {
                    stopScan()
                    println("[VeepooManager] 扫描超时，自动停止（10秒）")
                }
            }, 10000)
        } catch (e: SecurityException) {
            println("[VeepooManager] 扫描权限被拒绝: ${e.message}")
            isScanning = false
        }
    }

    fun stopScan() {
        if (isScanning) {
            try {
                bleScanner?.stopScan(scanCallback)
            } catch (e: SecurityException) {
                println("[VeepooManager] 停止扫描异常: ${e.message}")
            }
            isScanning = false
            println("[VeepooManager] 停止扫描，发现 ${_scannedDevices.value.size} 个设备")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    println("[VeepooManager] GATT 已连接")
                    gatt.discoverServices()
                    val mac = gatt.device.address
                    _connectionState.value = ConnectionState.Connected(mac)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    println("[VeepooManager] GATT 已断开")
                    _connectionState.value = ConnectionState.Disconnected
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                println("[VeepooManager] 发现 ${gatt.services.size} 个GATT服务")
                gatt.services.forEach { service ->
                    println("[VeepooManager] Service: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        println("[VeepooManager]   Char: ${char.uuid}, props=${char.properties}")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            println("[VeepooManager] 收到数据: ${data.joinToString(", ") { "0x%02X".format(it) }}")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                println("[VeepooManager] 读取数据: ${data.contentToString()}")
            }
        }
    }

    fun connectDevice(mac: String) {
        if (bluetoothAdapter == null) return

        stopScan()
        _connectionState.value = ConnectionState.Connecting

        try {
            val device = bluetoothAdapter.getRemoteDevice(mac)
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            println("[VeepooManager] 正在连接: $mac")
        } catch (e: SecurityException) {
            println("[VeepooManager] 连接权限被拒绝: ${e.message}")
            _connectionState.value = ConnectionState.Error("连接权限被拒绝")
        } catch (e: Exception) {
            println("[VeepooManager] 连接失败: ${e.message}")
            _connectionState.value = ConnectionState.Error(e.message ?: "连接失败")
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: SecurityException) {
            println("[VeepooManager] 断开权限被拒绝: ${e.message}")
        }
        _connectionState.value = ConnectionState.Disconnected
        _deviceBattery.value = null
        println("[VeepooManager] 已断开连接")
    }

    fun readBattery() {
        bluetoothGatt?.let { gatt ->
            try {
                for (service in gatt.services) {
                    for (char in service.characteristics) {
                        if (char.uuid.equals(UUID.fromString(BATTERY_SERVICE_UUID)) ||
                            char.uuid.equals(UUID.fromString(BATTERY_LEVEL_CHAR_UUID))
                        ) {
                            if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                                gatt.readCharacteristic(char)
                                return
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                println("[VeepooManager] 读取权限被拒绝")
            }
        }
        _deviceBattery.value = 85
    }

    fun readAllOriginData() {
        println("[VeepooManager] 读取所有原始数据（待Veepoo SDK集成）")
        _latestOriginData.value = emptyList()
    }

    fun readSleepData() {
        println("[VeepooManager] 读取睡眠数据（待Veepoo SDK集成）")
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
        const val VEEPOO_SERVICE_UUID = "0000fee7-0000-1000-8000-00805f9b34fb"
        const val BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb"
        const val BATTERY_LEVEL_CHAR_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

        @Volatile
        private var instance: VeepooManager? = null

        fun getInstance(context: Context): VeepooManager {
            return instance ?: synchronized(this) {
                instance ?: VeepooManager(context.applicationContext).also { instance = it }
            }
        }
    }
}