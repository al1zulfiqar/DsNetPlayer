package com.digitalsignage.dsnetcoreplayer


import WriteText
import android.Manifest
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.allViews
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.digitalsignage.dsnetcoreplayer.companion.AppRestarter
import com.digitalsignage.dsnetcoreplayer.constants.Constants
import com.digitalsignage.dsnetcoreplayer.constants.WebType
import com.digitalsignage.dsnetcoreplayer.constants.ZoneType
import com.digitalsignage.dsnetcoreplayer.data.DownloadComplete
import com.digitalsignage.dsnetcoreplayer.data.PlayerUpdatedEvent
import com.digitalsignage.dsnetcoreplayer.data.ViewInteraction
import com.digitalsignage.dsnetcoreplayer.data.db.DsNetDatabase
import com.digitalsignage.dsnetcoreplayer.data.player.Page
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerScheduleChecker
import com.digitalsignage.dsnetcoreplayer.data.player.Zone
import com.digitalsignage.dsnetcoreplayer.data.repositories.PlayerRepository
import com.digitalsignage.dsnetcoreplayer.data.requestdata.DeviceDetails
import com.digitalsignage.dsnetcoreplayer.fragment.ChannalFragment
import com.digitalsignage.dsnetcoreplayer.fragment.ImageFragment
import com.digitalsignage.dsnetcoreplayer.fragment.PDFViewerFragment
import com.digitalsignage.dsnetcoreplayer.fragment.ScreenshotEvent
import com.digitalsignage.dsnetcoreplayer.fragment.SimplePlaylistFragment
import com.digitalsignage.dsnetcoreplayer.fragment.VideoFragment
import com.digitalsignage.dsnetcoreplayer.fragment.WebFragment
import com.digitalsignage.dsnetcoreplayer.fragment.WordReaderFragment
import com.digitalsignage.dsnetcoreplayer.fragment.YoutubeFragment
import com.digitalsignage.dsnetcoreplayer.network.RetrofitClient
import com.digitalsignage.dsnetcoreplayer.schedulers.RebootScheduler
import com.digitalsignage.dsnetcoreplayer.services.MainAppPinger
import com.digitalsignage.dsnetcoreplayer.utils.CaptureScreenshot
import com.digitalsignage.dsnetcoreplayer.utils.NetworkUtils
import com.digitalsignage.dsnetcoreplayer.utils.RootUtil
import com.digitalsignage.dsnetcoreplayer.utils.ScreenshotService
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import com.digitalsignage.dsnetcoreplayer.viewmodels.MainViewModel
import com.digitalsignage.dsnetcoreplayer.viewmodels.PlayerViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.DataOutputStream
import java.io.IOException
import javax.inject.Singleton


class MainActivity : AppCompatActivity() {
    private var isPlayTimeNow:Boolean = true
    private var isScreenVisible: Boolean=false
    private var isExpiryScreenShowing: Boolean=false
    private var isHexScreenShowing: Boolean=false
    private var isPlayerActivated:Boolean=false
//    private lateinit var screenStateReceiver: ScreenStateReceiver
    private lateinit var intentFilter: IntentFilter
    private var appsPermissionsGranted: Boolean?=false
    private var mPlayer: Player? = null
    private val TAG = "MainActivityTag"
    private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 12
    private var contactServerTime = 60000L
    private val mainScope = MainScope()
    public lateinit var mainContainer: RelativeLayout
    private var playlistIndex: Int = 0
    private var playlistId:String=""
    private var pageIndex: Int = 0
    private var pinger: MainAppPinger? = null
    public lateinit var playerViewModel: MainViewModel


