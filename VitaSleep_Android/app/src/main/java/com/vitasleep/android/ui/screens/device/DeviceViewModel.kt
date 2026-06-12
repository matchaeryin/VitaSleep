package com.vitasleep.android.ui.screens.device

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.model.VeepooOriginRecord
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.VeepooRepository
import com.vitasleep.android.veepoo.ConnectionState
import com.vitasleep.android.veepoo.ScannedDevice
import com.vitasleep.android.veepoo.VeepooManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val veepooRepository: VeepooRepository
) : ViewModel() {

    private val veepooManager = VeepooManager.getInstance(context)

    // UI 鐘舵€?    val connectionState = veepooManager.connectionState
    val scannedDevices = veepooManager.scannedDevices
    val deviceBattery = veepooManager.deviceBattery
    val latestOriginData = veepooManager.latestOriginData
    val isScanning = veepooManager.isScanning

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _uploadResult = MutableStateFlow<String?>(null)
    val uploadResult: StateFlow<String?> = _uploadResult

    // 鈹€鈹€鈹€ 鎵弿 鈹€鈹€鈹€

    fun startScan() {
        veepooManager.startScan()
    }

    fun stopScan() {
        veepooManager.stopScan()
    }

    // 鈹€鈹€鈹€ 杩炴帴 鈹€鈹€鈹€

    fun connect(device: ScannedDevice) {
        veepooManager.connectDevice(device.mac)
    }

    fun disconnect() {
        veepooManager.disconnect()
    }

    // 鈹€鈹€鈹€ 璇诲彇鏁版嵁 鈹€鈹€鈹€

    fun readBattery() {
        veepooManager.readBattery()
    }

    fun readAllOriginData() {
        _syncState.value = SyncState.ReadingData
        veepooManager.readAllOriginData()
    }

    fun readSleepData() {
        _syncState.value = SyncState.ReadingSleep
        veepooManager.readSleepData()
    }

    // 鈹€鈹€鈹€ 涓婁紶鏁版嵁 鈹€鈹€鈹€

    fun uploadOriginData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        val originData = veepooManager.latestOriginData.value
        if (originData.isEmpty()) {
            _uploadResult.value = "娌℃湁鍙笂浼犵殑鏁版嵁"
            return
        }

        viewModelScope.launch {
            val records = veepooManager.convertOriginDataToUploadFormat(originData)
            veepooRepository.uploadOrigin5min(
                userId = userId,
                deviceId = veepooManager.connectionState.value.let { state ->
                    if (state is ConnectionState.Connected) state.mac else null
                },
                records = records
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _syncState.value = SyncState.Uploading
                    }
                    is ApiResult.Success -> {
                        _syncState.value = SyncState.Success(
                            "宸蹭笂浼?${result.data.recordsProcessed} 鏉℃暟鎹紝鐢熸垚 ${result.data.metricIds.size} 鏉℃寚鏍?
                        )
                        _uploadResult.value = result.data.message
                    }
                    is ApiResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                        _uploadResult.value = "涓婁紶澶辫触: ${result.message}"
                    }
                }
            }
        }
    }

    fun uploadSleepData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        val sleepData = veepooManager.latestSleepData.value ?: run {
            _uploadResult.value = "娌℃湁鍙笂浼犵殑鐫＄湢鏁版嵁"
            return
        }

        viewModelScope.launch {
            val request = veepooManager.convertSleepDataToUploadFormat(sleepData)
            veepooRepository.uploadSleep(
                userId = userId,
                deviceId = veepooManager.connectionState.value.let { state ->
                    if (state is ConnectionState.Connected) state.mac else null
                },
                request = request
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _syncState.value = SyncState.Uploading
                    }
                    is ApiResult.Success -> {
                        _syncState.value = SyncState.Success("鐫＄湢鏁版嵁涓婁紶鎴愬姛")
                        _uploadResult.value = result.data.message
                    }
                    is ApiResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                        _uploadResult.value = "涓婁紶澶辫触: ${result.message}"
                    }
                }
            }
        }
    }

    // 鈹€鈹€鈹€ 鍏ㄩ噺鍚屾锛氳鍙?+ 涓婁紶 鈹€鈹€鈹€

    fun syncAllData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        viewModelScope.launch {
            _syncState.value = SyncState.ReadingData
            veepooManager.readAllOriginData()

            try {
                withTimeoutOrNull(5000L) {
                    veepooManager.latestOriginData
                        .filter { it.isNotEmpty() }
                        .take(1)
                        .collect { originData ->
                            val records = veepooManager.convertOriginDataToUploadFormat(originData)
                            _syncState.value = SyncState.Uploading

                            veepooRepository.syncAllData(
                                userId = userId,
                                deviceId = (veepooManager.connectionState.value as? ConnectionState.Connected)?.mac,
                                records = records
                            ).collect { result ->
                                when (result) {
                                    is ApiResult.Loading -> {}
                                    is ApiResult.Success -> {
                                        _syncState.value = SyncState.Success(
                                            "鍏ㄩ噺鍚屾瀹屾垚: ${result.data.recordsProcessed} 鏉℃暟鎹?
                                        )
                                    }
                                    is ApiResult.Error -> {
                                        _syncState.value = SyncState.Error(result.message)
                                    }
                                }
                            }
                        }
                } ?: run {
                    _syncState.value = SyncState.Error("鏈鍙栧埌璁惧鏁版嵁锛岃纭璁惧宸茶繛鎺?)
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "鍚屾澶辫触")
            }
        }
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object ReadingData : SyncState()
    data object ReadingSleep : SyncState()
    data object Uploading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
