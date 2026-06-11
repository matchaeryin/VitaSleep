package com.vitasleep.android.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vitasleep.android.ui.navigation.Screen
import com.vitasleep.android.ui.navigation.bottomNavItems
import com.vitasleep.android.ui.screens.chat.ChatScreen
import com.vitasleep.android.ui.screens.device.DeviceScreen
import com.vitasleep.android.ui.screens.health.HealthScreen
import com.vitasleep.android.ui.screens.schedule.ScheduleScreen
import com.vitasleep.android.ui.screens.sleep.SleepScreen
import com.vitasleep.android.ui.theme.VitaSleepTheme
import com.vitasleep.android.veepoo.VeepooManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val veepooManager by lazy { VeepooManager.getInstance(this) }

    private val activityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "需要蓝牙和位置权限才能连接设备", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        veepooManager.initialize()
        requestBluetoothPermissions()
        setContent {
            VitaSleepTheme {
                MainScreen(
                    veepooManager = veepooManager,
                    onRequestEnableBluetooth = {
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val needsRequest = requiredPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            activityPermissionLauncher.launch(requiredPermissions)
        }
    }

    companion object {
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    veepooManager: VeepooManager,
    onRequestEnableBluetooth: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var bluetoothEnabled by remember { mutableStateOf(veepooManager.hasBluetooth()) }

    val requiredPermissions = MainActivity.getRequiredPermissions()
    val permissionsGranted = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "需要蓝牙和位置权限才能连接设备", Toast.LENGTH_LONG).show()
        }
    }

    if (!bluetoothEnabled && permissionsGranted) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("开启蓝牙") },
            text = { Text("需要开启蓝牙才能扫描和连接Veepoo设备") },
            confirmButton = {
                TextButton(onClick = {
                    onRequestEnableBluetooth()
                    bluetoothEnabled = true
                }) { Text("开启蓝牙") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Device.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Device.route) {
                DeviceScreen(
                    onRequestPermissions = {
                        permissionLauncher.launch(requiredPermissions)
                    },
                    onRequestEnableBluetooth = onRequestEnableBluetooth
                )
            }
            composable(Screen.Health.route) { HealthScreen() }
            composable(Screen.Sleep.route) { SleepScreen() }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Schedule.route) { ScheduleScreen() }
        }
    }
}