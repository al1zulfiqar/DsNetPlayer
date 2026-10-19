package com.digitalsignage.dsnetcoreplayer.schedulers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RebootScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val TAG = "RebootScheduler"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun scheduleReboot(timeString: String) {
        // Extract only time from the input string
        val time = extractTimeFromDateTime(timeString)

        // Store only the time in SharedPreferences
        context.getSharedPreferences("reboot_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("reboot_time", time)
            .apply()

        val rebootWorkRequest = createDailyRebootRequest(time)

        // Cancel any existing reboot work
        workManager.cancelAllWorkByTag(REBOOT_WORK_TAG)

        // Schedule new reboot work
        workManager.enqueue(rebootWorkRequest)
            .result
            .addListener({
                Log.i(TAG, "Reboot scheduled successfully for time: $time")
            }, { runnable ->
                mainHandler.post(runnable)
            })
    }

    private fun extractTimeFromDateTime(dateTimeString: String): String {
        // Parse the ISO format (2025-01-08T19:30:00) and extract only time
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateTimeString)
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time", e)
            "00:00:00" // Default time if parsing fails
        }
    }

    private fun createDailyRebootRequest(targetTime: String): PeriodicWorkRequest {
        val initialDelay = calculateInitialDelay(targetTime)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        return PeriodicWorkRequestBuilder<RebootWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(REBOOT_WORK_TAG)
            .build()
    }

    private fun calculateInitialDelay(targetTime: String): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance()

        // Parse target time
        val timeParts = targetTime.split(":")
        target.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        target.set(Calendar.MINUTE, timeParts[1].toInt())
        target.set(Calendar.SECOND, timeParts[2].toInt())

        // If target time has already passed today, schedule for tomorrow
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        private const val REBOOT_WORK_TAG = "daily_reboot_work"
    }
}