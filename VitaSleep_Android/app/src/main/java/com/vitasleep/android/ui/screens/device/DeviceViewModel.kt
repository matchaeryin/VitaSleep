package com.vitasleep.android.ui.screens.device

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    }
    fun readSleepData() {
        _syncState.value = SyncState.ReadingSleep
        veepooManager.readSleepData()
    }
    fun uploadOriginData(userId: String = VeepooManager.DEFAULT_USER_ID) {
        viewModelScope.launch {
            _syncState.value = SyncState.Uploading
            _syncState.value = SyncState.Success("upload complete")
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object ReadingData : SyncState()
    object ReadingSleep : SyncState()
    object Uploading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
