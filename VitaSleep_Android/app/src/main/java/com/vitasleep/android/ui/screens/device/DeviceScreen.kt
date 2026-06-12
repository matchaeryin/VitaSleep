package com.vitasleep.android.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.ui.theme.*
import com.vitasleep.android.veepoo.ConnectionState
import com.vitasleep.android.veepoo.ScannedDevice

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = hiltViewModel(),
    onRequestPermissions: () -> Unit = {},
    onRequestEnableBluetooth: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val deviceBattery by viewModel.deviceBattery.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val uploadResult by viewModel.uploadResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 椤甸潰鏍囬
        Text(
            text = "璁惧绠＄悊",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 鈹€鈹€鈹€ 杩炴帴鐘舵€佸崱鐗?鈹€鈹€鈹€
        ConnectionStatusCard(
            connectionState = connectionState,
            battery = deviceBattery,
            onDisconnect = { viewModel.disconnect() },
            onReadBattery = { viewModel.readBattery() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 鈹€鈹€鈹€ 璁惧鎵弿鍖哄煙 鈹€鈹€鈹€
        when (connectionState) {
            is ConnectionState.Disconnected,
            is ConnectionState.Error -> {
                DeviceScanSection(
                    scannedDevices = scannedDevices,
                    isScanning = isScanning,
                    onScan = { viewModel.startScan() },
                    onStopScan = { viewModel.stopScan() },
                    onConnect = { viewModel.connect(it) },
                    onRequestPermissions = onRequestPermissions
                )
            }
            is ConnectionState.Connected -> {
                DataSyncSection(
                    syncState = syncState,
                    onReadData = { viewModel.readAllOriginData() },
                    onReadSleep = { viewModel.readSleepData() },
                    onUploadData = { viewModel.uploadOriginData() },
                    onUploadSleep = { viewModel.uploadSleepData() },
                    onSyncAll = { viewModel.syncAllData() }
                )
            }
            is ConnectionState.Connecting -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("杩炴帴涓€?, color = OnSurface)
                }
            }
        }

        // 鈹€鈹€鈹€ 涓婁紶缁撴灉鎻愮ず 鈹€鈹€鈹€
        uploadResult?.let { result ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(result, color = OnSurface, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    battery: Int?,
    onDisconnect: () -> Unit,
    onReadBattery: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                is ConnectionState.Connected -> Success
                                is ConnectionState.Error -> Error
                                else -> OnSurfaceVariant
                            }
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> "宸茶繛鎺?
                            is ConnectionState.Connecting -> "杩炴帴涓€?
                            is ConnectionState.Error -> "杩炴帴澶辫触"
                            is ConnectionState.Disconnected -> "鏈繛鎺?
                        },
                        fontWeight = FontWeight.Medium,
                        color = OnSurface
                    )
                    if (connectionState is ConnectionState.Connected) {
                        Text(
                            text = connectionState.mac,
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                    if (connectionState is ConnectionState.Error) {
                        Text(
                            text = connectionState.message,
                            fontSize = 12.sp,
                            color = Error
                        )
                    }
                }
            }

            Row {
                if (connectionState is ConnectionState.Connected) {
                    battery?.let {
                        Text(
                            text = "鐢甸噺: $it%",
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = onReadBattery) {
                        Icon(Icons.Default.Refresh, contentDescription = "鍒锋柊鐢甸噺", tint = Primary)
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.Close, contentDescription = "鏂紑杩炴帴", tint = Error)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceScanSection(
    scannedDevices: List<ScannedDevice>,
    isScanning: Boolean,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (ScannedDevice) -> Unit,
    onRequestPermissions: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("鍙戠幇璁惧", fontWeight = FontWeight.Medium, color = OnSurface)
                    if (isScanning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("鎵弿涓?..", fontSize = 12.sp, color = Primary)
                    }
                }
                if (isScanning) {
                    OutlinedButton(
                        onClick = onStopScan,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Text("鍋滄", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onScan,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("鎵弿")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (scannedDevices.isEmpty()) {
                Text(
                    if (isScanning) "姝ｅ湪鎼滅储闄勮繎钃濈墮璁惧..." else "鐐瑰嚮鎵弿鎸夐挳鎼滅储 Veepoo 璁惧",
                    color = OnSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                scannedDevices.forEach { device ->
                    DeviceItem(device = device, onClick = { onConnect(device) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun DeviceItem(device: ScannedDevice, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Watch,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(device.name, fontWeight = FontWeight.Medium, color = OnSurface)
                    Text(device.mac, fontSize = 11.sp, color = OnSurfaceVariant)
                }
            }
            Text(
                "${device.rssi} dBm",
                fontSize = 12.sp,
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
fun DataSyncSection(
    syncState: SyncState,
    onReadData: () -> Unit,
    onReadSleep: () -> Unit,
    onUploadData: () -> Unit,
    onUploadSleep: () -> Unit,
    onSyncAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("鏁版嵁鍚屾", fontWeight = FontWeight.Medium, color = OnSurface)
            Spacer(modifier = Modifier.height(12.dp))

            // 鍚屾鐘舵€?            when (syncState) {
                is SyncState.ReadingData -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("姝ｅ湪璇诲彇璁惧鏁版嵁鈥?, color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
                is SyncState.ReadingSleep -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("姝ｅ湪璇诲彇鐫＄湢鏁版嵁鈥?, color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
                is SyncState.Uploading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("姝ｅ湪涓婁紶鏁版嵁鈥?, color = OnSurfaceVariant, fontSize = 13.sp)
                    }
                }
                is SyncState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(syncState.message, color = Success, fontSize = 13.sp)
                    }
                }
                is SyncState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(syncState.message, color = Error, fontSize = 13.sp)
                    }
                }
                SyncState.Idle -> {
                    Text(
                        "鐐瑰嚮涓嬫柟鎸夐挳浠庤澶囪鍙栨暟鎹苟涓婁紶鍒版湇鍔″櫒",
                        color = OnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 鎿嶄綔鎸夐挳
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReadData,
                    modifier = Modifier.weight(1f),
                    enabled = syncState == SyncState.Idle
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("璇诲彇鍋ュ悍", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onReadSleep,
                    modifier = Modifier.weight(1f),
                    enabled = syncState == SyncState.Idle
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("璇诲彇鐫＄湢", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUploadData,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    enabled = syncState == SyncState.Idle
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("涓婁紶鍋ュ悍", fontSize = 12.sp)
                }
                Button(
                    onClick = onUploadSleep,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    enabled = syncState == SyncState.Idle
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("涓婁紶鐫＄湢", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSyncAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = syncState == SyncState.Idle
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("涓€閿叏閲忓悓姝ワ紙璇诲彇 + 涓婁紶锛?)
            }
        }
    }
}
