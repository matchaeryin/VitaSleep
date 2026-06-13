package com.vitasleep.android.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vitasleep.android.VitaSleepApp
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

    val context = LocalContext.current
    val app = context.applicationContext as VitaSleepApp
    var crashLog by remember { mutableStateOf(app.getLastCrashLog()) }
    var crashExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设备管理",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        crashLog?.let { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { crashExpanded = !crashExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "检测到上次崩溃日志 (点击${if (crashExpanded) "收起" else "展开"})",
                                color = Error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row {
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("crash_log", log))
                            }) {
                                Text("复制", fontSize = 12.sp, color = Primary)
                            }
                            TextButton(onClick = {
                                app.clearCrashLog()
                                crashLog = null
                            }) {
                                Text("清除", fontSize = 12.sp, color = OnSurfaceVariant)
                            }
                        }
                    }
                    if (crashExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = log,
                            color = Color(0xFFB71C1C),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            softWrap = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 360.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── 连接状态卡片 ───
        ConnectionStatusCard(
            connectionState = connectionState,
            battery = deviceBattery,
            onDisconnect = { viewModel.disconnect() },
            onReadBattery = { viewModel.readBattery() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ─── 设备扫描区域 ───
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
            is ConnectionState.Connecting,
            is ConnectionState.Confirming -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (connectionState is ConnectionState.Confirming) "验证设备中…" else "连接中…",
                        color = OnSurface
                    )
                }
            }
        }

        // ─── 上传结果提示 ───
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
                            is ConnectionState.Connected -> "已连接"
                            is ConnectionState.Connecting -> "连接中…"
                            is ConnectionState.Confirming -> "验证设备中…"
                            is ConnectionState.Error -> "连接失败"
                            is ConnectionState.Disconnected -> "未连接"
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
                            text = "电量: $it%",
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = onReadBattery) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新电量", tint = Primary)
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.Default.Close, contentDescription = "断开连接", tint = Error)
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
                    Text("发现设备", fontWeight = FontWeight.Medium, color = OnSurface)
                    if (isScanning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("扫描中...", fontSize = 12.sp, color = Primary)
                    }
                }
                if (isScanning) {
                    OutlinedButton(
                        onClick = onStopScan,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Text("停止", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onScan,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("扫描")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (scannedDevices.isEmpty()) {
                Text(
                    if (isScanning) "正在搜索附近蓝牙设备..." else "点击扫描按钮搜索 Veepoo 设备",
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
            Text("数据同步", fontWeight = FontWeight.Medium, color = OnSurface)
            Spacer(modifier = Modifier.height(12.dp))

            // 同步状态
            when (syncState) {
                is SyncState.ReadingData -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在读取设备数据…", color = OnSurfaceVariant, fontSize = 13.sp)
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
                        Text("正在读取睡眠数据…", color = OnSurfaceVariant, fontSize = 13.sp)
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
                        Text("正在上传数据…", color = OnSurfaceVariant, fontSize = 13.sp)
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
                        "点击下方按钮从设备读取数据并上传到服务器",
                        color = OnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 操作按钮
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
                    Text("读取健康", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onReadSleep,
                    modifier = Modifier.weight(1f),
                    enabled = syncState == SyncState.Idle
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("读取睡眠", fontSize = 12.sp)
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
                    Text("上传健康", fontSize = 12.sp)
                }
                Button(
                    onClick = onUploadSleep,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                    enabled = syncState == SyncState.Idle
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("上传睡眠", fontSize = 12.sp)
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
                Text("一键全量同步（读取 + 上传）")
            }
        }
    }
}
