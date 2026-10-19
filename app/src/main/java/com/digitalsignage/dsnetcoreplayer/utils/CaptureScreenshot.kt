package com.digitalsignage.dsnetcoreplayer.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.digitalsignage.dsnetcoreplayer.MainActivity
import com.digitalsignage.dsnetcoreplayer.constants.Constants
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CaptureScreenshot(
    private val mRootView: View,
    private val mAppCompactActivity: AppCompatActivity
) {
    private val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
    private val screenshotScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val imageQuality = 40 // Reduced quality as requested

    suspend fun takeAndUploadScreenShot(accountId: String, playerId: String) = withContext(Dispatchers.Default) {
        try {
            val bitmap = getBitmapOfRootView(mRootView)
            if (bitmap == null) {
                Log.w("Screenshot", "Failed to capture screenshot")
                return@withContext
            }

            try {
                val optimizedBitmap = optimizeBitmap(bitmap)
                bitmap.recycle() // Recycle original bitmap

                if (optimizedBitmap != null) {
                    withContext(Dispatchers.IO) {
                        uploadScreenshot(accountId, Utilities.getCurrentDateTime(), playerId, optimizedBitmap)
                    }
                    optimizedBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e("Screenshot", "Error processing bitmap", e)
            }
        } catch (e: Exception) {
            Log.e("Screenshot", "Error in takeAndUploadScreenShot", e)
        }
    }

    private fun getBitmapOfRootView(v: View): Bitmap? {
        try {
            if (v.isAttachedToWindow.not()) {
                return null
            }

            val rootView = v.rootView
            val window = (mAppCompactActivity as? MainActivity)?.window
            if (window == null) {
                return null
            }

            // Use RGB_565 for reduced memory usage
            val bitmap = Bitmap.createBitmap(
                rootView.width,
                rootView.height,
                Bitmap.Config.RGB_565
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val location = IntArray(2)
                rootView.getLocationInWindow(location)
                val rect = Rect(
                    location[0],
                    location[1],
                    location[0] + rootView.width,
                    location[1] + rootView.height
                )

                var pixelCopyResult = false
                val latch = CountDownLatch(1)

                window.let {
                    val onCopyFinished = PixelCopy.OnPixelCopyFinishedListener { result ->
                        pixelCopyResult = (result == PixelCopy.SUCCESS)
                        latch.countDown()
                    }
                    PixelCopy.request(window, rect, bitmap, onCopyFinished, Handler(Looper.getMainLooper()))
                }

                latch.await(1, TimeUnit.SECONDS)

                if (!pixelCopyResult) {
                    bitmap.recycle()
                    return null
                }
            } else {
                Canvas(bitmap).apply {
                    rootView.draw(this)
                }
            }
            return bitmap
        } catch (e: Exception) {
            Log.e("Screenshot", "Error capturing screenshot", e)
            return null
        }
    }

    private fun optimizeBitmap(original: Bitmap): Bitmap? {
        try {
            val targetWidth = 630
            val targetHeight = 350
            val scale = Math.min(
                targetWidth.toFloat() / original.width,
                targetHeight.toFloat() / original.height
            )

            val newWidth = (original.width * scale).toInt()
            val newHeight = (original.height * scale).toInt()

            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)?.also { scaled ->
                val stream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, imageQuality, stream)
                stream.close()
            }
        } catch (e: Exception) {
            Log.e("Screenshot", "Error optimizing bitmap", e)
            return null
        }
    }

    private suspend fun uploadScreenshot(
        accountId: String,
        date: String,
        playerId: String,
        bitmap: Bitmap
    ) {
        try {
            val screenshotData = Utilities.takeScreenshot(bitmap)
            val request = ScreenshotRequest(accountId, date, playerId, screenshotData)

            apiService.uploadScreenshot(request).let { response ->
                if (response.isSuccessful) {
                } else {

                }
            }
        } catch (e: Exception) {

        }
    }

    fun cleanup() {
        screenshotScope.cancel()
    }
}

class ScreenshotService(private val context: MainActivity) {
    private var screenshotJob: Job? = null
    private var captureScreenshot: CaptureScreenshot? = null

    fun start(player: Player) {
        stop()

        captureScreenshot = CaptureScreenshot(context.mainContainer, context)
        screenshotJob = CoroutineScope(Dispatchers.Default).launch {
            flow {
                while (true) {
                    emit(Unit)
                    delay( 1* 60 * 1000) // 3 minute
                   // Utilities.showToast(context, "in job")
                    if (!isActive) break
                    //Utilities.showToast(context, "checking network")
                    if (NetworkUtils.isInternetAvailable(context)) {
                        //Utilities.showToast(context, "uploading screenshot")
                        captureScreenshot?.takeAndUploadScreenShot(player.accountID, player.playerID)
                    }
                }
            }.catch { e ->
                //Utilities.showToast(context, "Error screenshot")
                Log.e("ScreenshotService", "Error in screenshot flow", e)
            }.collect {}  // Empty collector lambda
        }
    }