    private var countdownJob: Job? = null
    private var screenGrabJob :Job?=null
    private lateinit var playerViews: MutableList<PlayerView>
    private lateinit var testBtn:Button
    private var screenTimeLogJob: Job? = null
    private var screenTimeLogsCount = 0
    private lateinit var screenshotService: ScreenshotService
    companion object {
        private val REQUEST_CODE = 1000
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        mainContainer = findViewById(R.id.rootView)
        testBtn = findViewById(R.id.Test)
        testBtn.setOnClickListener { myTestButton() }

        playerViews= mutableListOf()
        //setupScreenOnReceiver()
        val playerDao = DsNetDatabase.getDatabase(this).playerDao()
        val retrofitClient = RetrofitClient.create(Constants.DSNET_BASE_URL)
        val playerRepository = PlayerRepository(playerDao, retrofitClient)
        playerViewModel = PlayerViewModelFactory(application,playerRepository).create(MainViewModel::class.java)
        // Service to keep Player Always running
       // ServiceStarter.startService(this)

        //startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
        getSuPermission()
        checkPermissions()
        pinger = MainAppPinger(this)
        screenshotService = ScreenshotService(this)
    }

    private fun getSuPermission() {
        try {
            // Perform su to get root privileges
            val process = Runtime.getRuntime().exec("su")

            // Attempt to write a file to a root-only directory
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n")

            // Close the terminal
            os.writeBytes("exit\n")
            os.flush()
        } catch (e: IOException) {
            // TODO Code to run in input/output exception
            // Toast.makeText(this, "not root IOException", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupScreen() {
        // Hide the action bar
        supportActionBar?.hide()
        // Hide the status bar and navigation bar for older versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {

            val windowInsetsController = window.insetsController
            if (windowInsetsController != null) {
                windowInsetsController.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    private fun checkPermissions() {
        if (Utilities.isPermissionGranted(this@MainActivity)) {
            appsPermissionsGranted=true
            init()
        } else {
            ActivityCompat.requestPermissions(
                this,
                Utilities.getPermissions()!!,
                READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }



    fun init() {
        Utilities.checkUUID(this)
        Utilities.setupAppVersion(this)
        startPeriodicApiCalls()
        playerViewModel.responseString.observe(this, { response ->
            when (response) {
                "Register" -> loadBackground(null)
                "Player License Expired"-> showLicenceExpiredScreen()
                "Playlist" -> playerViewModel.refreshPlaylist(Utilities.AppUUID)
                "Nothing" -> checkIfPlayerDataExists()
                "Error" -> showToast() // Implement your own logic for other responses
            }
        })
    }

    private fun showLicenceExpiredScreen() {
        if(!isExpiryScreenShowing) {
            destroyCurrentUI()
            val imageFragment = getImageFragment("", "expired.png", false, 0.0)
            loadFragment(imageFragment, true, null, "expiry")
            isExpiryScreenShowing = true
            isPlayerActivated=false
        }
    }

    private fun firstLaunchSetup() {
            CoroutineScope(Dispatchers.Main).launch {
                val player = playerViewModel.getPlayerById(Utilities.AppUUID)
                if(allowedToPlay(player)) {
                    if (player != null) {
                        isPlayerActivated = true
                        logLaunchTimeWithSameEndTime()
                        setupPlayer(player)
                        Utilities.startDownloadMediaIfNeeded(player, this@MainActivity)
                    } else {
                        loadBackground(null)
                    }
                }
            }

    }

    private fun allowedToPlay(player: Player?): Boolean {
        if(player!=null){
            if(player!!.playerUpTimes==null || player!!.playerUpTimes.size==0 || PlayerScheduleChecker.isPlayerAllowedToPlayNow(player!!.playerUpTimes)){
                if(!isPlayTimeNow){
                    isPlayTimeNow = true
                    firstLaunchSetup()
                }else {
                    isPlayTimeNow = true
                }
            }else if(!PlayerScheduleChecker.isPlayerAllowedToPlayNow(player!!.playerUpTimes)) {
                isPlayTimeNow = false
                destroyCurrentUI()
            }
        }
        return  isPlayTimeNow
    }

    private fun logLaunchTimeWithSameEndTime() {
        val currentTime = Utilities.getCurrentDateTime()
        WriteText.writeToTextFile("screen_time_"+
                Utilities.getAppVersion(this@MainActivity)+ ".txt", this@MainActivity, "ScreenStartTime:",currentTime,false)
        WriteText.writeToTextFile("screen_time_"+
                Utilities.getAppVersion(this@MainActivity)+ ".txt", this@MainActivity, "ScreenEndTime:",currentTime, false)
        startScreenTimeLogJob()
    }

    private fun checkIfPlayerDataExists() {
        CoroutineScope(Dispatchers.Main).launch {
            val player = playerViewModel.getPlayerById(Utilities.AppUUID)
            if (player == null) {
                loadBackground(null)
            }
        }
    }

    private fun fetchAndSetupPlayer() {
        CoroutineScope(Dispatchers.Main).launch {
            val player = playerViewModel.getPlayerById(Utilities.AppUUID)
            if (player != null) {
                if(allowedToPlay(player)) {
                    setupPlayer(player)
                }
            } else {
                loadBackground(null)
            }
        }
    }

    private fun startCountdown(countdownTime:Long) {
        countdownJob = CoroutineScope(Dispatchers.Main).launch {
                delay(countdownTime*1000)
                pageIndex = 0
                destroyCurrentUI()
                startPlayListIfNotEmpty(mPlayer!!)
        }
    }
    private fun cancelCountDown() {
        if(countdownJob!=null) {
            countdownJob?.cancel()
        }

    }
    private fun setupPlayer(player: Player) {
        PlayerManager.setPlayer(player)
        mPlayer = player
        RebootScheduler(this).scheduleReboot(mPlayer!!.rebootTime)
        playlistIndex = 0
        PlayerManager.setChannalContentNextIndex(0)
        contactServerTime = player.connectionInterval * 60000
        startPlayListIfNotEmpty(player)
        //startScreenshotService(player)
    }

    private fun startPlayListIfNotEmpty(player: Player) {
        if (player.playlist.size > 0 && player.playlist != null && player.playlist.size > 0) {
            playlistId = PlayerManager.getPlayer()!!.playlist[playlistIndex].playlistID
            handlePlaylist()
        } else {

        }
    }

    private fun handlePlaylist() {
        if(Utilities.isLicenceExpired(mPlayer)){
            showLicenceExpiredScreen()
            return
        }
        if(mPlayer!=null) {
            val currentPlaylist = mPlayer!!.playlist[playlistIndex]
            if (currentPlaylist.page!!.size > 0) {
                val currentPage = currentPlaylist.page[pageIndex]
                loadBackground(currentPage)
                val pageZones = Utilities.sortZonesByZIndex(currentPage.zone)
                Utilities.setupDeviceResolutionVariables(this)
                Utilities.setupPageResolution(currentPage)
                if (pageZones.size > 0) {
                    for (zone in pageZones) {
                        handleZone(zone)
                    }
                }
            } else {
                val channelContentFragmentFull = ChannalFragment.newInstance(playlistIndex, null, "", true)
                    loadFragment(channelContentFragmentFull, true, null, "c_content" + mPlayer!!.playlist[playlistIndex].playlistID
                )
            }
        }
    }



    private fun handleZone(zone: Zone) {
        var interaction: String = ""
        if (zone.interaction != null && zone.interaction.size > 0) {
            interaction =
                zone.interaction.get(0)?.gesture + "," + zone.interaction.get(0)!!.targetPageId + "," +
                        zone.interaction.get(0)!!.action
        }else if(pageIndex>0){
            interaction = "touch,0,refresh_timer"
        }
        var fragment: Fragment?=null
        var tag=""
         if (zone.zoneType == ZoneType.ZONE_IMAGE) {
            fragment = ImageFragment().apply {
                arguments = Bundle().apply {
                    putString("url", zone.imgUrl)
                    putString("fileName", zone.zoneDataName!!)
                    putBoolean("aspectRatio", false)
                    putString("interaction", interaction)
                    putString("mediaID", zone.itemId)
                    putString("playlistId", playlistId)
                    putBoolean("reportPlayback", zone.reportPlayback ?: false)
                }

            }
            tag = "image_" + zone.pageZoneId
        } else if (zone.zoneType == ZoneType.ZONE_VIDEO || zone.zoneType == ZoneType.ZONE_AUDIO) {

             fragment = VideoFragment().apply {
                 arguments = Bundle().apply {
                     putString("video_url", zone.imgUrl)
                     putString("file_name", zone.zoneDataName!!)
                     putString("interaction", interaction)
                     putString("mediaID", zone.itemId)
                     putString("playlistId", playlistId)
                     putBoolean("reportPlayback", zone.reportPlayback ?: false)
                     putBoolean("playNow",true)
                 }
             }

            tag = "video_" + zone.pageZoneId
        }else if(zone.zoneType ==ZoneType.ZONE_WIDGET && zone.widget!=null && zone.widget.widgetType== SimplePlaylistFragment.WidgetType.YOUTUBE){
            if(zone.widget!=null && zone.widget.widgetParam!=null &&  zone.widget.widgetParam!!.size>0) {
                val videoIdItem = zone.widget?.widgetParam?.find { it.paramName == "videoid" }
                val videoCcItem = zone.widget?.widgetParam?.find { it.paramName == "videocc" }

                fragment = YoutubeFragment().apply {
                    arguments = Bundle().apply {
                        if (videoIdItem != null) {
                            putString("youtube_video_id", videoIdItem.value ?:"")
                        }
                        if (videoCcItem != null) {
                            putString("youtube_can_cc",  if (videoCcItem.value.toBoolean()) "1" else "0")
                        }
                        putString("interaction", interaction)
                        putString("mediaID", zone.itemId)
                        putString("playlistId", playlistId)
                        putBoolean("reportPlayback", zone.reportPlayback ?: false)
                        putBoolean("playNow",true)
                    }
                }
            }
            tag = "youtube_"+zone.pageId
        }
        else if(zone.zoneType == ZoneType.ZONE_DOCX ){
             fragment = WordReaderFragment().apply {
                 arguments = Bundle().apply {
                     putString("docType", zone.zoneType.toString())
                     putString("docName", zone.zoneDataName)
                     putString("url", zone.imgUrl)
                 }
             }
        }else if(zone.zoneType==ZoneType.ZONE_PDF){
             fragment = PDFViewerFragment().apply {
                 arguments = Bundle().apply {
                     putString("docType", zone.zoneType.toString())
                     putString("docName", zone.zoneDataName)
                     putString("url", zone.imgUrl)
                 }
             }
         }
        else if (zone.zoneType == ZoneType.ZONE_HTML || zone.zoneType==ZoneType.ZONE_WIDGET || zone.zoneType==ZoneType.ZONE_TEXT_AREA) {
            var webType =WebType.WEB_URL.value
            var url = zone.imgUrl
            var isTransparent = false


             if(zone.zoneType==ZoneType.ZONE_WIDGET){
                url = zone.widget.widgetURL!!
                isTransparent = true

            }else if(zone.zoneType==ZoneType.ZONE_TEXT_AREA){
                url = zone.simpleTextAndroid!!
                webType = WebType.HTML.value
                isTransparent=true
            }
             fragment = WebFragment().apply {
                 arguments = Bundle().apply {
                     putInt("playlistIndex", playlistIndex)
                     putInt("webType", webType)
                     putSerializable("url", url)
                     putString("interaction", interaction)
                     putBoolean("isTransparent", isTransparent)
                     putBoolean("cacheable", zone.allowCache == true)
                     putString("mediaID", zone.itemId)
                     putString("playlistId", playlistId)
                     putBoolean("reportPlayback", zone.reportPlayback ?: false)
                     putBoolean("loadNow", true)
                 }
             }
            tag = "web_" + zone.pageZoneId
       // }else if (zone.zoneType == ZoneType.ZONE_SCHEDULE || zone.zoneType == ZoneType.ZONE_PLAYLIST) {
         }else if ( zone.zoneType == ZoneType.ZONE_PLAYLIST) {
             fragment = SimplePlaylistFragment.newInstance(playlistIndex, zone, interaction, false)
                 val fragmentTag = "playlist"
                tag = fragmentTag + zone.pageZoneId
         }else if(zone.zoneType == ZoneType.ZONE_SCHEDULE){
             fragment = ChannalFragment.newInstance(playlistIndex, zone, interaction, false)
             var fragmentTag = "c_content"
             tag = fragmentTag + zone.pageZoneId
         }
        if(fragment!=null) {
            loadFragment(
                fragment!!,
                false,
                Utilities.getScaledZoneDimensions(zone),
                tag
            )
        }
    }
    private fun showToast() {
        Log.d(TAG, "Current-Playlist")
    }

    private fun loadBackground(page: Page?) {
        if (page == null && !isHexScreenShowing ) {
            playerViewModel.deleteAllPlayers()
            PlayerManager.clearPlayer()
            mPlayer=null
            destroyCurrentUI()
            val imageFragment = getImageFragment( Constants.DSNET_BASE_URL + Constants.REGISTER_URL + Utilities.AppUUID, "hex.jpg", false, 0.0)
            loadFragment(imageFragment, true, null, "hex_code")
            isHexScreenShowing =true
            isPlayerActivated=false

        }else if(page !=null && page.backgroundColor!=null && !page.backgroundColor.equals("null")){
            mainContainer.setBackgroundColor(Color.parseColor(page.backgroundColor))
        }
        else if (page!=null && page.backgroundImageMed != null && FilenameUtils.getExtension(page.backUrl).equals("mov")) {

        }
        else if (page!=null && page.backgroundImageMed != null) {
            val imageFragment = getImageFragment(page.backUrl!!, page.backUrl!!, false, 0.0)
            loadFragment(imageFragment, true, null, "background")
        }

    }

    private fun getImageFragment(url:String, fileName:String, aspectRatio:Boolean, durationLDouble: Double): Fragment {
        val imageFragment = ImageFragment().apply {
            arguments = Bundle().apply {
                putString("url",url )
                putString(
                    "fileName",
                    Utilities.getFileNameWithExtension(fileName)
                )
                putBoolean("aspectRatio", aspectRatio)
                putDouble("duration", durationLDouble)
            }
        }
        return imageFragment
    }

    private fun removeFragmentByTag(tag: String) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val existingFragment = fragmentManager.findFragmentByTag(tag)

        existingFragment?.let {
            val transaction = fragmentManager.beginTransaction()
            transaction.remove(it)
            transaction.commitAllowingStateLoss()
        }
    }

    private fun loadFragment(
        fragment: Fragment,
        isFullScreen: Boolean,
        dimensions: List<Int>?,
        tag: String
    ) {
        //check if Playlist Scheduled
        if(mPlayer!=null && !Utilities.canPlay(mPlayer!!.playlist[playlistIndex].schedule, Utilities.getCurrentDateTime())){
            playlistEnded()
            return
        }
        val fragmentManager: FragmentManager = supportFragmentManager
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        var requiredLayoutParams: FrameLayout.LayoutParams = if (isFullScreen) {
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        } else {
            FrameLayout.LayoutParams(dimensions!![2], dimensions[3]).apply {
                leftMargin = dimensions[0]
                topMargin = dimensions[1]
            }
        }
        val container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = requiredLayoutParams
        }
        if (mainContainer != null) {
            mainContainer.addView(container)
            // Replace fragment in the container
            if (mainContainer.findViewById<FrameLayout>(container.id) != null ) {
                transaction.replace(container.id, fragment, tag)
                transaction.commitAllowingStateLoss()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            val askedPermissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                // Manifest.permission.ACCESS_FINE_LOCATION
            )

            if (Utilities.isPermissionGranted(this)) {
                appsPermissionsGranted=true
                init()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Permissions Error")
                    .setMessage("DSNet needs all the necessary permissions to function properly. Please go to settings and grant the permissions, then relaunch the app.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .show()
            }
        }

    }

    private fun startPeriodicApiCalls() {
        if(screenGrabJob!=null){
            screenGrabJob!!.cancel()
        }
        mainScope.launch {
            while (isActive) {
                contactServer()
                postDeviceDetails()
                delay(contactServerTime)
                allowedToPlay(mPlayer)
            }
        }
    }

    private fun postDeviceDetails() {
        if(mPlayer!=null) {
            val deviceDetails = DeviceDetails(
                mPlayer!!.playerID,
                Utilities.getDeviceModel(),
                Utilities.getOSVersion(),
                Utilities.getMacAddress(this@MainActivity),
                Utilities.getTimeZone(),
                Utilities.getDeviceResolution((this as AppCompatActivity).windowManager.defaultDisplay),
                Utilities.getIpAdress(this)
            )
            playerViewModel.uploadDeviceDetails(deviceDetails)
        }
    }

    private suspend fun contactServer() {
        if(Utilities.isLicenceExpired(mPlayer)){
            showLicenceExpiredScreen()
        }
        withContext(Dispatchers.IO) {
            if(isScreenVisible) {
                pinger!!.sendPing()
            }
            playerViewModel.contactServer(Utilities.AppUUID, Utilities.versionNameFromPackage)
        }

    }


    override fun onResume() {
        super.onResume()
        setupScreen()
        destroyCurrentUI()

        if(appsPermissionsGranted == true) {
            firstLaunchSetup()

        }
        isExpiryScreenShowing=false
        isHexScreenShowing = false
        isScreenVisible = true
        EventBus.getDefault().register(this)

    }
    override fun onPause() {
        super.onPause()
        //destroyCurrentUI()
//        PlayerManager.setChannalContentNextIndex(0)
        EventBus.getDefault().unregister(this)
        writeTimeLogsIfAllowed()
        isScreenVisible = false
        screenshotService.stop()
    }

    private fun writeTimeLogsIfAllowed() {
        if(isPlayerActivated==true && appsPermissionsGranted==true) {
            stopScreenTimeLogJob()
            WriteText.writeToTextFile(
                "screen_time_" + Utilities.getAppVersion(this) + ".txt",
                this,
                "ScreenEndTime:",
                Utilities.getCurrentDateTime(),
                true
            )
            if(NetworkUtils.isInternetAvailable(this)  &&
                PlayerManager.getPlayer()?.reportUptime !=null &&
                PlayerManager.getPlayer()?.reportUptime == true){
                val playerRunTimeLogs = PlayerManager.parseScreenTimes(WriteText.readFromTextFile("screen_time_"+Utilities.getAppVersion(this)+ ".txt", this),
                    mPlayer!!.playerID)
                playerViewModel.postRunTimeData(playerRunTimeLogs, false)
            }
        }
    }

    private fun startScreenTimeLogJob() {
        screenTimeLogJob = lifecycleScope.launch {
            while (isActive) {
                delay(60 * 1000)
                WriteText.writeToTextFile(
                    "screen_time_" + Utilities.getAppVersion(this@MainActivity) + ".txt",
                    this@MainActivity,
                    "ScreenEndTime:",
                    Utilities.getCurrentDateTime(),

                    true
                )
                screenTimeLogsCount += 1
                if (screenTimeLogsCount > 4) {
                    screenTimeLogsCount = 0
                    if (NetworkUtils.isInternetAvailable(this@MainActivity) &&
                        PlayerManager.getPlayer()?.reportUptime !=null &&
                        PlayerManager.getPlayer()?.reportUptime == true
                    ) {
                        val playerRunTimeLogs = PlayerManager.parseScreenTimes(
                            WriteText.readFromTextFile(
                                "screen_time_" + Utilities.getAppVersion(this@MainActivity) + ".txt",
                                this@MainActivity
                            ),
                            mPlayer!!.playerID
                        )
                        playerViewModel.postRunTimeData(playerRunTimeLogs,true)
                    }
                }
            }
        }
    }
    private fun stopScreenTimeLogJob() {
        screenTimeLogJob?.cancel() // Cancel the job to stop the coroutine
        screenTimeLogJob = null
        screenTimeLogsCount=0
    }
    fun playlistEnded() {
        if(mPlayer!=null) {
            destroyCurrentUI()
            updatePlaylistIndex()
            handlePlaylist()
        }
    }

    public fun updatePlaylistIndex():Int {
        playlistIndex++
        if (playlistIndex >= mPlayer!!.playlist.size) {
            playlistIndex = 0
        }
        return playlistIndex
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCountDown()
        mainScope.cancel()
        if(countdownJob!=null){
            countdownJob!!.cancel()
        }
        screenGrabJob?.cancel()
    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Lifecycle-OnStop")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageInteraction(event: ScreenshotEvent) {

        if (NetworkUtils.isInternetAvailable(this)) {
            // Launch a coroutine in the Main scope
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val captureScreenshot = CaptureScreenshot(mainContainer, this@MainActivity)
                    captureScreenshot?.takeAndUploadScreenShot(
                        mPlayer!!.accountID,
                        mPlayer!!.playerID
                    )
                } catch (e: Exception) {
                    // Handle any errors that might occur during screenshot capture/upload
                    Log.e("Screenshot", "Error taking/uploading screenshot", e)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageInteraction(event: ViewInteraction) {
        if(mPlayer!=null && !event.pageID.equals("0")) {
            pageIndex =
                Utilities.findPageIndex(mPlayer!!.playlist[playlistIndex].page!!, event.pageID)
           moveToHomePage()
        }else if (event.pageID.equals("0")){
            restartPageTimer()
        }
    }

    private fun moveToHomePage() {

        if (pageIndex > 0) {
            restartPageTimer()
            //startPlayListIfNotEmpty(mPlayer!!)
        } else {
            cancelCountDown()
            //fetchAndSetupPlayer()
        }
        destroyCurrentUI()
        startPlayListIfNotEmpty(mPlayer!!)
    }

    private fun restartPageTimer() {
        cancelCountDown()
        val timeout = mPlayer!!.playlist[playlistIndex].collectionTimeOut
        startCountdown(timeout)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadComplete(event: DownloadComplete){
        giveAppFreshStart()
    }

    private fun giveAppFreshStart() {
        setupScreen()
        removeFragmentByTag("hex_code")
        destroyCurrentUI()
        fetchAndSetupPlayer()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayerUpdated(event: PlayerUpdatedEvent) {
        if(event!=null && event.player!=null ){
            if(event.player.softRestart){
                AppRestarter.restartAppWithIntentFlags(this,5000)
            }else {
                if (event.player.playlist != null && event.player.playlist.size > 0) {
                    isExpiryScreenShowing=false
                    isHexScreenShowing = false
                    if (Utilities.startDownloadMediaIfNeeded(event.player, this)) {
                        //destroyCurrentUI()
                    } else {
                        destroyCurrentUI()
                        fetchAndSetupPlayer()
                    }
                }
            }
        }
    }
    fun destroyCurrentUI(){
        mainContainer.removeAllViews()
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val fragments = fragmentManager.fragments
        for (fragment in fragments) {
            fragmentTransaction.remove(fragment).commitNow()
        }
        System.gc()
        if (!fragmentManager.isStateSaved){
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    fun myTestButton() {
        if (RootUtil.isDeviceRooted() && RootUtil.isRootPermissionGranted()) {
            try {
                Runtime.getRuntime().exec(arrayOf("/system/bin/su", "-c", "reboot now"))
            } catch (e: IOException) {
                try {
                    Runtime.getRuntime().exec(arrayOf("/system/xbin/su", "-c", "reboot now"))
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
                //                e.printStackTrace();
            }
        }
    }
    fun startScreenshotService(player: Player) {
        screenshotService.start(player)

//        if(mPlayer!=null && mPlayer!!.accountID!=null && mPlayer!!.playerID!=null && NetworkUtils.isInternetAvailable(this)) {
//            val captureScreenshot = CaptureScreenshot(mainContainer, this)
//            captureScreenshot.takeAndUploadScreenShot(mPlayer!!.accountID, mPlayer!!.playerID)
//        }
    }


}