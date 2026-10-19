package com.digitalsignage.dsnetcoreplayer.job

import android.app.job.JobParameters
import android.app.job.JobService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.digitalsignage.dsnetcoreplayer.services.EnhancedForegroundService

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ServiceRestartJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        // Check if service is running
        if (!isServiceRunning(EnhancedForegroundService::class.java)) {
            // Restart service
            val serviceIntent = Intent(applicationContext, EnhancedForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        return false // Job is finished
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Reschedule the job
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
