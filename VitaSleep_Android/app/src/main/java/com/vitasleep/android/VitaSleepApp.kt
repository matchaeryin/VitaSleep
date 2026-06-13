package com.vitasleep.android

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.util.VPLogger
import dagger.hilt.android.HiltAndroidApp
import java.io.File
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

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                pw.println("=== VitaSleep Crash Log ===")
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
                pw.println()
                pw.println("--- Suppressed ---")
                for (suppressed in throwable.suppressed) {
                    pw.println("Suppressed: ${suppressed.javaClass.name}: ${suppressed.message}")
                    suppressed.printStackTrace(pw)
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

                try {
                    val historyFile = File(filesDir, "crash_history.log")
                    val historyEntry = "\n\n${"=".repeat(60)}\n${crashLog}"
                    historyFile.appendText(historyEntry)
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

    fun getLastCrashLog(): String? {
        val fromPrefs = prefs.getString("last_crash", null)
        if (fromPrefs != null) return fromPrefs
        return try {
            val file = File(filesDir, "last_crash.log")
            if (file.exists()) file.readText() else null
        } catch (_: Throwable) {
            null
        }
    }

    fun clearCrashLog() {
        prefs.edit().remove("last_crash").remove("last_crash_time").apply()
        try {
            File(filesDir, "last_crash.log").delete()
        } catch (_: Throwable) {
        }
    }

    val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
}
