package com.digitalsignage.dsnetcoreplayer.fragment


import WriteText
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.digitalsignage.dsnetcoreplayer.MainActivity
import com.digitalsignage.dsnetcoreplayer.R
import com.digitalsignage.dsnetcoreplayer.constants.LogType
import com.digitalsignage.dsnetcoreplayer.constants.MediaType
import com.digitalsignage.dsnetcoreplayer.constants.WebType
import com.digitalsignage.dsnetcoreplayer.constants.ZoneType
import com.digitalsignage.dsnetcoreplayer.data.player.Media
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.data.player.Playlist
import com.digitalsignage.dsnetcoreplayer.data.player.Zone
import com.digitalsignage.dsnetcoreplayer.listeners.CustomGestureListener
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


private var imageReader: ImageReader? = null
class SimplePlaylistFragment: Fragment() {
    private lateinit var mPlayer: com.digitalsignage.dsnetcoreplayer.data.player.Player
    private var mediaShowTime=""
    private val TAG = "SimplePlaylist"
    private lateinit var mRunningPlaylist:Playlist
    private var totalPlaylists:Int=0
    private lateinit var mainContainer:RelativeLayout
    private var handler: Handler? = null
    private var resumeRunnable: Runnable? = null
//    private var currentIndexMedia=0
    private var playerView: PlayerView? = null
    private var player: ExoPlayer? = null
    private lateinit var mRunningMediaList:List<Media>
    private lateinit var supportLayout:FrameLayout
    private val playlistItemDurationHandler = Handler(Looper.getMainLooper())
    private lateinit var fadeView: View
    private lateinit var testBtn:Button
    private var animationHandler: Handler? = null
    private var animationRunnable: Runnable? = null
    private lateinit var lastFrameImageView: ImageView

