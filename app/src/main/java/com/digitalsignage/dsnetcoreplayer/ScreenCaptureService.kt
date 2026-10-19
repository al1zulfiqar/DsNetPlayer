import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.digitalsignage.dsnetcoreplayer.MainActivity
import com.digitalsignage.dsnetcoreplayer.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {

    private lateinit var mediaProjection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var virtualDisplay: VirtualDisplay
    private var width: Int = 0
    private var height: Int = 0
    private var screenDensity: Int = 0
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("result_code", -1) ?: -1
        val data = intent!!.getParcelableExtra<Intent>("data")
        val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)


        getDisplayMetrics()
        startScreenCapture()
        startForeground(1, createNotification())
        return START_STICKY
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Screen Capture Service"
            val descriptionText = "Capturing screen every minute"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("ScreenCaptureService", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this@ScreenCaptureService, "ScreenCaptureService")
            .setContentTitle("Screen Capture Service")
            .setContentText("Capturing screen every minute")
            .setSmallIcon(R.drawable.ic_launcher_background) // Use your app's notification icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    private fun getDisplayMetrics() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WindowManager::class.java)
        val display: Display? = windowManager?.defaultDisplay
        display?.getMetrics(metrics)

        width = metrics.widthPixels
        height = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    private fun startScreenCapture() {
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1)
        val surface: Surface = imageReader.surface
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            screenDensity,
            0,
            surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                saveImage(it)
                it.close()
            }
        }, null)
    }

    private fun saveImage(image: Image) {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Convert YUV to Bitmap
        val yuvImage = YuvImage(bytes, ImageFormat.NV21, width, height, null)
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
        val jpegData = outputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)

        // Save to Downloads folder
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "screenshot_${System.currentTimeMillis()}.png"
        )

        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        } catch (e: Exception) {
            Log.e("MediaProjectionService", "Error saving screenshot", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
