package com.digitalsignage.dsnetcoreplayer.companion
import android.app.Activity

import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import kotlin.system.exitProcess

class AppRestarter {
    companion object {
        /**
         * Restarts the app with a clean process state
         * @param activity The current activity
         * @param delayMillis Optional delay before relaunch (default 100ms)
         */
        fun restartApp(activity: Activity, delayMillis: Long = 100) {
            try {
                val packageManager = activity.packageManager
                val packageName = activity.packageName

                // Create a fresh start intent
                val intent = Intent(activity, activity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    // Add action to identify this as a restart
                    action = "app.restart.action"
                }

                // Schedule the restart
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        // Start the new instance
                        activity.startActivity(intent)

                        // Finish all activities in the current task
                        activity.finishAffinity()

                        // Kill the current process
                        Process.killProcess(Process.myPid())
                        exitProcess(0)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, delayMillis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fun restartAppWithIntentFlags(activity: Activity, delayMillis: Long = 100) {
            try {
                // Create a restart intent with comprehensive flags
                val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)?.apply {
                    // Clear top and create new task
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

                    // Add restart identifier
                    putExtra("is_restart", true)
                }

                // Ensure intent is not null
                intent?.let {
                    // Optional: Add a small delay if needed
                    if (delayMillis > 0) {
                        SystemClock.sleep(delayMillis)
                    }

                    // Start the app with the new intent
                    activity.startActivity(it)

                    // Finish current activities
                    activity.finishAffinity()

                    // Additional process kill (optional)
                    Process.killProcess(Process.myPid())
                } ?: run {
                    Log.e("AppRestarter", "Could not get launch intent for package")
                }
            } catch (e: Exception) {
                Log.e("AppRestarter", "Error in restartAppWithIntentFlags", e)
                e.printStackTrace()
            }
        }
        fun restartAppWithAlarm(activity: Activity, delayMillis: Long = 100) {
            try {
                val alarmManager = activity.getSystemService(Activity.ALARM_SERVICE) as android.app.AlarmManager

                // Verify AlarmManager is available
                if (alarmManager == null) {
                    Log.e("AppRestarter", "AlarmManager is null")
                    return
                }

                // Create restart intent
                val intent = Intent(activity, activity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    action = "app.restart.action"
                    putExtra("is_restart", true)
                }

                // Create pending intent with proper flags for Android 12+
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                } else {
                    android.app.PendingIntent.FLAG_ONE_SHOT
                }

                val pendingIntent = android.app.PendingIntent.getActivity(
                    activity,
                    0,
                    intent,
                    flags
                )

                val restartTime = System.currentTimeMillis() + delayMillis

                // Check alarm permission for Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e("AppRestarter", "No permission to schedule exact alarms")
                        // Optionally, request permission or fallback to inexact alarm
                        return
                    }
                }

                // Schedule restart based on Android version
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            restartTime,
                            pendingIntent
                        )
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            restartTime,
                            pendingIntent
                        )
                    }
                    else -> {
                        alarmManager.setExact(
                            android.app.AlarmManager.RTC_WAKEUP,
                            restartTime,
                            pendingIntent
                        )
                    }
                }

                // Finish current instance
                activity.finishAffinity()
                Process.killProcess(Process.myPid())
                exitProcess(0)
            } catch (e: Exception) {
                Log.e("AppRestarter", "Error in restartAppWithAlarm", e)
                e.printStackTrace()
            }
        }
    }
}