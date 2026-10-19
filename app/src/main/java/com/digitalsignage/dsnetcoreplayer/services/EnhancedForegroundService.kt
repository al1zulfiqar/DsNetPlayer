package com.digitalsignage.dsnetcoreplayer.services
import android.annotation.SuppressLint
import android.app.*
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.digitalsignage.dsnetcoreplayer.R
import com.digitalsignage.dsnetcoreplayer.job.ServiceRestartJobService

class EnhancedForegroundService : Service() {

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "MediaPlayerChannel"
    private val ACTION_RESTART_SERVICE = "com.yourapp.ACTION_RESTART_SERVICE"
    private val CHECK_INTERVAL = 30000L // 30 seconds
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var jobScheduler: JobScheduler
    private val JOB_ID = 100

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startForegroundCheck()
        scheduleServiceRestartJob()
        registerReceivers()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MediaPlayer::ServiceWakeLock"
        )
        wakeLock.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If service is killed, restart it
        return START_STICKY
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Schedule immediate restart
        val restartIntent = Intent(applicationContext, EnhancedForegroundService::class.java)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC,
            System.currentTimeMillis() + 1000,
            restartServicePendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        unregisterReceivers()
        // Restart service
        val broadcastIntent = Intent(ACTION_RESTART_SERVICE)
        sendBroadcast(broadcastIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private fun scheduleServiceRestartJob() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val builder = JobInfo.Builder(JOB_ID,
                ComponentName(packageName, ServiceRestartJobService::class.java.name)
            )

            builder.setPersisted(true)
                .setPeriodic(15 * 60 * 1000) // 15 minutes
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setRequiresCharging(false)

            jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(ACTION_RESTART_SERVICE)
        }
        registerReceiver(serviceRestartReceiver, filter)
    }

    private val serviceRestartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> checkAndBringToForeground()
                ACTION_RESTART_SERVICE -> startService(Intent(context, EnhancedForegroundService::class.java))
            }
        }
    }

    private fun buildNotification(): Notification {
        // Create an intent to launch your main activity
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Create the pending intent
        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(
                this,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
        } else null

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DsNetService")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(false) // Don't cancel the notification when clicked
            .setOngoing(true)     // Make the notification persistent

        // Set the pending intent if we successfully created one
        pendingIntent?.let {
            builder.setContentIntent(it)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Media Player Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun checkAndBringToForeground() {
        // Implement your logic to bring the service to the foreground if necessary
    }

    private fun startForegroundCheck() {
        // Implement your logic for foreground checking if necessary
    }

    private fun unregisterReceivers() {
        unregisterReceiver(serviceRestartReceiver)
    }
}