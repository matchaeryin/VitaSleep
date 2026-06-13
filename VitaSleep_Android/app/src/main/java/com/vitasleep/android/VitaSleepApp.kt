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

        capturePreviousLogcat()

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

    private fun capturePreviousLogcat() {
        try {
            val pid = android.os.Process.myPid().toString()
            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-t", "2000")
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!
                if (l.contains("FATAL") ||
                    l.contains("signal") ||
                    l.contains("SIGSEGV") ||
                    l.contains("SIGABRT") ||
                    l.contains("SIGBUS") ||
                    l.contains("tombstone") ||
                    l.contains("CRASH") ||
                    l.contains("died") ||
                    l.contains("kill") ||
                    l.contains("Force finishing") ||
                    l.contains("Force finishing") ||
                    l.contains("Low Memory") ||
                    l.contains("ANR") ||
                    l.contains("VitaSleep") ||
                    l.contains("VeepooManager") ||
                    l.contains("VeepooService") ||
                    l.contains("VPOperateManager") ||
                    l.contains("BluetoothService") ||
                    l.contains("inuker") ||
                    l.contains("veepoo") ||
                    l.contains("vitasleep") ||
                    l.contains("NoClassDef") ||
                    l.contains("UnsatisfiedLink") ||
                    l.contains("NoSuchMethod") ||
                    l.contains("IllegalAccess") ||
                    l.contains("ForegroundService") ||
                    l.contains("SecurityException") ||
                    l.contains("AndroidRuntime")
                ) {
                    sb.appendLine(l)
                }
            }
            reader.close()
            process.waitFor()

            val logcat = sb.toString().trim()
            if (logcat.isNotEmpty()) {
                val existing = prefs.getString("logcat_capture", "")
                val newLog = StringBuilder()
                newLog.appendLine("=== Logcat Capture (${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}) PID=$pid ===")
                newLog.appendLine(logcat)
                newLog.appendLine()

                val combined = if (existing.isNullOrEmpty()) {
                    newLog.toString()
                } else {
                    newLog.toString() + "\n" + existing
                }

                val trimmed = if (combined.length > 30000) {
                    combined.take(30000)
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

    fun getLastCrashLog(): String? {
        val logcatCapture = prefs.getString("logcat_capture", null)
        val javaCrash = prefs.getString("last_crash", null)

        val parts = mutableListOf<String>()
        if (!javaCrash.isNullOrEmpty()) {
            parts.add(javaCrash)
        }
        if (!logcatCapture.isNullOrEmpty()) {
            parts.add("\n=== System/Logcat Crash Info ===\n$logcatCapture")
        }

        return if (parts.isNotEmpty()) parts.joinToString("\n\n") else null
    }

    fun clearCrashLog() {
        prefs.edit()
            .remove("last_crash")
            .remove("last_crash_time")
            .remove("logcat_capture")
            .apply()
        try {
            File(filesDir, "last_crash.log").delete()
        } catch (_: Throwable) {
        }
    }

    val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
}
