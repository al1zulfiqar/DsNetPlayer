package com.digitalsignage.dsnetcoreplayer.utils

import android.content.Context
import android.net.ConnectivityManager


object NetworkUtils {
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
        return false // No connectivity manager available
    }
}