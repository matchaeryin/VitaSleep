package com.vitasleep.android.ui.screens.device

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.ConnectionState
import com.vitasleep.android.veepoo.ScannedDevice

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "设备管理",
            style = MaterialTheme.typography.headlineMedium,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "状态: ${when (connectionState) {
                        is ConnectionState.Connected -> "已连接"
                        is ConnectionState.Connecting -> "连接中"
                        is ConnectionState.Error -> "错误"
                        else -> "未连接"
                    }}",
                    color = OnSurface
                )
                if (connectionState is ConnectionState.Connected) {
                    Button(
                        onClick = { viewModel.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = Error)
                    ) {
                        Text("断开")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (connectionState) {
            is ConnectionState.Disconnected -> {
                if (!viewModel.hasBluetooth) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BluetoothDisabled,
                                null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("蓝牙未开启", color = OnSurface)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onRequestEnableBluetooth) {
                                Text("开启蓝牙")
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val hasPermission = checkBlePermissions(context)
                                if (hasPermission) {
                                    viewModel.startScan()
                                } else {
                                    onRequestPermissions()
                                }
                            },
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Icon(Icons.Default.Search, null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (isScanning) "扫描中..." else "扫描设备")
                        }
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isScanning && scannedDevices.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Primary, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("正在扫描附近的蓝牙设备...", color = OnSurfaceVariant)
                            }
                        }
                    } else if (!isScanning && scannedDevices.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("点击扫描按钮搜索附近的蓝牙设备", color = OnSurfaceVariant)
                            }
                        }
                    }

                    if (!isScanning && scannedDevices.isNotEmpty()) {
                        Text(
                            "发现 ${scannedDevices.size} 个设备",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    scannedDevices.forEach { device ->
                        DeviceItem(device, onClick = { viewModel.connect(device) })
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            is ConnectionState.Connected -> {
                val mac = (connectionState as ConnectionState.Connected).mac
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BluetoothConnected, null, tint = Success, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("已连接设备", color = OnSurface)
                            Text(mac, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.readAllOriginData() },
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) { Text("读取健康数据") }
                    Button(
                        onClick = { viewModel.uploadOriginData() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("上传数据") }
                }

                when (syncState) {
                    is SyncState.ReadingData -> {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator(color = Primary)
                    }
                    is SyncState.Uploading -> {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator(color = Primary)
                    }
                    is SyncState.Success -> {
                        Spacer(Modifier.height(12.dp))
                        Text("操作成功", color = Success)
                    }
                    is SyncState.Error -> {
                        Spacer(Modifier.height(12.dp))
                        Text("操作失败", color = Error)
                    }
                    else -> {}
                }
            }
            is ConnectionState.Connecting -> {
                CircularProgressIndicator(color = Primary)
                Spacer(Modifier.height(8.dp))
                Text("正在连接...", color = OnSurfaceVariant)
            }
            is ConnectionState.Error -> {
                Text("连接错误: ${(connectionState as ConnectionState.Error).message}", color = Error)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("重新扫描")
                }
            }
        }
    }
}

private fun checkBlePermissions(context: android.content.Context): Boolean {
    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun DeviceItem(device: ScannedDevice, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Watch, null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(device.name, color = OnSurface)
                    Text(
                        device.mac,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            Text(
                "${device.rssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}
