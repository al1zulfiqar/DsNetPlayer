package com.digitalsignage.dsnetcoreplayer.utils

import DownloadFileTask
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Base64
import android.view.Display
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.digitalsignage.dsnetcoreplayer.constants.Constants
import com.digitalsignage.dsnetcoreplayer.constants.MediaType
import com.digitalsignage.dsnetcoreplayer.constants.ZoneType
import com.digitalsignage.dsnetcoreplayer.data.MediaFile
import com.digitalsignage.dsnetcoreplayer.data.player.Media
import com.digitalsignage.dsnetcoreplayer.data.player.Page
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.data.player.Schedule
import com.digitalsignage.dsnetcoreplayer.data.player.Zone
import com.digitalsignage.dsnetcoreplayer.network.DownloadWorker
import com.digitalsignage.dsnetcoreplayer.prefrences.SharedPreferencesManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class Utilities {


    companion object {
        private var deviceWidth = 0
        private var deviceHeight = 0
        private var pageWidth = 0L
        private var pageHeight = 0L
        public var versionCodeFromPackage = 0
        public lateinit var versionNameFromPackage: String

        var AppUUID: String = ""


        fun checkUUID(context: Context) {
            AppUUID = SharedPreferencesManager(context).getString(Constants.PLAYER_ID, "")
            if (AppUUID.isEmpty()) {
                AppUUID = UUID.randomUUID().toString()
                SharedPreferencesManager(context).addString(Constants.PLAYER_ID, AppUUID)
            }
//            if (SharedPreferencesManager(context).getString(Constants.PLAYER_ID,"").) {
//                val rootFolder = File(Environment.getExternalStorageDirectory(), rootFolderName)
//
//                if (rootFolder.exists()) {
//                    val files = rootFolder.listFiles()
//
//                    files?.forEach { file ->
//                        if (FilenameUtils.getExtension(file.absolutePath) == "uuid") {
//                            AppUUID = FilenameUtils.getBaseName(file.absolutePath)
//                            return@forEach
//                        }
//                    }
//                }else{
//                    createFolders()
//                    AppUUID = UUID.randomUUID().toString()
//                    try {
//                        File(rootFolder, AppUUID).createNewFile()
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//            }
        }

        fun getScaledZoneDimensions(zone: Zone): List<Int> {
            return listOf(
                Utilities.getScaledWidth(zone.x),
                Utilities.getScaledHeight(zone.y),
                Utilities.getScaledWidth(zone.width),
                Utilities.getScaledHeight(zone.height)
            )
        }
        public fun getAppVersion(context: Context): String {
            return try {
                val packageManager: PackageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName // Returns the version name (e.g., "1.0.0")
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                "Version not found"
            }
        }
        fun extractFileName(url: String): String {
            val pattern = "/([^/]+\\.docx)\\?".toRegex()
            return pattern.find(url)?.groupValues?.get(1) ?: "default.docx"
        }
        fun startDownload(url: String, filename: String, folderName: String, context: Context) {
            val downloadRequest = OneTimeWorkRequest.Builder(DownloadWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString("url", url)
                        .putString("filename", filename)
                        .putString("folderName", folderName)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(downloadRequest)

        }

        fun showToast(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun setupDeviceResolutionVariables(activity: Activity) {
            val windowManager = activity.windowManager
            windowManager?.let {
                val display = it.defaultDisplay
                val point = android.graphics.Point()
                display.getRealSize(point)
                deviceWidth = point.x
                deviceHeight = point.y
            }
        }

        fun getCurrentDateTime(): String {
            val currentDateTime =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            return currentDateTime
        }

//        fun filterMediaList(mediaList: MutableList<Media>, currentDateTime: String): List<Media> {
//            // Define the date format that matches the format of expiryTime from the server
//            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
//
//            // Parse the current date and time
//            val currentDate: Date = dateFormat.parse(currentDateTime) ?: Date()
//
//            // Filter out expired media and those with length "0" or not convertible to long
//            return mediaList.filter { media ->
//                val expiryTime = media.expiryTime
//                val mediaLength = media.length // Assuming there's a length property
//
//                // Check if media length is valid
//                if (mediaLength.equals("0") || mediaLength.toString().toLongOrNull() == null) {
//                    false // Remove this media
//                } else if (expiryTime == "0001-01-01T00:00:00") {
//                    true // Keep media with this expiry time
//                } else {
//                    val expiryDate: Date = dateFormat.parse(expiryTime) ?: Date(0)
//                    // Keep media that is not expired
//                    expiryDate.after(currentDate)
//                }
//            }.toMutableList()
//        }

        fun setupPageResolution(currentPage: Page) {
            if (currentPage != null) {

                pageWidth = if (currentPage.width == 0L) 800 else currentPage.width
                pageHeight = if (currentPage.height == 0L) 600 else currentPage.height
            } else {
                pageWidth = 800
                pageHeight = 600
            }
        }

        fun setupAppVersion(context: Context) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                versionCodeFromPackage = packageInfo.versionCode
                versionNameFromPackage = packageInfo.versionName

            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        public fun getScaledWidth(value: Double): Int {
            return ((deviceWidth.toFloat() / pageWidth) * value).toInt()
        }

        public fun getScaledHeight(value: Double): Int {
            return ((deviceHeight.toFloat() / pageHeight) * value).toInt()
        }



        fun startDownloadMediaIfNeeded(player: Player?, context: Context): Boolean {
            val destinationDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "DsNetMedia"
            )
            val downloadableFiles = ArrayList<MediaFile>()
            if (player != null && player.playlist != null && player.playlist.size > 0) {
                for (playlist in player.playlist) {
                    for (media in playlist.media) {
                        var file = File(destinationDir, media.name)
                        //val isDocument = media.type.equals(MediaType.HTML) && media.url.contains("https://www.dropbox.com")
                        val isDocument = (media.type.equals(MediaType.DOCX) || media.type.equals(MediaType.PDF))
                        if(isDocument){
                            val filesDir = context.getExternalFilesDir("DownloadedFiles")
                            if (filesDir == null || (!filesDir.exists() && !filesDir.mkdirs())) {
                                throw IOException("Failed to create directory for downloads")
                            }
                            file = File(filesDir, media.name)
                        }
                        if (!file.exists()) {
                            if (media.type in setOf(
                                    MediaType.VIDEO,
                                    MediaType.AUDIO,
                                    MediaType.IMAGE,
                                    MediaType.PDF)
                                    || isDocument) {
                                    downloadableFiles.add(MediaFile(media.url, media.name))
                            }
                        }
                    }
                    if (playlist.page!!.size > 0) {
                        for (page in playlist.page) {
                            if (page.backgroundImageMed != null) {
                                val file = File(
                                    destinationDir, Utilities.getFileNameWithExtension(
                                        page.backUrl!!
                                    )!!
                                )
                                if (!file.exists()) {
                                    downloadableFiles.add(
                                        MediaFile(
                                            page.backUrl,
                                            Utilities.getFileNameWithExtension(page.backUrl)!!
                                        )
                                    )
                                }
                            }
                            for (zone in page.zone) {
                                val isDocument = (zone.zoneType.equals(ZoneType.ZONE_DOCX))
                                        //&& (Utilities.getFileExtension(zone.zoneDataName!!).equals("docx") || Utilities.getFileExtension(zone.zoneDataName!!).equals("xlsx"))
                                if (zone.zoneType.equals(ZoneType.ZONE_VIDEO) ||
                                    zone.zoneType.equals(ZoneType.ZONE_IMAGE) ||
                                    zone.zoneType.equals(ZoneType.ZONE_AUDIO) ||
                                    zone.zoneType.equals(ZoneType.ZONE_PDF) ||
                                    isDocument) {
                                    var file = File(destinationDir, zone.zoneDataName)
                                    if(isDocument){
                                        val filesDir = context.getExternalFilesDir("DownloadedFiles")
                                        if (filesDir == null || (!filesDir.exists() && !filesDir.mkdirs())) {
                                            throw IOException("Failed to create directory for downloads")
                                        }
                                        file = File(filesDir, zone.zoneDataName)
                                    }
                                    if (!file.exists()) {
                                        downloadableFiles.add(
                                            MediaFile(
                                                zone.imgUrl,
                                                zone.zoneDataName!!
                                            )
                                        )
                                    }
                                } else if (zone.zoneType.equals(ZoneType.ZONE_PLAYLIST)) {
                                    for (media in zone.playlist.media) {
                                        var file = File(destinationDir, media.name)
                                        val isDocument = (media.type.equals(MediaType.DOCX) || media.type.equals(MediaType.PDF) )

                                        if(isDocument){
                                            val filesDir = context.getExternalFilesDir("DownloadedFiles")
                                            if (filesDir == null || (!filesDir.exists() && !filesDir.mkdirs())) {
                                                throw IOException("Failed to create directory for downloads")
                                            }
                                            file = File(filesDir, media.name)
                                        }
                                        if (!file.exists()) {
                                            if (media.type in setOf(
                                                    MediaType.VIDEO,
                                                    MediaType.AUDIO,
                                                    MediaType.IMAGE,
                                                    MediaType.PDF)
                                                    || isDocument


                                            ) {
                                                downloadableFiles.add(
                                                    MediaFile(
                                                        media.url,
                                                        media.name
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (downloadableFiles.size > 0) {
                    DownloadFileTask(context).execute(downloadableFiles)
                    return true
                }
            }
            return false
        }

        fun getFileNameWithExtension(url: String): String? {
            return Uri.parse(url).lastPathSegment
        }

        fun sortZonesByZIndex(zones: List<Zone>): List<Zone> {
            return zones.sortedBy { it.zindex }
        }

        fun findPageIndex(pages: List<Page>, targetPageId: String): Int {
            return pages.indexOfFirst { it.pageId == targetPageId }
        }
        @OptIn(UnstableApi::class)
        fun captureAndSaveVideoFrame(playerView: PlayerView?) {
            val player = playerView!!.player ?: return

            if (player.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                Toast.makeText(
                    playerView.context,
                    "Please wait for the video to load",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val videoSurface = playerView.videoSurfaceView as? android.view.TextureView ?: return

            // Capture the actual video frame
            val bitmap = Bitmap.createBitmap(
                videoSurface.width,
                videoSurface.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            videoSurface.draw(canvas)

            val filename = "video_frame_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val context = playerView.context
            val resolver = context.contentResolver

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri =
                        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val imagesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val image = File(imagesDir, filename)
                    FileOutputStream(image).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }

                Toast.makeText(context, "Video frame saved to Downloads", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save video frame", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                bitmap.recycle()
            }
        }
        @OptIn(UnstableApi::class)
        fun getEmptyImageMediaItem(
            lenght: String,
            context: Context,
            drawableResId: Int,
            mediaId: String
        ): MediaItem? {
            // Step 1: Get the drawable resource
            val drawable: Drawable? = ContextCompat.getDrawable(context, drawableResId)
            val durationInSeconds = try {
                lenght.toLong()
            } catch (e: NumberFormatException) {
                0L // Default to 0 if conversion fails
            }
            // Check if drawable is not null and is a BitmapDrawable
            if (drawable is BitmapDrawable) {
                // Step 2: Convert Drawable to Bitmap
                val bitmap: Bitmap = drawable.bitmap

                // Step 3: Save Bitmap to a temporary file
                val file = File(context.cacheDir, "black.png")
                try {
                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null // Handle error, return null
                }

                // Step 4: Create the MediaItem
                return MediaItem.Builder()
                    .setUri(Uri.fromFile(file))
                    .setMediaId(mediaId)
                    .setImageDurationMs(durationInSeconds * 1000)
                    .build()
            }

            return null // Return null if drawable is not valid
        }
        fun rearrangeZones(zones: List<Zone>): List<Zone> {
            // Separate zones into two lists: one for zoneType 9 and one for the rest
            val zoneTypeNine = zones.filter { it.zoneType == 9 }
            val otherZones = zones.filter { it.zoneType != 9 }

            // Combine the lists, placing zoneType 9 zones at the end
            return otherZones + zoneTypeNine
        }
        fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
            val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            return memoryInfo
        }

        fun takeScreenshot(bitmap: Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 10, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        fun canPlay(schedule: Schedule?, currentDateTime: String): Boolean {
            if (schedule == null) {
                return true // If there's no schedule, we assume it can always play
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val currentDate: Date = dateFormat.parse(currentDateTime) ?: return false
            val calendar = Calendar.getInstance().apply { time = currentDate }

            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val currentTimeMillis = calendar.timeInMillis

            try {
                // Parse start and end dates
                val startDate = dateFormat.parse(schedule.start)
                val endDate = dateFormat.parse(schedule.end)

                // Check if DateRange is true
                val isWithinDateRange = if (schedule.dateRange) {
                    currentTimeMillis in startDate.time..endDate.time
                } else {
                    true // If DateRange is false, always consider it within range
                }

                // Check if current day of week is scheduled
                val isScheduledDay = when (currentDayOfWeek) {
                    Calendar.MONDAY -> schedule.monday
                    Calendar.TUESDAY -> schedule.tuesday
                    Calendar.WEDNESDAY -> schedule.wednesday
                    Calendar.THURSDAY -> schedule.thursday
                    Calendar.FRIDAY -> schedule.friday
                    Calendar.SATURDAY -> schedule.saturday
                    Calendar.SUNDAY -> schedule.sunday
                    else -> false
                }

                // Check if TimeRange is true
                val isWithinTimeRange = if (schedule.timeRange) {
                    val currentTimeCal = Calendar.getInstance().apply {
                        timeInMillis = currentTimeMillis
                        set(Calendar.YEAR, 1970)
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val scheduleStartTimeCal = Calendar.getInstance().apply {
                        time = startDate
                        set(Calendar.YEAR, 1970)
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val scheduleEndTimeCal = Calendar.getInstance().apply {
                        time = endDate
                        set(Calendar.YEAR, 1970)
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    currentTimeCal.timeInMillis in scheduleStartTimeCal.timeInMillis..scheduleEndTimeCal.timeInMillis
                } else {
                    true // If TimeRange is false, always consider it within time range
                }

                return isWithinDateRange && isScheduledDay && isWithinTimeRange
            } catch (e: Exception) {
                // Handle parsing errors if necessary
                e.printStackTrace()
                return false // Exclude items with parsing errors
            }
        }
        fun getNextMediaItems(mediaList: List<Media>, currentMediaId: String?, count: Int): List<Media> {
            // Handle empty list
            if (mediaList.isEmpty()) {
                return emptyList()
            }

            // Handle count greater than list length
            val itemCount = minOf(count, mediaList.size)

            // If currentMediaId is null or empty, return first n items
            if (currentMediaId.isNullOrEmpty()) {
                return mediaList.take(itemCount)
            }

            // Find the index of current media
            val currentIndex = mediaList.indexOfFirst { it.mediaID == currentMediaId }

            // If media ID not found, return first n items
            if (currentIndex == -1) {
                return mediaList.take(itemCount)
            }

            return buildList {
                var nextIndex = (currentIndex + 1) % mediaList.size
                repeat(itemCount) {
                    add(mediaList[nextIndex])
                    nextIndex = (nextIndex + 1) % mediaList.size
                }
            }
        }
        fun getFileExtension(fileName: String): String {
            return fileName.substringAfterLast('.', "")
        }

        fun filterMediaList(mediaList: List<Media>, currentDateTime: String): List<Media> {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val currentDate: Date = dateFormat.parse(currentDateTime) ?: Date()
            val calendar = Calendar.getInstance().apply { time = currentDate }

            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val currentTimeMillis = calendar.timeInMillis
            return mediaList.filter { media ->
                // Check expiry and length
                val expiryTime = media.expiryTime
                val mediaLength = media.length
                val isValidLength = !((mediaLength == null) || mediaLength == "0" || mediaLength.toLongOrNull() == null)
                val isNotExpired = when {
                    expiryTime == "0001-01-01T00:00:00" -> true
                    else -> {
                        val expiryDate: Date = dateFormat.parse(expiryTime) ?: Date(0)
                        expiryDate.after(currentDate)
                    }
                }

                // If the media is expired or has invalid length, filter it out
                if (!isValidLength || !isNotExpired) {
                    false
                } else {
                    // Check schedule
                    val schedule = media.schedule
                    if (schedule == null) {
                        true // Keep items with null schedule
                    } else {
                        try {
                            // Parse start and end dates
                            val startDate = dateFormat.parse(schedule.start)
                            val endDate = dateFormat.parse(schedule.end)

                            // Check if DateRange is true
                            val isWithinDateRange = if (schedule.dateRange) {
                                currentTimeMillis in startDate.time..endDate.time
                            } else {
                                true // If DateRange is false, always consider it within range
                            }

                            // Check if current day of week is scheduled
                            val isScheduledDay = when (currentDayOfWeek) {
                                Calendar.MONDAY -> schedule.monday
                                Calendar.TUESDAY -> schedule.tuesday
                                Calendar.WEDNESDAY -> schedule.wednesday
                                Calendar.THURSDAY -> schedule.thursday
                                Calendar.FRIDAY -> schedule.friday
                                Calendar.SATURDAY -> schedule.saturday
                                Calendar.SUNDAY -> schedule.sunday
                                else -> false
                            }

                            // Check if TimeRange is true
                            val isWithinTimeRange = if (schedule.timeRange) {
                                val currentTimeCal = Calendar.getInstance().apply {
                                    timeInMillis = currentTimeMillis
                                    set(Calendar.YEAR, 1970)
                                    set(Calendar.MONTH, Calendar.JANUARY)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                }
                                val scheduleStartTimeCal = Calendar.getInstance().apply {
                                    time = startDate
                                    set(Calendar.YEAR, 1970)
                                    set(Calendar.MONTH, Calendar.JANUARY)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                }
                                val scheduleEndTimeCal = Calendar.getInstance().apply {
                                    time = endDate
                                    set(Calendar.YEAR, 1970)
                                    set(Calendar.MONTH, Calendar.JANUARY)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                }
                                currentTimeCal.timeInMillis in scheduleStartTimeCal.timeInMillis..scheduleEndTimeCal.timeInMillis
                            } else {
                                true // If TimeRange is false, always consider it within time range
                            }

                            isWithinDateRange && isScheduledDay && isWithinTimeRange
                        } catch (e: Exception) {
                            // Handle parsing errors if necessary
                            e.printStackTrace()
                            false // Exclude items with parsing errors
                        }
                    }
                }
            }
        }
        fun getCurrentTimeForServer(): String {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            // Format the current date
            return dateFormat.format(calendar.time)
        }

        fun isPermissionGranted(context: Context):Boolean {
            return hasPermissions(context, *getPermissions()!!)
        }

        public fun getPermissions(): Array<String>? {
            var permissions: Array<String>? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 and above
                permissions = arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            }else {
                permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

            }
            return permissions
        }

        fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            permission!!
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            }
            return true
        }
        fun getDeviceModel(): String {
            return "${Build.MANUFACTURER} ${Build.MODEL}"
        }
        fun getOSVersion(): String {
            return Build.VERSION.RELEASE
        }
        fun getMacAddress(context: Context): String {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiManager.connectionInfo
                wifiInfo.macAddress ?: "Unavailable"
            } else {
                getMacAddressOnMarshmallow()
            }
        }
        fun getIpAdress(context: Context): String {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        }
        fun getDeviceResolution(display: Display): String {
            val p = Point()
            display.getRealSize(p)
            return p.x.toString() + " * " + p.y
        }
        fun getTimeZone(): String {
            val tz = TimeZone.getDefault()
            return tz.id
        }
        fun getMacAddressOnMarshmallow(): String {
            return try {
                val allInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (networkInterface in allInterfaces) {
                    if (!networkInterface.name.equals("wlan0", ignoreCase = true)) continue

                    val macBytes = networkInterface.hardwareAddress ?: return ""
                    val macAddress = StringBuilder()

                    for (b in macBytes) {
                        macAddress.append(String.format("%02X:", b))
                    }

                    if (macAddress.isNotEmpty()) {
                        macAddress.deleteCharAt(macAddress.length - 1)
                    }
                    return macAddress.toString()
                }
                "02:00:00:00:00:00" // Default return value if no valid MAC address found
            } catch (ex: Exception) {
                "02:00:00:00:00:00" // Return default value on exception
            }
        }
        fun getContentChannelLength(media: List<Media>): Double {
            return media.sumOf {
                it.length.toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
            }
        }

        fun isLicenceExpired(mPlayer: Player?): Boolean {
            if(mPlayer==null){
                return false
            }
            // Define the date format that matches the given string format
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            var licenceExpiry = mPlayer!!.licenseFinish
            // Parse the given time string into a Date object
            val parsedGivenTime: Date = format.parse(licenceExpiry)

            // Get the current time from the system
            val currentDeviceTime = Date(System.currentTimeMillis())

            // Compare the two Date objects
            return currentDeviceTime.after(parsedGivenTime)
        }
    }
}

