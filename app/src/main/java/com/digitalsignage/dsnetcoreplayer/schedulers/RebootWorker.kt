package com.digitalsignage.dsnetcoreplayer.schedulers

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.digitalsignage.dsnetcoreplayer.utils.RootUtil
import java.io.IOException
import java.util.Calendar

class RebootWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val TAG = "RebootWorker"

    override fun doWork(): Result {
        try {
            Log.i(TAG, "Starting reboot work execution")

            // Check if device is idle using PowerManager
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
//            if (!powerManager.isDeviceIdleMode) {
//                Log.i(TAG, "Device is not idle, postponing reboot")
//                return Result.retry()
//            }

            // Get stored target time
            val prefs = applicationContext.getSharedPreferences("reboot_prefs", Context.MODE_PRIVATE)
            val targetTime = prefs.getString("reboot_time", null) ?: return Result.failure()

            // Check if we're within the acceptable time window (e.g., within 1 minute of target time)
            if (!isWithinTargetTimeWindow(targetTime)) {
                return Result.success()  // Not the right time, skip this execution
            }

            // Check if device has root permissions
            if (!hasRootPermission()) {
                Log.e(TAG, "Root permission not available")
                return Result.failure()
            }

            Log.i(TAG, "Executing reboot command")
            //Runtime.getRuntime().exec(arrayOf("su", "-c", "reboot"))
            reboot()
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during reboot execution", e)
            return Result.failure()
        }
    }
    public fun reboot() {
        if (RootUtil.isDeviceRooted() && RootUtil.isRootPermissionGranted()) {
            try {
                Runtime.getRuntime().exec(arrayOf("/system/bin/su", "-c", "reboot now"))
            } catch (e: IOException) {
                try {
                    Runtime.getRuntime().exec(arrayOf("/system/xbin/su", "-c", "reboot now"))
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
                //                e.printStackTrace();
            }
        }
    }
    private fun isWithinTargetTimeWindow(targetTime: String): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance()

        // Parse target time
        val timeParts = targetTime.split(":")
        target.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        target.set(Calendar.MINUTE, timeParts[1].toInt())
        target.set(Calendar.SECOND, timeParts[2].toInt())

        // Calculate difference in minutes
        val diffInMinutes = Math.abs(
            (now.timeInMillis - target.timeInMillis) / (60 * 1000)
        )

        // Return true if within 1 minute of target time
        return diffInMinutes <= 1
    }

    private fun hasRootPermission(): Boolean {
        return try {
            Runtime.getRuntime().exec("su")
            true
        } catch (e: Exception) {
            false
        }
    }
}