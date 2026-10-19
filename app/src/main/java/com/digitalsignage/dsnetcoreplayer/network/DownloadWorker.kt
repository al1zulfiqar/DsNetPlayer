package com.digitalsignage.dsnetcoreplayer.network

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val filename = inputData.getString("filename") ?: return Result.failure()

        return try {
            downloadFile(url, filename)
            // Log a success message
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DownloadWorker", "Download failed: ${e.message}")
            Result.failure()
        }
    }

    private fun downloadFile(urlString: String, filename: String) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        // Use app's private storage directory
        val filesDir = applicationContext.getExternalFilesDir("DownloadedFiles")
        if (filesDir == null || (!filesDir.exists() && !filesDir.mkdirs())) {
            throw IOException("Failed to create directory for downloads")
        }

        val file = File(filesDir, filename)
        val inputStream: InputStream = BufferedInputStream(connection.inputStream)
        val outputStream = FileOutputStream(file)

        val data = ByteArray(1024)
        var count: Int
        while (inputStream.read(data).also { count = it } != -1) {
            outputStream.write(data, 0, count)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        connection.disconnect()
    }
}