    fun stop() {
        screenshotJob?.cancel()
        screenshotJob = null
        captureScreenshot?.cleanup()
        captureScreenshot = null
    }
}
//import android.graphics.Bitmap
//import android.util.Log
//import android.view.View
//import android.view.ViewTreeObserver
//import androidx.appcompat.app.AppCompatActivity
//import com.digitalsignage.dsnetcoreplayer.MainActivity
//import com.digitalsignage.dsnetcoreplayer.constants.Constants
//import com.digitalsignage.dsnetcoreplayer.network.RetrofitClient
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import retrofit2.Response
//import java.io.IOException
//import java.net.SocketTimeoutException
//
//class CaptureScreenshot(private val mRootView: View, private val mAppCompactActivity: AppCompatActivity) {
//
//    val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
//
//    fun takeAndUploadScreenShot(accountId: String, playerId: String) {
//        var bitmap: Bitmap? = getBitmapOfRootView(mRootView)
//        val date = Utilities.getCurrentDateTime()
//        val memInfo = Utilities.getMemoryInfo(mAppCompactActivity as MainActivity)
//
//        if (bitmap != null && !memInfo.lowMemory) {
//            // Calculate new dimensions while preserving aspect ratio
//            val targetWidth = 630
//            val targetHeight = 350
//            val originalWidth = bitmap.width
//            val originalHeight = bitmap.height
//
//            // Determine if we're in portrait or landscape
//            val isPortrait = originalHeight > originalWidth
//
//            // Calculate new dimensions maintaining aspect ratio
//            val newWidth: Int
//            val newHeight: Int
//            if (isPortrait) {
//                // In portrait mode, use height as the controlling dimension
//                newHeight = targetHeight
//                newWidth = (targetHeight.toFloat() * originalWidth / originalHeight).toInt()
//            } else {
//                // In landscape mode, use width as the controlling dimension
//                newWidth = targetWidth
//                newHeight = (targetWidth.toFloat() * originalHeight / originalWidth).toInt()
//            }
//
//            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
//            uploadScreenshot(accountId, date, playerId, bitmap)
//        } else if (memInfo.lowMemory) {
//            Log.w("MemoryIssue", "No screenshot sent because bitmap was not created due to low memory")
//        }
//    }
//
//    private fun getBitmapOfRootView(v: View): Bitmap? {
//        if (v == null) return null
//
//        val rootView = v.rootView
//        rootView.isDrawingCacheEnabled = true
//        var bitmap: Bitmap? = null
//
//        try {
//            val memInfo = Utilities.getMemoryInfo(mAppCompactActivity as MainActivity)
//            if (!memInfo.lowMemory) {
//                // Get the display rotation
//                val rotation = mAppCompactActivity.windowManager.defaultDisplay.rotation
//                bitmap = Bitmap.createBitmap(rootView.drawingCache)
//            } else {
//                Log.w("MemoryIssue", "No screenshot sent because bitmap was not created due to low memory")
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        } finally {
//            rootView.isDrawingCacheEnabled = false
//        }
//
//        return bitmap
//    }
//
//    private fun uploadScreenshot(accountId: String, date: String, playerId: String, bitmap: Bitmap) {
//        // Convert bitmap to base64 or desired format for the request body
//        val screenshotRequest = ScreenshotRequest(accountId, date, playerId, Utilities.takeScreenshot(bitmap)) // Modify as per your request structure
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response: Response<String> = apiService.uploadScreenshot(screenshotRequest)
//
//                withContext(Dispatchers.Main) {
//                    try {
//                        if (response.isSuccessful) {
//                            Log.d("Upload", "Screenshot uploaded successfully: ${response.body()}")
//                        } else {
//                            Log.e("Upload", "Failed to upload screenshot: ${response.errorBody()?.string() ?: "Unknown error"}")
//                        }
//                    } catch (e: Exception) {
//                        Log.e("Upload", "Error processing response", e)
//                    }
//                }
//            } catch (socketTimeoutException: SocketTimeoutException) {
//                withContext(Dispatchers.Main) {
//                    Log.e("Upload", "Network timeout occurred", socketTimeoutException)
//                    // Optional: Show user-friendly error message or retry mechanism
//                }
//            } catch (ioException: IOException) {
//                withContext(Dispatchers.Main) {
//                    Log.e("Upload", "Network error occurred", ioException)
//                    // Handle network connectivity issues
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Log.e("Upload", "Unexpected error during screenshot upload", e)
//                    // Handle any other unexpected errors
//                }
//            }
//        }
//    }
//}
