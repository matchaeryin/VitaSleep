package com.vitasleep.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Chat : Screen("chat", "对话", Icons.Default.Chat)
    data object Health : Screen("health", "身体", Icons.Default.Favorite)
    data object Schedule : Screen("schedule", "日程", Icons.Default.CalendarMonth)

    data object Device : Screen("device", "设备", Icons.Default.Watch)
    data object Sleep : Screen("sleep", "睡眠", Icons.Default.Bedtime)
}

val bottomNavItems = listOf(
    Screen.Chat,
    Screen.Health,
    Screen.Schedule
)
