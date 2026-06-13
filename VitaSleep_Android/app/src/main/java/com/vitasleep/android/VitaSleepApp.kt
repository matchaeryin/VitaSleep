package com.vitasleep.android

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.util.VPLogger
import dagger.hilt.android.HiltAndroidApp
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class VitaSleepApp : Application() {

    private val prefs by lazy {
        getSharedPreferences("vitasleep_crash", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        val currentPid = android.os.Process.myPid()
        val prevPid = prefs.getInt("app_pid", -1)

        capturePreviousLogcat(prevPid)
        checkRealtimeLogcatFile()

        prefs.edit().putInt("app_pid", currentPid).apply()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                pw.println("=== VitaSleep Java Crash Log ===")
                pw.println("Time: ${sdf.format(Date())}")
                pw.println("Thread: ${thread.name}")
                pw.println("PID: ${android.os.Process.myPid()}")
                pw.println("Exception: ${throwable.javaClass.name}")
                pw.println("Message: ${throwable.message}")
                pw.println()
                pw.println("--- Stack Trace ---")
                throwable.printStackTrace(pw)
                pw.println()
                pw.println("--- Cause ---")
                val cause = throwable.cause
                if (cause != null) {
                    pw.println("Cause: ${cause.javaClass.name}: ${cause.message}")
                    cause.printStackTrace(pw)
                } else {
                    pw.println("No cause")
                }
                pw.flush()

                val crashLog = sw.toString()
                Log.e("VitaSleepCrash", crashLog)

                prefs.edit()
                    .putString("last_crash", crashLog)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .apply()

                try {
                    val file = File(filesDir, "last_crash.log")
                    file.writeText(crashLog)
                } catch (_: Throwable) {
                }
            } catch (_: Throwable) {
            }

            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }

        try {
            VPOperateManager.getInstance().init(this)
            VPLogger.setDebug(true)
        } catch (e: Throwable) {
            Log.e("VitaSleepApp", "SDK init failed", e)
        }

        try {
            com.jieli.jl_rcsp.util.JL_Log.setTagPrefix("VitaSleep")
            com.jieli.jl_rcsp.util.JL_Log.configureLog(this, true, true)
        } catch (e: Throwable) {
            Log.e("VitaSleepApp", "JL_Log init failed", e)
        }
    }

    private fun capturePreviousLogcat(prevPid: Int) {
        try {
            if (prevPid <= 0) return

            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "--pid=$prevPid", "-t", "3000")
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.appendLine(line)
            }
            reader.close()
            process.waitFor()

            val logcat = sb.toString().trim()
            if (logcat.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val newLog = buildString {
                    appendLine("=== Logcat Capture (${sdf.format(Date())}) PID=$prevPid ===")
                    appendLine(logcat)
                    appendLine()
                }

                val existing = prefs.getString("logcat_capture", "")
                val combined = if (existing.isNullOrEmpty()) {
                    newLog
                } else {
                    newLog + "\n" + existing
                }

                val trimmed = if (combined.length > 50000) {
                    combined.take(50000)
                } else {
                    combined
                }

                prefs.edit()
                    .putString("logcat_capture", trimmed)
                    .apply()
            }
        } catch (e: Throwable) {
            Log.e("VitaSleepApp", "logcat capture failed", e)
        }
    }

    private fun checkRealtimeLogcatFile() {
        try {
            val file = File(filesDir, "realtime_logcat.txt")
            if (file.exists() && file.length() > 0) {
                val content = file.readText()
                if (content.isNotBlank()) {
                    val existing = prefs.getString("realtime_logcat", "")
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val newEntry = buildString {
                        appendLine("=== Realtime Logcat (${sdf.format(Date())}) ===")
                        appendLine(content.take(50000))
                    }
                    val combined = if (existing.isNullOrEmpty()) {
                        newEntry
                    } else {
                        newEntry + "\n" + existing
                    }
                    prefs.edit()
                        .putString("realtime_logcat", combined.take(50000))
                        .apply()
                }
            }
            file.delete()
        } catch (e: Throwable) {
            Log.e("VitaSleepApp", "checkRealtimeLogcatFile failed", e)
        }
    }

    fun getLastCrashLog(): String? {
        val parts = mutableListOf<String>()

        val javaCrash = prefs.getString("last_crash", null)
        if (!javaCrash.isNullOrEmpty()) {
            parts.add(javaCrash)
        }

        val logcatCapture = prefs.getString("logcat_capture", null)
        if (!logcatCapture.isNullOrEmpty()) {
            parts.add("\n=== System/Logcat Crash Info ===\n$logcatCapture")
        }

        val realtimeLogcat = prefs.getString("realtime_logcat", null)
        if (!realtimeLogcat.isNullOrEmpty()) {
            parts.add("\n=== Realtime Logcat (pre-crash) ===\n$realtimeLogcat")
        }

        return if (parts.isNotEmpty()) parts.joinToString("\n\n") else null
    }

    fun clearCrashLog() {
        prefs.edit()
            .remove("last_crash")
            .remove("last_crash_time")
            .remove("logcat_capture")
            .remove("realtime_logcat")
            .apply()
        try {
            File(filesDir, "last_crash.log").delete()
        } catch (_: Throwable) {
        }
    }

    val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
}
