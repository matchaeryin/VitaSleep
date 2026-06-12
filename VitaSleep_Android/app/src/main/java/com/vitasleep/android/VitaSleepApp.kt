package com.vitasleep.android

import android.app.Application
import android.util.Log
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
    }
}
