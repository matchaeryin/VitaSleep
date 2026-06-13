package com.vitasleep.android

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                pw.println("=== VitaSleep Crash Log ===")
                pw.println("Time: ${sdf.format(Date())}")
                pw.println("Thread: ${thread.name}")
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

                val file = File(filesDir, "last_crash.log")
                file.writeText(crashLog)

                val historyFile = File(filesDir, "crash_history.log")
                val historyEntry = "\n\n${"=".repeat(60)}\n${crashLog}"
                historyFile.appendText(historyEntry)
            } catch (_: Throwable) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
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
            com.jieli.jl_rcsp.util.JL_Log.setLog(true)
        } catch (e: Throwable) {
            Log.e("VitaSleepApp", "JL_Log init failed", e)
        }
    }

    fun getLastCrashLog(): String? {
        return try {
            val file = File(filesDir, "last_crash.log")
            if (file.exists()) file.readText() else null
        } catch (_: Throwable) {
            null
        }
    }

    fun clearCrashLog() {
        try {
            File(filesDir, "last_crash.log").delete()
        } catch (_: Throwable) {
        }
    }
}
