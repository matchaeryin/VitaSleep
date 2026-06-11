package com.vitasleep.android.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.ConnectionState
import com.vitasleep.android.veepoo.ScannedDevice

@Composable
fun DeviceScreen(viewModel: DeviceViewModel = hiltViewModel(), onRequestPermissions: () -> Unit) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("设备管理", style = MaterialTheme.typography.headlineMedium, color = OnBackground)
        Spacer(modifier = Modifier.height(16.dp))

        // 连接状态
        Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("状态: ${when (connectionState) { is ConnectionState.Connected -> "已连接"; is ConnectionState.Connecting -> "连接中"; else -> "未连接" }}", color = OnSurface)
                if (connectionState is ConnectionState.Connected) Button(onClick = { viewModel.disconnect() }, colors = ButtonDefaults.buttonColors(containerColor = Error)) { Text("断开") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 扫描/连接
        when (connectionState) {
            is ConnectionState.Disconnected -> {
                Button(onClick = { viewModel.startScan() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(4.dp)); Text("扫描设备") }
                Spacer(modifier = Modifier.height(8.dp))
                scannedDevices.forEach { device -> DeviceItem(device, onClick = { viewModel.connect(device) }) }
            }
            is ConnectionState.Connected -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.readAllOriginData() }, colors = ButtonDefaults.buttonColors(containerColor = Secondary)) { Text("读取健康数据") }
                    Button(onClick = { viewModel.uploadOriginData() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("上传数据") }
                }
            }
            else -> CircularProgressIndicator(color = Primary)
        }
    }
}

@Composable
fun DeviceItem(device: ScannedDevice, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp).clickable(onClick = onClick), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Watch, null, tint = Primary); Spacer(Modifier.width(12.dp)); Column { Text(device.name, color = OnSurface); Text(device.mac, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) } }
            Text("${device.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
    }
}
