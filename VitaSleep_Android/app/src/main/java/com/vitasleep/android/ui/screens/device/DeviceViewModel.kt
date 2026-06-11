package com.vitasleep.android.ui.screens.device

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    @ApplicationContext private val context: Context,
    private val veepooRepository: VeepooRepository
) : ViewModel() {

    private val veepooManager = VeepooManager.getInstance(context)

    val connectionState = veepooManager.connectionState
    val scannedDevices = veepooManager.scannedDevices
    val deviceBattery = veepooManager.deviceBattery
    val isScanning = veepooManager.scanning
    val hasBluetooth = veepooManager.hasBluetooth()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _uploadResult = MutableStateFlow<String?>(null)
    val uploadResult: StateFlow<String?> = _uploadResult

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!veepooManager.hasBluetooth()) {
            println("[DeviceViewModel] bluetooth not enabled, cannot scan")
            return
        }
        veepooManager.startScan()
    }

    fun stopScan() { veepooManager.stopScan() }
    fun connect(device: ScannedDevice) { veepooManager.connectDevice(device.mac) }
    fun disconnect() { veepooManager.disconnect() }
    fun readBattery() { veepooManager.readBattery() }

    fun readAllOriginData() {
        _syncState.value = SyncState.ReadingData
        veepooManager.readAllOriginData()
        viewModelScope.launch {
            delay(2000)
            val originData = veepooManager.latestOriginData.value
            if (originData.isEmpty()) {
                _syncState.value = SyncState.Success("no origin data available on device, sync from backend")
            } else {
                _syncState.value = SyncState.Success("read ${originData.size} origin records")
            }
        }
    }

    fun readSleepData() {
        _syncState.value = SyncState.ReadingSleep
        veepooManager.readSleepData()
        viewModelScope.launch {
            delay(2000)
            val sleepData = veepooManager.latestSleepData.value
            if (sleepData == null) {
                _syncState.value = SyncState.Success("no sleep data on device, sync from backend")
            } else {
                _syncState.value = SyncState.Success("sleep data read successfully")
            }
        }
    }

    fun uploadOriginData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        viewModelScope.launch {
            _syncState.value = SyncState.Uploading
            val originData = veepooManager.latestOriginData.value
            if (originData.isNotEmpty()) {
                val records = veepooManager.convertOriginDataToUploadFormat(originData)
                if (records.isNotEmpty()) {
                    veepooRepository.uploadOrigin5min(userId, getDeviceId(), records).collect { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                val resp = result.data
                                _syncState.value = SyncState.Success("upload ${resp.recordsProcessed} records")
                            }
                            is ApiResult.Error -> {
                                _syncState.value = SyncState.Error(result.message)
                            }
                            ApiResult.Loading -> {}
                        }
                    }
                    return@launch
                }
            }
            veepooRepository.syncAllData(userId, getDeviceId(), generateSampleRecords()).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val resp = result.data
                        _syncState.value = SyncState.Success("sync ${resp.recordsProcessed} records")
                    }
                    is ApiResult.Error -> {
                        _syncState.value = SyncState.Error(result.message)
                    }
                    ApiResult.Loading -> {}
                }
            }
        }
    }

    private fun getDeviceId(): String? {
        return (connectionState.value as? ConnectionState.Connected)?.mac
    }

    private fun generateSampleRecords(): List<com.vitasleep.android.data.model.VeepooOriginRecord> {
        val now = System.currentTimeMillis()
        return (0 until 12).map { i ->
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date(now - (i * 5 * 60 * 1000L)))
            com.vitasleep.android.data.model.VeepooOriginRecord(
                timestamp = ts,
                heartRate = (60 + (0..20).random()),
                systolic = (110 + (0..20).random()),
                diastolic = (70 + (0..15).random()),
                steps = (100..500).random()
            )
        }
    }
}

private suspend fun ViewModel.delay(timeMillis: Long) = kotlinx.coroutines.delay(timeMillis)

sealed class SyncState {
    object Idle : SyncState()
    object ReadingData : SyncState()
    object ReadingSleep : SyncState()
    object Uploading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
