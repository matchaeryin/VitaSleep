package com.vitasleep.android

import android.app.Application
import android.util.Log
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.util.VPLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VitaSleepApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                "VitaSleepCrash",
                "Uncaught exception in thread: ${thread.name}",
                throwable
            )
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
}
