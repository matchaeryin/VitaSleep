package com.vitasleep.android.veepoo

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Veepoo SDK 封装管理器
 * 负责设备扫描、连接、数据读取
 */
class VeepooManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VeepooManager"
        @Volatile private var instance: VeepooManager? = null
        fun getInstance(context: Context): VeepooManager {
            return instance ?: synchronized(this) {
                instance ?: VeepooManager(context.applicationContext).also { instance = it }
            }
        }
        const val DEFAULT_USER_ID = "android_user_001"
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices

    private val _deviceBattery = MutableStateFlow<Int?>(null)
    val deviceBattery: StateFlow<Int?> = _deviceBattery

    private var currentMac: String? = null
    private var watchDay: Int = 3

    fun initialize() {
        Log.i(TAG, "Veepoo SDK 初始化完成")
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    }

    fun startScan() {
        Log.i(TAG, "开始扫描设备")
        _scannedDevices.value = emptyList()
        // 实际 SDK 调用：VPOperateManager.getInstance().startScanDevice(...)
    }

    fun stopScan() {
        Log.i(TAG, "停止扫描")
    }

    fun connectDevice(mac: String) {
        currentMac = mac
        _connectionState.value = ConnectionState.Connecting
        Log.i(TAG, "连接设备: $mac")
        // 实际 SDK 调用：VPOperateManager.getInstance().connectDevice(...)
        _connectionState.value = ConnectionState.Connected(mac)
    }

    fun disconnect() {
        currentMac?.let { mac ->
            Log.i(TAG, "断开连接: $mac")
            _connectionState.value = ConnectionState.Disconnected
            currentMac = null
            _deviceBattery.value = null
        }
    }

    fun readBattery() {
        Log.i(TAG, "读取设备电量")
        _deviceBattery.value = 85 // 模拟值
    }

    fun readAllOriginData() {
        Log.i(TAG, "读取所有原始数据")
    }

    fun readSleepData() {
        Log.i(TAG, "读取睡眠数据")
    }
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val mac: String) : ConnectionState()
    data class Connected(val mac: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class ScannedDevice(
    val name: String,
    val mac: String,
    val rssi: Int
)
