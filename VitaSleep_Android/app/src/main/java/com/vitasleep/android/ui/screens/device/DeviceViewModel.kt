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
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val veepooRepository: VeepooRepository
) : ViewModel() {

    private val veepooManager = VeepooManager.getInstance(context)

    val connectionState = veepooManager.connectionState
    val scannedDevices = veepooManager.scannedDevices
    val deviceBattery = veepooManager.deviceBattery
    val latestOriginData = veepooManager.latestOriginData
    val isScanning = veepooManager.isScanning
    val originDataProgress = veepooManager.originDataProgress
    val originDataReading = veepooManager.originDataReading

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _uploadResult = MutableStateFlow<String?>(null)
    val uploadResult: StateFlow<String?> = _uploadResult

    fun startScan() {
        veepooManager.startScan()
    }

    fun stopScan() {
        veepooManager.stopScan()
    }

    fun connect(device: ScannedDevice) {
        veepooManager.connectDevice(device.mac, device.name)
    }

    fun disconnect() {
        veepooManager.disconnect()
    }

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

    fun uploadOriginData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        val originData = veepooManager.latestOriginData.value
        if (originData.isEmpty()) {
            _uploadResult.value = "没有可上传的数据"
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
                            "已上传 ${result.data.recordsProcessed} 条数据，生成 ${result.data.metricIds.size} 条指标"
                        )
                        _uploadResult.value = result.data.message
                    }
                    is ApiResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                        _uploadResult.value = "上传失败: ${result.message}"
                    }
                }
            }
        }
    }

    fun uploadSleepData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        val sleepData = veepooManager.latestSleepData.value ?: run {
            _uploadResult.value = "没有可上传的睡眠数据"
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
                        _syncState.value = SyncState.Success("睡眠数据上传成功")
                        _uploadResult.value = result.data.message
                    }
                    is ApiResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                        _uploadResult.value = "上传失败: ${result.message}"
                    }
                }
            }
        }
    }

    fun syncAllData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        viewModelScope.launch {
            _syncState.value = SyncState.ReadingData
            veepooManager.readAllOriginData()

            try {
                veepooManager.originDataReading
                    .filter { !it }
                    .take(1)
                    .collect {
                        val originData = veepooManager.latestOriginData.value
                        if (originData.isEmpty()) {
                            _syncState.value = SyncState.Error("未读取到设备数据，请确认设备已连接")
                            return@collect
                        }

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
                                        "全量同步完成: ${result.data.recordsProcessed} 条数据"
                                    )
                                }
                                is ApiResult.Error -> {
                                    _syncState.value = SyncState.Error(result.message)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "同步失败")
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
