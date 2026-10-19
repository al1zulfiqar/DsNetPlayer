package com.digitalsignage.dsnetcoreplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.digitalsignage.dsnetcoreplayer.MainActivity


class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if app is in foreground
            val isInForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
                Lifecycle.State.STARTED)

            if (!isInForeground) {
                val i = Intent(context, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }else{
            }
        }
    }
}