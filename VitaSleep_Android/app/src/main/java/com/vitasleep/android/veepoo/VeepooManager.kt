package com.vitasleep.android.veepoo

import android.content.Context
import android.util.Log
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IABleConnectStatusListener
import com.veepoo.protocol.listener.base.IABluetoothStateListener
import com.veepoo.protocol.listener.base.IBleWriteResponse
import com.veepoo.protocol.listener.base.IConnectResponse
import com.veepoo.protocol.listener.base.INotifyResponse
import com.veepoo.protocol.listener.data.*
import com.veepoo.protocol.model.datas.*
import com.veepoo.protocol.model.settings.CustomSettingData
import com.inuker.bluetooth.library.Code
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.model.BleGattProfile
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.vitasleep.android.data.model.VeepooOriginRecord
import com.vitasleep.android.data.model.VeepooSleepDataRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

class VeepooManager private constructor(
    private val context: Context
) {

    private val vpOperateManager: VPOperateManager = VPOperateManager.getInstance()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    private val _deviceBattery = MutableStateFlow<Int?>(null)
    private val _latestOriginData = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    private val _latestSleepData = MutableStateFlow<Map<String, Any>?>(null)
    private val _isScanning = MutableStateFlow(false)
    private val _originDataProgress = MutableStateFlow(0f)
    private val _originDataReading = MutableStateFlow(false)

    val connectionState: StateFlow<ConnectionState> = _connectionState
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices
    val deviceBattery: StateFlow<Int?> = _deviceBattery
    val latestOriginData: StateFlow<List<Map<String, Any>>> = _latestOriginData
    val latestSleepData: StateFlow<Map<String, Any>?> = _latestSleepData
    val isScanning: StateFlow<Boolean> = _isScanning
    val originDataProgress: StateFlow<Float> = _originDataProgress
    val originDataReading: StateFlow<Boolean> = _originDataReading

    private var watchDataDay = 3
    private var connectedMac: String? = null
    private var connectedName: String? = null
    private var isOadModel = false

    private val scannedMap = mutableMapOf<String, ScannedDevice>()

    @Volatile
    private var sdkInitialized = false

    private var logcatProcess: Process? = null

    private val searchResponse = object : SearchResponse {
        override fun onSearchStarted() {
            _isScanning.value = true
        }

        override fun onDeviceFounded(device: SearchResult) {
            try {
                val name = device.name ?: return
                if (name.isBlank()) return
                val mac = device.address ?: return
                val rssi = device.rssi

                scannedMap[mac] = ScannedDevice(name = name, mac = mac, rssi = rssi)
                _scannedDevices.value = scannedMap.values.sortedByDescending { it.rssi }
            } catch (e: Throwable) {
                Log.e(TAG, "onDeviceFounded error", e)
            }
        }

        override fun onSearchStopped() {
            _isScanning.value = false
        }

        override fun onSearchCanceled() {
            _isScanning.value = false
        }
    }

    private val connectStatusListener = object : IABleConnectStatusListener() {
        override fun onConnectStatusChanged(mac: String, status: Int) {
            try {
                Log.d(TAG, "onConnectStatusChanged: mac=$mac status=$status")
                if (status == Constants.STATUS_DISCONNECTED) {
                    stopRealtimeLogcat()
                    VeepooService.stop(context)
                    _connectionState.value = ConnectionState.Disconnected
                    _deviceBattery.value = null
                }
            } catch (e: Throwable) {
                Log.e(TAG, "connectStatusListener error", e)
            }
        }
    }

    private val defaultWriteResponse = object : IBleWriteResponse {
        override fun onResponse(code: Int) {}
    }

    fun initialize() {
        Log.d(TAG, "initialize: SDK already initialized by VitaSleepApp.onCreate, skipping duplicate init")
        sdkInitialized = true

        try {
            vpOperateManager.setDeviceShowConfirm(true)
            Log.d(TAG, "initialize: setDeviceShowConfirm(true)")
        } catch (e: Throwable) {
            Log.w(TAG, "setDeviceShowConfirm failed", e)
        }

        try {
            vpOperateManager.registerBluetoothStateListener(object : IABluetoothStateListener() {
                override fun onBluetoothStateChanged(openOrClosed: Boolean) {
                    Log.d(TAG, "Bluetooth state changed: open=$openOrClosed")
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "registerBluetoothStateListener failed", e)
        }

        Log.d(TAG, "initialize: completed successfully")
    }

    private fun ensureInit() {
        if (!sdkInitialized) {
            Log.w(TAG, "ensureInit: flag not set; trusting VitaSleepApp.onCreate init")
            sdkInitialized = true
        }
    }

    fun hasBluetooth(): Boolean = true

    fun startScan() {
        if (_isScanning.value) return
        ensureInit()
        scannedMap.clear()
        _scannedDevices.value = emptyList()
        try {
            Log.d(TAG, "startScan: calling startScanDevice")
            vpOperateManager.startScanDevice(searchResponse)
        } catch (e: Throwable) {
            Log.e(TAG, "startScan failed", e)
            _isScanning.value = false
        }
    }

    fun stopScan() {
        if (!_isScanning.value) return
        try {
            vpOperateManager.stopScanDevice()
        } catch (e: Throwable) {
            Log.e(TAG, "stopScan failed", e)
        }
        _isScanning.value = false
    }

    fun connectDevice(mac: String, name: String = "Unknown") {
        Log.d(TAG, "connectDevice: mac=$mac name=$name")
        stopScan()
        _connectionState.value = ConnectionState.Connecting
        connectedMac = mac
        connectedName = name

        startRealtimeLogcat()

        ensureInit()

        try {
            Log.d(TAG, "connectDevice: calling vpOperateManager.connectDevice")
            vpOperateManager.connectDevice(mac, name, object : IConnectResponse {
                override fun connectState(code: Int, profile: BleGattProfile, isoadModel: Boolean) {
                    Log.d(TAG, "IConnectResponse.connectState: code=$code isoadModel=$isoadModel")
                    try {
                        if (code == Code.REQUEST_SUCCESS) {
                            isOadModel = isoadModel
                            Log.d(TAG, "connectState: SUCCESS")
                            // Register the connect-status listener ONLY after the GATT
                            // layer confirms the connection, so we don't interfere with
                            // the SDK's internal connection-state bookkeeping during setup
                            // (crash previously observed at internal setConnectStat).
                            try {
                                vpOperateManager.registerConnectStatusListener(mac, connectStatusListener)
                                Log.d(TAG, "connectState: registerConnectStatusListener OK")
                            } catch (e: Throwable) {
                                Log.e(TAG, "registerConnectStatusListener failed", e)
                            }
                            try {
                                VeepooService.start(context)
                            } catch (e: Throwable) {
                                Log.e(TAG, "VeepooService.start failed after connect success", e)
                            }
                        } else {
                            Log.e(TAG, "connectState: FAILED code=$code")
                            stopRealtimeLogcat()
                            _connectionState.value = ConnectionState.Error("???? (code=$code)")
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "IConnectResponse.connectState error", e)
                    }
                }
            }, object : INotifyResponse {
                override fun notifyState(state: Int) {
                    Log.d(TAG, "INotifyResponse.notifyState: state=$state")
                    try {
                        if (state == Code.REQUEST_SUCCESS) {
                            Log.d(TAG, "notifyState: SUCCESS, calling confirmDevicePwd")
                            _connectionState.value = ConnectionState.Confirming
                            confirmDevicePwd()
                        } else {
                            Log.e(TAG, "notifyState: FAILED state=$state")
                            stopRealtimeLogcat()
                            VeepooService.stop(context)
                            _connectionState.value = ConnectionState.Error("???????? (state=$state)")
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "INotifyResponse.notifyState error", e)
                        stopRealtimeLogcat()
                        VeepooService.stop(context)
                        _connectionState.value = ConnectionState.Error("??????: ${e.javaClass.simpleName}: ${e.message}")
                    }
                }
            })
            Log.d(TAG, "connectDevice: SDK call returned without exception")
        } catch (e: Throwable) {
            Log.e(TAG, "connectDevice FAILED", e)
            stopRealtimeLogcat()
            _connectionState.value = ConnectionState.Error("????: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun confirmDevicePwd() {
        Log.d(TAG, "confirmDevicePwd: start")
        try {
            vpOperateManager.confirmDevicePwd(
                object : IBleWriteResponse {
                    override fun onResponse(code: Int) {
                        Log.d(TAG, "confirmDevicePwd writeResponse: code=$code")
                    }
                },
                object : IPwdDataListener {
                    override fun onPwdDataChange(pwdData: PwdData) {
                        Log.d(TAG, "onPwdDataChange")
                    }
                    override fun onConnectionConfirmTimeout() {
                        Log.e(TAG, "onConnectionConfirmTimeout")
                        try {
                            stopRealtimeLogcat()
                            VeepooService.stop(context)
                            _connectionState.value = ConnectionState.Error("??????")
                        } catch (e: Throwable) {
                            Log.e(TAG, "onConnectionConfirmTimeout error", e)
                        }
                    }
                },
                object : IDeviceFuctionDataListener {
                    override fun onFunctionSupportDataChange(functionSupport: FunctionDeviceSupportData) {
                        Log.d(TAG, "onFunctionSupportDataChange")
                        try {
                            watchDataDay = functionSupport.wathcDay
                        } catch (e: Throwable) {
                            Log.e(TAG, "onFunctionSupportDataChange error", e)
                        }
                    }
                    override fun onDeviceFunctionPackage1Report(p1: DeviceFunctionPackage1) {
                        Log.d(TAG, "onDeviceFunctionPackage1Report")
                    }
                    override fun onDeviceFunctionPackage2Report(p2: DeviceFunctionPackage2) {
                        Log.d(TAG, "onDeviceFunctionPackage2Report")
                    }
                    override fun onDeviceFunctionPackage3Report(p3: DeviceFunctionPackage3) {
                        Log.d(TAG, "onDeviceFunctionPackage3Report")
                    }
                    override fun onDeviceFunctionPackage4Report(p4: DeviceFunctionPackage4) {
                        Log.d(TAG, "onDeviceFunctionPackage4Report")
                    }
                    override fun onDeviceFunctionPackage5Report(p5: DeviceFunctionPackage5) {
                        Log.d(TAG, "onDeviceFunctionPackage5Report")
                    }
                },
                object : ISocialMsgDataListener {
                    override fun onSocialMsgSupportDataChange(data: FunctionSocailMsgData) {
                        Log.d(TAG, "onSocialMsgSupportDataChange")
                    }
                    override fun onSocialMsgSupportDataChange2(data: FunctionSocailMsgData) {
                        Log.d(TAG, "onSocialMsgSupportDataChange2")
                    }
                },
                object : ICustomSettingDataListener {
                    override fun OnSettingDataChange(customSettingData: CustomSettingData) {
                        Log.d(TAG, "OnSettingDataChange: CONNECTED!")
                        try {
                            stopRealtimeLogcat()
                            _connectionState.value = ConnectionState.Connected(
                                mac = connectedMac ?: "",
                                name = connectedName ?: ""
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "OnSettingDataChange error", e)
                        }
                    }
                },
                "0000",
                true
            )
            Log.d(TAG, "confirmDevicePwd: SDK call returned without exception")
        } catch (e: Throwable) {
            Log.e(TAG, "confirmDevicePwd FAILED", e)
            stopRealtimeLogcat()
            VeepooService.stop(context)
            _connectionState.value = ConnectionState.Error("??????: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun disconnect() {
        stopRealtimeLogcat()
        try {
            vpOperateManager.disconnectWatch(defaultWriteResponse)
        } catch (e: Throwable) {
            Log.e(TAG, "disconnectWatch failed", e)
        }
        try {
            connectedMac?.let { vpOperateManager.unregisterConnectStatusListener(it, connectStatusListener) }
        } catch (e: Throwable) {
            Log.e(TAG, "unregisterConnectStatusListener failed", e)
        }
        VeepooService.stop(context)
        _connectionState.value = ConnectionState.Disconnected
        _deviceBattery.value = null
    }

    fun readBattery() {
        try {
            vpOperateManager.readBattery(object : IBleWriteResponse {
                override fun onResponse(code: Int) {}
            }, object : IBatteryDataListener {
                override fun onDataChange(batteryData: BatteryData) {
                    try {
                        val percent = batteryData.batteryPercent
                        _deviceBattery.value = if (percent > 0) percent else batteryData.batteryLevel * 25
                    } catch (e: Throwable) {
                        Log.e(TAG, "onBatteryDataChange error", e)
                    }
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "readBattery failed", e)
        }
    }

    fun readAllOriginData() {
        _latestOriginData.value = emptyList()
        _originDataProgress.value = 0f
        _originDataReading.value = true
        val collectedOriginData = mutableListOf<Map<String, Any>>()

        try {
            vpOperateManager.readOriginData(
                object : IBleWriteResponse {
                    override fun onResponse(code: Int) {}
                },
                object : IOriginData3Listener {
                    override fun onOriginFiveMinuteListDataChange(originDataList: MutableList<OriginData3>) {
                        try {
                            for (originData in originDataList) {
                                val timeData = originData.getmTime()
                                val timestamp = String.format(
                                    Locale.getDefault(),
                                    "%04d-%02d-%02dT%02d:%02d:00",
                                    timeData.year, timeData.month, timeData.day,
                                    timeData.hour, timeData.minute
                                )
                                val record = mapOf<String, Any>(
                                    "timestamp" to timestamp,
                                    "heart_rate" to originData.rateValue,
                                    "systolic" to originData.highValue,
                                    "diastolic" to originData.lowValue,
                                    "steps" to originData.stepValue,
                                    "sport" to originData.sportValue
                                )
                                collectedOriginData.add(record)
                            }
                            _latestOriginData.value = collectedOriginData.toList()
                        } catch (e: Throwable) {
                            Log.e(TAG, "onOriginFiveMinuteListDataChange error", e)
                        }
                    }

                    override fun onOriginHalfHourDataChange(originHalfHourData: OriginHalfHourData) {}

                    override fun onOriginHRVOriginListDataChange(originHrvDataList: MutableList<HRVOriginData>) {}

                    override fun onOriginSpo2OriginListDataChange(originSpo2hDataList: MutableList<Spo2hOriginData>) {}

                    override fun onReadOriginProgress(progress: Float) {
                        _originDataProgress.value = progress
                    }

                    override fun onReadOriginProgressDetail(day: Int, date: String, allPackage: Int, currentPackage: Int) {}

                    override fun onReadOriginComplete() {
                        _originDataProgress.value = 1f
                        _originDataReading.value = false
                    }
                },
                watchDataDay
            )
        } catch (e: Throwable) {
            Log.e(TAG, "readAllOriginData failed", e)
            _originDataReading.value = false
        }
    }

    fun readSleepData() {
        _latestSleepData.value = null

        try {
            vpOperateManager.readSleepData(
                object : IBleWriteResponse {
                    override fun onResponse(code: Int) {}
                },
                object : ISleepDataListener {
                    override fun onSleepDataChange(day: String, sleepData: SleepData) {
                        try {
                            val sleepDown = sleepData.sleepDown
                            val sleepUp = sleepData.sleepUp

                            val sleepStart = if (sleepDown != null) {
                                String.format(
                                    Locale.getDefault(),
                                    "%04d-%02d-%02dT%02d:%02d:00",
                                    sleepDown.year, sleepDown.month, sleepDown.day,
                                    sleepDown.hour, sleepDown.minute
                                )
                            } else ""

                            val sleepEnd = if (sleepUp != null) {
                                String.format(
                                    Locale.getDefault(),
                                    "%04d-%02d-%02dT%02d:%02d:00",
                                    sleepUp.year, sleepUp.month, sleepUp.day,
                                    sleepUp.hour, sleepUp.minute
                                )
                            } else ""

                            val totalMin = sleepData.allSleepTime
                            val deepMin = sleepData.deepSleepTime
                            val lightMin = sleepData.lowSleepTime
                            val awakeMin = totalMin - deepMin - lightMin

                            val result = mapOf<String, Any>(
                                "sleep_date" to day,
                                "sleep_start" to sleepStart,
                                "sleep_end" to sleepEnd,
                                "total_sleep_min" to totalMin,
                                "deep_sleep_min" to deepMin,
                                "light_sleep_min" to lightMin,
                                "rem_sleep_min" to 0,
                                "awake_min" to awakeMin
                            )
                            _latestSleepData.value = result
                        } catch (e: Throwable) {
                            Log.e(TAG, "onSleepDataChange error", e)
                        }
                    }

                    override fun onSleepProgress(progress: Float) {}
                    override fun onSleepProgressDetail(day: String, packagenumber: Int) {}
                    override fun onReadSleepComplete() {}
                },
                watchDataDay
            )
        } catch (e: Throwable) {
            Log.e(TAG, "readSleepData failed", e)
        }
    }

    fun convertOriginDataToUploadFormat(
        data: List<Map<String, Any>>
    ): List<VeepooOriginRecord> {
        return data.mapNotNull { record ->
            try {
                VeepooOriginRecord(
                    timestamp = record["timestamp"] as? String ?: return@mapNotNull null,
                    heartRate = (record["heart_rate"] as? Number)?.toInt()?.takeIf { it > 0 },
                    systolic = (record["systolic"] as? Number)?.toInt()?.takeIf { it > 0 },
                    diastolic = (record["diastolic"] as? Number)?.toInt()?.takeIf { it > 0 },
                    steps = (record["steps"] as? Number)?.toInt() ?: 0
                )
            } catch (e: Throwable) {
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

    private fun startRealtimeLogcat() {
        try {
            stopRealtimeLogcat()
            val pid = android.os.Process.myPid()
            val file = File(context.filesDir, "realtime_logcat.txt")
            file.delete()
            logcatProcess = Runtime.getRuntime().exec(
                arrayOf("logcat", "-v", "threadtime", "--pid=$pid")
            )
            Thread {
                try {
                    val reader = BufferedReader(InputStreamReader(logcatProcess?.inputStream))
                    val writer = file.bufferedWriter()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        writer.write(line)
                        writer.newLine()
                        writer.flush()
                    }
                    writer.close()
                } catch (_: Throwable) {}
            }.start()
            Log.d(TAG, "startRealtimeLogcat: started for PID=$pid")
        } catch (e: Throwable) {
            Log.e(TAG, "startRealtimeLogcat failed", e)
        }
    }

    private fun stopRealtimeLogcat() {
        try {
            logcatProcess?.destroy()
            logcatProcess = null
        } catch (_: Throwable) {}
    }

    companion object {
        private const val TAG = "VeepooManager"
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