    private var isMediaListAggrigated = false
    object WidgetType {
        const val YOUTUBE: String="21"
    }
    companion object {
        private const val ARG_URL = "playlistIndex"
        private const val ARG_ZONE = "zone"
        private const val ARG_INTERACTION = "interaction"
        private const val ARG_FULL_SCREEN = "full_screen"
        fun newInstance( playlistIndex:Int, zone: Zone?, interaction: String?, isFullScreen:Boolean): SimplePlaylistFragment {
            val fragment = SimplePlaylistFragment()
            val args = Bundle().apply {
                putInt(ARG_URL,playlistIndex)
                putSerializable(ARG_ZONE, zone)
                putString(ARG_INTERACTION, interaction)
                putBoolean(ARG_FULL_SCREEN, isFullScreen)
            }
            fragment.arguments = args
            return fragment
        }
    }
    private var playlistIndex: Int = 0
    private var zone: Zone? = null
    private  var interaction:String?=""
    private var isChannalFullScreen:Boolean=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistIndex = it.getInt(ARG_URL)!!
            if(it.getSerializable(ARG_ZONE)!=null){
                zone  = (it.getSerializable(ARG_ZONE) as Zone?)!!
            }
            interaction= it.getString(ARG_INTERACTION)!!
            isChannalFullScreen= it.getBoolean(ARG_FULL_SCREEN)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_channel_content, container, false)
        mainContainer = view.findViewById(R.id.rootView)
        playerView = view.findViewById(R.id.player_view1)
        supportLayout = view.findViewById<FrameLayout>(R.id.webView_layout)
        fadeView = view.findViewById<View>(R.id.fade_view)
        lastFrameImageView = view.findViewById(R.id.last_frame_image_view)

        testBtn = view.findViewById<Button>(R.id.testbtn)

        testBtn.setOnClickListener {
            //Utilities.captureAndSaveVideoFrame(playerView)


        }
        initializeData()
        return view
    }
    fun animatePlayer() {
           if(playerView!=null) {
               playerView!!.visibility = View.VISIBLE
               playerView!!.scaleX = 1f
               playerView!!.scaleY = 1f

               // Create slide-out animation
               val slideOut = ObjectAnimator.ofFloat(
                   playerView,
                   View.TRANSLATION_X,
                   0f,
                   playerView!!.width.toFloat()
               )
               slideOut.duration = 1000 // 1 second

               // Create scale animation
               val scaleX = ObjectAnimator.ofFloat(playerView, View.SCALE_X, 0f, 1f)
               val scaleY = ObjectAnimator.ofFloat(playerView, View.SCALE_Y, 0f, 1f)
               val scaleAnimSet = AnimatorSet().apply {
                   playTogether(scaleX, scaleY)
                   duration = 100 // 0.5 seconds
               }

               // Combine animations
               AnimatorSet().apply {
                   playSequentially(slideOut, scaleAnimSet)
                   interpolator = AccelerateDecelerateInterpolator()
                   addListener(object : android.animation.AnimatorListenerAdapter() {
                       override fun onAnimationStart(animation: android.animation.Animator) {
                           if (playerView != null) {
                               playerView!!.visibility = View.VISIBLE
                           }
                       }

                       override fun onAnimationEnd(animation: android.animation.Animator) {
                           if (playerView != null) {
                               playerView!!.translationX = 0f
                           }
                       }
                   })
                   start()
               }
           }
    }
    private fun fadeIn(view: View) {
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 0.3f)
        fadeIn.duration = 1000 // Fade in duration
        fadeIn.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                fadeOut(view) // Start fade out after fade in ends
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })
        fadeIn.start()
    }

    private fun fadeOut(view: View) {
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 0f)
        fadeOut.duration = 1000 // Fade out duration
        fadeOut.start()
    }
    private fun setupInteraction() {
        if(interaction!=null && !interaction!!.isEmpty()){
            val interactionData = interaction!!.split(",")
            val gestureDetector = GestureDetector(requireContext(), CustomGestureListener(interactionData))
            playerView!!.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
    }
    private fun initializeData() {
        mPlayer  = PlayerManager.getPlayer()!!
        mRunningPlaylist = mPlayer.playlist[playlistIndex]
        totalPlaylists = mPlayer.playlist.size
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteraction()
        checkNullPlaylist()
    }

    private fun checkNullPlaylist() {
        if (mRunningPlaylist!=null) {
            Log.d(TAG, "player-updated")
            initPlayerLoadPlaylist()
        } else {
            Utilities.showToast(requireContext(), "No Playlist")
        }
    }

    @OptIn(UnstableApi::class)
    fun initPlayerLoadPlaylist(){
        player = ExoPlayer.Builder(requireContext()).build()

        playerView?.player = player

        mRunningMediaList = emptyList<Media>()
        if(zone==null || zone!!.zoneType == ZoneType.ZONE_SCHEDULE) {
            prepareMediaListForPageChannalContent()
        }else {
            prepareMediaPlaylistForPagePlaylist()
        }
        if(mRunningMediaList==null|| mRunningMediaList.size==0){
            loadNextPlaylistIfNeeded()
            return
        }
        player!!.setMediaItems(makeMediaItemsList())
        playMedia()
    }

    private fun prepareMediaPlaylistForPagePlaylist() {
        player!!.repeatMode = Player.REPEAT_MODE_ALL
        mRunningMediaList= zone!!.playlist.media
        if(zone!!.playlist.shuffle){
            mRunningMediaList = mRunningMediaList.shuffled()
        }
        mRunningMediaList = Utilities.filterMediaList(mRunningMediaList.toMutableList(), Utilities.getCurrentDateTime())
    }

    private fun prepareMediaListForPageChannalContent() {
        if(totalPlaylists==1){
            player!!.repeatMode = Player.REPEAT_MODE_ALL
            setSingleMediaList()
        }else {
            val aggrigatedMediaList = PlayerManager.aggregateMediaFromPlaylists(mPlayer.playlist)

            if (aggrigatedMediaList != null && aggrigatedMediaList.size > 0) {
                mRunningMediaList = aggrigatedMediaList
                mRunningMediaList.forEach { media ->
                    Log.d(TAG, "Playlist new Media ID: ${media.mediaID}")
                }
                isMediaListAggrigated =true
                //player!!.repeatMode = Player.REPEAT_MODE_ALL
            } else {
                setSingleMediaList()
            }
        }
        mRunningMediaList = Utilities.filterMediaList(
            mRunningMediaList.toMutableList(),
            Utilities.getCurrentDateTime())
    }
    private fun setSingleMediaList() {
        mRunningMediaList = mRunningPlaylist.media

        if (mRunningPlaylist.maximumMedia > 0 && mRunningPlaylist.maximumMedia < mRunningMediaList.size) {
            mRunningMediaList = PlayerManager.getNextPlaylistItems(
                mRunningPlaylist.playlistID,
                mRunningPlaylist.media,
                mRunningPlaylist.maximumMedia
            )
        }
        if (mRunningPlaylist.shuffle) {
            mRunningMediaList = mRunningMediaList.shuffled()
        }
        isMediaListAggrigated =false
    }


    @OptIn(UnstableApi::class)
    private fun makeMediaItemsList():List<MediaItem> {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        for (media in mRunningMediaList) {
            if (media.fullScreen && !isChannalFullScreen) {
                mediaItems.add(handleFullScreenMedia(media)!!)
            } else {
                mediaItems.add(getMediaItem(media))
            }
        }
        return mediaItems
    }
    private fun handleFullScreenMedia(media: Media): MediaItem? {
        var mediaItem: MediaItem? = null
        if(media.type.equals(MediaType.VIDEO) ||
            media.type.equals(MediaType.AUDIO) ||
            (media.type.equals(MediaType.IMAGE) && media.length != null && !media.length.equals("0"))) {
            //val file = File(requireContext().filesDir, media.name)
            val destinationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DsNetMedia")

            val file = File(destinationDir, media.name)
            if(file.exists()) {
                mediaItem = Utilities.getEmptyImageMediaItem(
                    media.length,
                    requireContext(),
                    R.drawable.black,
                    media.type
                )
            }
        }else {
            var mediaId = MediaType.HTML
            if(media.type.equals(MediaType.WIDGET) ) {
                val widgetType = media.widget.widgetType
                if (widgetType.equals(WidgetType.YOUTUBE)) {
                    mediaId = MediaType.WIDGET_YOUTUBE
                } else {
                    mediaId = MediaType.WIDGET_HTML
                }
            }
            mediaItem = Utilities.getEmptyImageMediaItem(
                media.length,
                requireContext(),
                R.drawable.black,
                mediaId
            )
        }
        return mediaItem
    }

    private fun playMedia() {
        player?.apply {
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                @OptIn(UnstableApi::class)
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> loadNextPlaylistIfNeeded()
                        Player.STATE_READY -> {
                            val nextShowMediaIndex = player!!.currentMediaItemIndex
                            Log.d(TAG, "State-StateReady"+ nextShowMediaIndex +"-"+nextShowMediaIndex )
                            if (nextShowMediaIndex == 0){
                                handleTransition(nextShowMediaIndex, player!!.getMediaItemAt(nextShowMediaIndex))
                            }
                        }
                    }
                } override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int) {

                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val nextShowMediaIndex = player!!.currentMediaItemIndex
                    Log.d(TAG,"State-OnTransition-"+nextShowMediaIndex.toString())
                    handleTransition(nextShowMediaIndex,mediaItem)
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.d(TAG, "onPlayerError-"+error.message!!+error.errorCode)
                    releaseResources()
                    if(player!=null && player!!.repeatMode!=null && player!!.repeatMode==Player.REPEAT_MODE_ALL){
                        endPlaylist()
                    }
                }
            })
            play()
        }
    }
    @OptIn(UnstableApi::class)
    private fun handleTransition(nextMediaItemIndex:Int, nextMediaItem: MediaItem?) {
        //WriteText.writeToTextFile("media_transition.txt",requireContext())
        if (nextMediaItemIndex >= 0 && nextMediaItemIndex < player!!.mediaItemCount && nextMediaItem!=null) {
            addProofOfPlay(nextMediaItemIndex)
            removeYouTubeFragment()
            removeFragmentsByTagsFullScreen()
            var media =  mRunningMediaList.get(player!!.currentMediaItemIndex)
            if(media.transition!=null && media.transition!=0L || media.transition!=-1L) {
                setupTransitionAnimationIfNeeded(media)
            }
            if(media.aspectRatio){
                    playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }else{
                    playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            if(media.fullScreen && !isChannalFullScreen){

            }
            val mediaId = nextMediaItem!!.mediaId
            if(mediaId.equals(MediaType.IMAGE)){
                if(media.fullScreen && !isChannalFullScreen ){
                    createFullScreenFragment("image_full" , media)
                }else {
                    //removeFragmentsByTagsFullScreen( )
                    supportLayout.visibility = View.GONE
                }
            } else if(mediaId.equals(MediaType.AUDIO) || nextMediaItem!!.mediaId.equals(MediaType.VIDEO)){
                if(media.fullScreen && nextMediaItem!!.mediaId.equals(MediaType.VIDEO) && !isChannalFullScreen){
                    createFullScreenFragment("video_full" , media)
                }else{
                    //removeFragmentsByTagsFullScreen( )
                    supportLayout.visibility = View.GONE
                }
                setTheAllowedDuration(nextMediaItemIndex)
            }
            else if (mediaId.equals(MediaType.HTML) || mediaId.equals(MediaType.WIDGET_HTML)) {
                loadOverlayFragment("web_channal_content", mediaId, media, media.fullScreen)
            }else if(mediaId.equals(MediaType.WIDGET_YOUTUBE)){
                loadOverlayFragment("widget_youtube", mediaId, media, media.fullScreen)
            }

        }
    }

    private fun addProofOfPlay(nextMediaItemIndex: Int) {
        if(nextMediaItemIndex==0){
            if(mediaShowTime.isEmpty() && mRunningMediaList.get(0).reportPlayback){
                mediaShowTime=Utilities.getCurrentTimeForServer()
                return
            }else{
                val mediaJustCompleted = mRunningMediaList.get(mRunningMediaList.size-1)
                val mediaZeroth = mRunningMediaList.get(0)
                writeProofOfPlayIfAllowed(mediaJustCompleted)
                if(mediaZeroth.reportPlayback){
                    mediaShowTime=Utilities.getCurrentTimeForServer()
                }
            }
        }else{
            val mediaJustCompleted = mRunningMediaList.get(nextMediaItemIndex-1)
            val mediaNext = mRunningMediaList.get(nextMediaItemIndex)
            writeProofOfPlayIfAllowed(mediaJustCompleted)
            if(mediaNext.reportPlayback){
                mediaShowTime=Utilities.getCurrentTimeForServer()
            }
        }
    }

    private fun writeProofOfPlayIfAllowed(mediaJustCompleted:Media) {
        if(mediaJustCompleted.reportPlayback && PlayerManager.getPlayer()!=null && PlayerManager.getPlayer()!!.playerID!=null) {
            WriteText.writeProofOfPlay(
                requireContext(),
                mRunningPlaylist.playlistID, mediaJustCompleted.mediaID, mediaShowTime, Utilities.getCurrentTimeForServer(),
                LogType.PROOF_OF_PLAY.value,
                playerId = PlayerManager.getPlayer()!!.playerID)
        }
    }

    private fun setupTransitionAnimationIfNeeded(media: Media) {
        animationHandler = Handler(Looper.getMainLooper())
        animationRunnable = Runnable { // Your code to be executed after 10 seconds
            if(media.transition==1L) {
                fadeView.visibility = View.VISIBLE
                fadeIn(fadeView)
            }else if(media.transition==2L){
                animatePlayer()
            }
            Log.d(TAG, "starting animation")
        }
        animationHandler!!.postDelayed(animationRunnable!!, (player!!.duration-1000));
    }

    private fun removeYouTubeFragment() {
        val fragment = childFragmentManager.findFragmentByTag("widget_youtube")
        val transaction = childFragmentManager.beginTransaction()
        if (fragment != null) {
            transaction.remove(fragment)
            transaction.commitAllowingStateLoss()
        }
    }

    fun removeFragmentsByTagsFullScreen() {
        val tags = listOf("image_full","video_full","web_channal_content_full","widget_youtube_full")
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // Iterate through the provided tags
        for (tag in tags) {
            val fragment = fragmentManager.findFragmentByTag(tag)
            if (fragment != null) {
                if(fragment!!.tag.equals("web_channal_content_full")){
                    transaction.hide(fragment)
                }else {
                    transaction.remove(fragment)
                }
            }
        }

        // Commit the transaction to remove the fragments
        transaction.commitAllowingStateLoss()

        // Clear the back stack
        if (!fragmentManager.isStateSaved) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setTheAllowedDuration(nextMediaItemIndex: Int) {
        val allowedPlayDuaration = ((mRunningMediaList.get(nextMediaItemIndex).length).toLong()*1000L)
        val actualVideoDuration = player!!.duration
        if(allowedPlayDuaration>0 && actualVideoDuration> allowedPlayDuaration){
            val durationDifference = actualVideoDuration - allowedPlayDuaration
            if (durationDifference > 1500) {
                playlistItemDurationHandler.postDelayed({
                    if (player != null) {
                        if (player!!.hasNext()) {
                            player!!.seekToNext()
                        } else {
                            loadNextPlaylistIfNeeded()
                        }
                    }
                }, allowedPlayDuaration)
            }
        }else{

        }
    }

    private fun createFullScreenFragment(tag: String, media: Media) {
        var fragment: Fragment?=null

        if(tag.equals("image_full")){
             fragment = ImageFragment().apply {
                 arguments = Bundle().apply {
                     putString("url", media.url)
                     putString("fileName", media.name)
                     putString("interaction", interaction)
                     putBoolean("aspectRatio", media.aspectRatio)
//                     putString("mediaId", media.mediaID)
//                     putString("playlistId",mRunningPlaylist.playlistID )
//                     putBoolean("reportPlayback", media.reportPlayback)
                 }
             }
        }else {
            fragment = VideoFragment().apply {
                arguments = Bundle().apply {
                    putString("video_url", media.url)
                    putString("file_name", media.name)
                    putString("interaction", interaction)
                    putBoolean("aspectRatio", media.aspectRatio)
                    putBoolean("repeat", false)

                }
            }
        }
        addFullScreenFragment(fragment, tag)
    }

    private fun addFullScreenFragment(fragment: Fragment?, tag:String) {
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        if (mainContainer != null && fragment!=null) {
            transaction.replace(mainContainer.id, fragment!!, tag)
            transaction.commitAllowingStateLoss()
        }
    }

    private fun loadOverlayFragment(tag:String, mediaId:String, media: Media, isFullScreen:Boolean) {
        var fragmentManager: FragmentManager = childFragmentManager
        var modifiedTag = tag
        if(isFullScreen && !isChannalFullScreen){
            modifiedTag = tag+"_full"
            fragmentManager = requireActivity().supportFragmentManager
        }else{
            removeFragmentsByTagsFullScreen()
        }


        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        var fragment: Fragment?=null
        if(mediaId.equals(MediaType.HTML) || mediaId.equals(MediaType.WIDGET_HTML)) {
            var url = media.url
            var cacheAble = media.allowCache
            if(mediaId.equals(MediaType.WIDGET_HTML)) {
                if(media.widget.widgetURL!=null) {
                    url = media.widget.widgetURL!!
                }else{
                    return
                }
            }
            val existingFragment = fragmentManager.findFragmentByTag(modifiedTag)
            if(existingFragment!=null){
                (existingFragment as? WebFragment)?.updateContent(url)
                transaction.show(existingFragment)
                transaction.commitAllowingStateLoss()
                supportLayout.visibility = View.VISIBLE
                return
            }else {
                fragment = WebFragment().apply {
                    arguments = Bundle().apply {
                        putInt("playlistIndex", playlistIndex)
                        putInt("webType", WebType.WEB_URL.value)
                        putString("url", url)
                        putString("interaction", interaction)
                        putBoolean("isTransparent", false)
                        putBoolean("cacheable", cacheAble)
                        putBoolean("loadNow", true)
//                        putString("mediaID", media.mediaID)
//                        putString("playlistId", mRunningPlaylist.playlistID)
//                        putBoolean("reportPlayback", media.reportPlayback ?: false)
                    }
                }
            }
        }else if(mediaId.equals(MediaType.WIDGET_YOUTUBE)){
            if(media.widget!=null && media.widget.widgetParam!=null &&  media.widget.widgetParam!!.size>0) {
                val videoIdItem = media.widget?.widgetParam?.find { it.paramName == "videoid" }
                val videoCcItem = media.widget?.widgetParam?.find { it.paramName == "videocc" }

                fragment = YoutubeFragment().apply {
                    arguments = Bundle().apply {
                        if (videoIdItem != null) {
                            putString("youtube_video_id", videoIdItem.value ?:"")
                        }
                        if (videoCcItem != null) {
                            putString("youtube_can_cc",  if (videoCcItem.value.toBoolean()) "1" else "0")
                        }
                            putString("interaction", interaction)
                            putBoolean("playNow",true)
//                            putString("mediaID", media.mediaID)
//                            putString("playlistId", mRunningPlaylist.playlistID)
//                            putBoolean("reportPlayback", media.reportPlayback ?: false)
                        }
                    }
            }
        }
            if (supportLayout != null && fragment != null && !isFullScreen) {
                transaction.replace(supportLayout.id, fragment!!, modifiedTag)
                supportLayout.visibility = View.VISIBLE
            }else if(mainContainer != null && fragment != null && (isFullScreen && !isChannalFullScreen)){
                transaction.replace(mainContainer.id, fragment!!, modifiedTag)
                supportLayout.visibility = View.GONE
            }
        transaction.commitAllowingStateLoss()

    }

    private fun loadNextPlaylistIfNeeded() {
        if(isMediaListAggrigated){
            restartWithAggrigation()
        }else {
            playlistItemDurationHandler.removeCallbacksAndMessages(null)
            endPlaylist()
        }
    }

    private fun restartWithAggrigation() {
        if(mPlayer!=null && mPlayer.playlist!=null) {
            mRunningMediaList = PlayerManager.aggregateMediaFromPlaylists(mPlayer.playlist)!!
            mRunningMediaList.forEach { media ->
                Log.d(TAG, "Playlist ended Media ID: ${media.mediaID}")
            }

            player!!.clearMediaItems()
            player!!.stop()
            player!!.addMediaItems(makeMediaItemsList())
            player!!.prepare()
            player!!.play()
        }
    }

    private fun endPlaylist() {
        if (activity is MainActivity && (zone == null || zone!!.zoneType == ZoneType.ZONE_SCHEDULE)) {
            releaseResources()
            val mainActivity: MainActivity? = activity as MainActivity?
            // Check if the activity is finishing or destroyed
            if (!mainActivity!!.isFinishing && !mainActivity.isDestroyed) {
                mainActivity.playlistEnded()
            }

        }
    }

    override fun onDestroy() {
        releaseResources()
        resumeRunnable?.let { handler?.removeCallbacks(it) }
        handler = null
        resumeRunnable = null
        super.onDestroy()
        Log.d(TAG, "onDestroy-playlist-"+playlistIndex)
    }

    private fun releaseResources() {
        if(player!=null) {
            player!!.clearVideoSurface()
            player!!.stop()
            player?.release()
            player?.clearMediaItems()
            player = null
            playerView = null
        }
    }


    @OptIn(UnstableApi::class)
    private fun getMediaItem(media: Media): MediaItem {
        var mediaItem: MediaItem? = null

        if (media.type.equals(MediaType.VIDEO) ||
            media.type.equals(MediaType.AUDIO) ||
            (media.type.equals(MediaType.IMAGE) && media.length != null && !media.length.equals("0"))) {
            //val file = File(requireContext().filesDir, media.name)
            val destinationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DsNetMedia")
            val file = File(destinationDir, media.name)
            if (file.exists() && (media.type.equals(MediaType.IMAGE) && media.length != null && !media.length.equals("0"))) {
                mediaItem =
                    MediaItem.Builder().setUri(file.absolutePath).setMediaId(media.type)
                        .setImageDurationMs((media.length).toLong() * 1000).build()
            } else if (file.exists() && (media.type.equals(MediaType.VIDEO) || media.type.equals(MediaType.AUDIO))) {
                mediaItem =
                    MediaItem.Builder().setUri(file.absolutePath).setMediaId(media.type)
                        .build()
            } else {
                mediaItem = Utilities.getEmptyImageMediaItem(
                    media.length,
                    requireContext(),
                    R.drawable.black,
                    MediaType.IMAGE
                )
            }
            return mediaItem!!
        } else if (media.type.equals(MediaType.WIDGET) && media.length != null && !media.length.equals("0")) {
            val widgetType = media.widget.widgetType
            var mediaId = ""
            if (widgetType.equals(WidgetType.YOUTUBE)) {
                mediaId = MediaType.WIDGET_YOUTUBE
            } else {
                mediaId = MediaType.WIDGET_HTML
            }
            mediaItem = Utilities.getEmptyImageMediaItem(
                media.length,
                requireContext(),
                R.drawable.black,
                mediaId
            )
            return mediaItem!!
        } else if (media.type.equals(MediaType.HTML) && media.length != null && !media.length.equals("0")) {
            mediaItem = Utilities.getEmptyImageMediaItem(
                media.length,
                requireContext(),
                R.drawable.black,
                media.type
            )
        }
        return mediaItem!!
    }


    private fun captureFrame() {
        imageReader?.let { reader ->
            val image: Image? = reader.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                saveBitmap(bitmap)
                image.close()
            } else {
                Toast.makeText(requireContext(), "No image available to capture.", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(requireContext(), "ImageReader not initialized.", Toast.LENGTH_SHORT).show()
    }

    private fun imageToBitmap(image: Image): Bitmap {
        // Implement your conversion from Image to Bitmap here
        // This method will depend on the specific format of the image
        // Placeholder for the actual conversion logic
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Replace with actual conversion
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val out: FileOutputStream
        try {
            val directory = File(Environment.getExternalStorageDirectory(), "Screenshots")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "screenshot.png")
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            Toast.makeText(requireContext(), "Screenshot saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save screenshot", Toast.LENGTH_SHORT).show()
        }
    }





}