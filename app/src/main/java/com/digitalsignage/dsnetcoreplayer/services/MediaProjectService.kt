//package com.digitalsignage.dsnetcoreplayer.services
//
//import android.R.attr.height
//import android.R.attr.width
//import android.app.Activity
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Intent
//import android.content.pm.ServiceInfo
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.PixelFormat
//import android.hardware.display.DisplayManager
//import android.hardware.display.VirtualDisplay
//import android.media.Image
//import android.media.ImageReader
//import android.media.projection.MediaProjection
//import android.media.projection.MediaProjectionManager
//import android.os.Build
//import android.os.Environment
//import android.os.Handler
//import android.os.IBinder
//import android.os.Looper
//import android.util.DisplayMetrics
//import android.util.Log
//import android.view.WindowManager
//import android.widget.Toast
//import androidx.core.app.NotificationCompat
//import com.digitalsignage.dsnetcoreplayer.R
//import com.digitalsignage.dsnetcoreplayer.constants.Constants
//import com.digitalsignage.dsnetcoreplayer.network.RetrofitClient
//import com.digitalsignage.dsnetcoreplayer.utils.ScreenshotRequest
//import com.digitalsignage.dsnetcoreplayer.utils.Utilities
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import retrofit2.Response
//
//
//
//class MediaProjectionService : Service() {
//    private val TAG = "MediaProjectionService"
//    private var DISPLAY_WIDTH: Int = 0
//    private var DISPLAY_HEIGHT: Int = 0
//    private lateinit var mediaProjectionManager: MediaProjectionManager
//    private var mediaProjection: MediaProjection? = null
//    private var virtualDisplay: VirtualDisplay? = null
//    private var imageReader: ImageReader? = null
//    val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
//    private var accountId:String?=""
//    private var playerId:String?=""
//    override fun onCreate() {
//        super.onCreate()
//        mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
//
//        // Get display dimensions
//        val displayMetrics = DisplayMetrics()
//        val windowManager = getSystemService(WindowManager::class.java)
//        windowManager.defaultDisplay.getMetrics(displayMetrics)
//
//        DISPLAY_WIDTH = displayMetrics.widthPixels
//        DISPLAY_HEIGHT = displayMetrics.heightPixels
//    }
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // Start foreground service
//        val notification = createNotification()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
//        } else {
//            startForeground(1, notification)
//        }
//
//        intent?.let {
//            val resultCode = it.getIntExtra("resultCode", Activity.RESULT_CANCELED)
//            val data = it.getParcelableExtra<Intent>("data")
//            accountId = it.getStringExtra("accountId")
//            playerId = it.getStringExtra("playerId")
//            if (resultCode == Activity.RESULT_OK && data != null) {
//                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
//
//                captureScreen(resultCode, data)
//
//            }
//        }
//
//
//        return START_STICKY
//    }
//
//    private fun createNotification(): Notification {
//        val channelId = "media_projection_channel"
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Media Projection Service",
//                NotificationManager.IMPORTANCE_LOW
//            )
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        return NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Media Projection Service")
//            .setContentText("Running...")
//            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
//            .build()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    public fun captureScreen(resultCode: Int, data: Intent) {
//        val metrics = resources.displayMetrics
//        val density = metrics.densityDpi
//        val width = metrics.widthPixels
//        val height = metrics.heightPixels
//
//        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
//        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
//
//        virtualDisplay = mediaProjection?.createVirtualDisplay(
//            "ScreenCapture",
//            width, height, density,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            imageReader!!.surface, null, null
//        )
//
//        Handler(Looper.getMainLooper()).postDelayed({
//
//            val image = imageReader!!.acquireLatestImage()
//            if(image!=null) {
//                val planes = image.planes
//                val buffer = planes[0].buffer
//                val pixelStride = planes[0].pixelStride
//                val rowStride = planes[0].rowStride
//                val rowPadding = rowStride - pixelStride * width
//
//                val bitmap = Bitmap.createBitmap(
//                    width + rowPadding / pixelStride, height,
//                    Bitmap.Config.ARGB_8888
//                )
//                bitmap.copyPixelsFromBuffer(buffer)
//                uploadScreenshot(accountId,  System.currentTimeMillis().toString() ,playerId ,bitmap )
//                // Save the bitmap to the Downloads folder
////                val timeStamp =
////                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
////                val fileName = "Screenshot_Dsnet.png"
////                val file = File(
////                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
////                    fileName
////                )
////                if(file.exists()){
////                    file.delete()
////                }
////                try {
////                    val fos = FileOutputStream(file)
////                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
////                    fos.flush()
////                    fos.close()
////                    Toast.makeText(this, "Screenshot saved to Downloads", Toast.LENGTH_SHORT).show()
////                } catch (e: Exception) {
////                    e.printStackTrace()
////                    Toast.makeText(this, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
////                }
//
//                image.close()
//                virtualDisplay?.release()
//                mediaProjection?.stop()
//                releaseResources()
//            }
//        }, 1000)
//    }
//
//    private fun uploadScreenshot(accountId: String?, date: String?, playerId: String?, bitmap: Bitmap) {
//        // Convert bitmap to base64 or desired format for the request body
//        val screenshotRequest = ScreenshotRequest(accountId!!, date!!, playerId!!, Utilities.takeScreenshot(bitmap)) // Modify as per your request structure
//
//        CoroutineScope(Dispatchers.IO).launch {
//            val response: Response<String> = apiService.uploadScreenshot(screenshotRequest)
//
//            withContext(Dispatchers.Main) {
//                if (response.isSuccessful) {
//                    Log.d(TAG, "Screenshot uploaded successfully: ${response.body()}")
//                } else {
//                    Log.e(TAG, "Failed to upload screenshot: ${response.errorBody()?.string()}")
//                }
//            }
//        }
//    }
//    private fun releaseResources() {
//        virtualDisplay?.release()
//        virtualDisplay = null
//        imageReader?.close()
//        imageReader = null
//        mediaProjection?.stop()
//        mediaProjection = null
//    }
//
//}