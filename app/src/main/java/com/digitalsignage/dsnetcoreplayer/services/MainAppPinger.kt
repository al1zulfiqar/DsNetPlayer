package com.digitalsignage.dsnetcoreplayer.services
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class MainAppPinger(context: Context) {
    private val TAG = "MainAppPinger"
    private companion object {
        private const val PING_INTERVAL = 30000L // 30 seconds
    }

    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var isPinging = false



    fun startPinging() {
        isPinging = true
    }

    fun stopPinging() {
        isPinging = false
    }

    public fun sendPing() {
        Log.d("pinging now","")
        val intent = Intent("com.digitalsignage.dsnetcoreplaye.PING")
        context.sendBroadcast(intent)
    }
}