package com.vitasleep.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Device : Screen("device", "设备", Icons.Default.Watch)
    data object Health : Screen("health", "身体状态", Icons.Default.Favorite)
    data object Sleep : Screen("sleep", "睡眠", Icons.Default.Bedtime)
    data object Chat : Screen("chat", "健康助手", Icons.Default.Chat)
    data object Schedule : Screen("schedule", "日程", Icons.Default.CalendarMonth)
}

val bottomNavItems = listOf(
    Screen.Device,
    Screen.Health,
    Screen.Sleep,
    Screen.Chat,
    Screen.Schedule
)
