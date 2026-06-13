package com.vitasleep.android.veepoo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class VeepooService : Service() {

    companion object {
        private const val TAG = "VeepooService"
        private const val CHANNEL_ID = "veepoo_connection_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, VeepooService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "VeepooService.start() called")
            } catch (e: Throwable) {
                Log.e(TAG, "VeepooService.start() failed", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, VeepooService::class.java))
                Log.d(TAG, "VeepooService.stop() called")
            } catch (e: Throwable) {
                Log.e(TAG, "VeepooService.stop() failed", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VeepooService.onCreate()")
        createNotificationChannel()
        val notification = createNotification("Veepoo 设备连接中")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "startForeground() success")
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground() failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "VeepooService.onStartCommand()")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "VeepooService.onDestroy()")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Veepoo 设备连接",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持与 Veepoo 设备的蓝牙连接"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VitaSleep")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
