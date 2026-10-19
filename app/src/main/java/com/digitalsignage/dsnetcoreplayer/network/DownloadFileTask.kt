import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.digitalsignage.dsnetcoreplayer.data.DownloadComplete
import com.digitalsignage.dsnetcoreplayer.data.MediaFile
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.EventBus
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DownloadFileTask(private val context: Context) : AsyncTask<List<MediaFile>, Int, Unit>() {

    private var totalItems: Int = 0
    private var downloadedCount: Int = 0
    private var eventPosted: Boolean = false
    override fun onPreExecute() {
        super.onPreExecute()
        // Initialize any UI components or show a progress dialog here if needed
    }

    override fun doInBackground(vararg mediaFiles: List<MediaFile>) {
        totalItems = mediaFiles[0].size
        Log.e("DownloadTask", "total-tems to download{$totalItems}")
        try {
            mediaFiles[0].forEachIndexed { index, mediaFile ->

                val destinationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DsNetMedia")
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs() // Create the directory if it doesn't exist
                }
//                val file = File(context.filesDir, mediaFile.fileName)
                var file = File(destinationDir, mediaFile.fileName)
                // If its word file change the download location
                if(mediaFile.fileName.contains(".docx") || mediaFile.fileName.contains(".pdf")){
                    val filesDir = context.getExternalFilesDir("DownloadedFiles")
                    if (filesDir == null || (!filesDir.exists() && !filesDir.mkdirs())) {
                        throw IOException("Failed to create directory for downloads")
                    }
                    file = File(filesDir, mediaFile.fileName)
                }
                val filePath = file.absolutePath

                // Skip downloading if file already exists
                if (file.exists()) {
                    publishProgress(index + 1) // Update progress for skipped files
                    return@forEachIndexed
                }

                val url = URL(mediaFile.url)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connect()
                }

                // Check for successful response
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("DownloadTask", "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                    return
                }

                // Download the file
                BufferedInputStream(connection.inputStream).use { input ->
                    FileOutputStream(filePath).use { output ->
                        val data = ByteArray(1024)
                        var downloaded = 0
                        var count: Int

                        while (input.read(data).also { count = it } != -1) {
                            output.write(data, 0, count)
                            downloaded += count
                            publishProgress((downloaded * 100 / connection.contentLength), totalItems - (index + 1)) // Update progress
                        }
                    }
                }

                downloadedCount++ // Increment downloaded count
                // Check if half of the items are downloaded
                if (totalItems > 1 && downloadedCount >= totalItems / 2 && !eventPosted) {
                    EventBus.getDefault().post(DownloadComplete())
                    Log.e("DownloadTask", "Now Playing")
                    eventPosted = true
                } else {
                    Log.e("DownloadTask", "item downloaded")
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadTask", "Error: ${e.message}")
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {
        // Update the UI with the progress if necessary
        // For example: update a progress dialog here
    }

    override fun onPostExecute(result: Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            EventBus.getDefault().post(DownloadComplete())
            Log.e("DownloadTask", "Completed")
        }, 1000)
    }
}
