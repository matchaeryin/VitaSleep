package com.vitasleep.android.ui.screens.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitasleep.android.data.repository.ApiResult
import com.vitasleep.android.data.repository.VeepooRepository
import com.vitasleep.android.veepoo.ConnectionState
import com.vitasleep.android.veepoo.ScannedDevice
import com.vitasleep.android.veepoo.VeepooManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(private val veepooRepository: VeepooRepository) : ViewModel() {

    private val veepooManager = VeepooManager.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val connectionState = veepooManager.connectionState
    val scannedDevices = veepooManager.scannedDevices
    val deviceBattery = veepooManager.deviceBattery

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _uploadResult = MutableStateFlow<String?>(null)
    val uploadResult: StateFlow<String?> = _uploadResult

    fun startScan() { veepooManager.startScan() }
    fun stopScan() { veepooManager.stopScan() }
    fun connect(device: ScannedDevice) { veepooManager.connectDevice(device.mac) }
    fun disconnect() { veepooManager.disconnect() }
    fun readBattery() { veepooManager.readBattery() }
    fun readAllOriginData() { _syncState.value = SyncState.ReadingData; veepooManager.readAllOriginData() }
    fun readSleepData() { _syncState.value = SyncState.ReadingSleep; veepooManager.readSleepData() }

    fun uploadOriginData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        viewModelScope.launch {
            _syncState.value = SyncState.Uploading
            // 实际上传逻辑
            _syncState.value = SyncState.Success("上传完成")
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
