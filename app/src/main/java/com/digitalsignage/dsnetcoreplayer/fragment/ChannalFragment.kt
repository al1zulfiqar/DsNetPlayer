package com.digitalsignage.dsnetcoreplayer.fragment

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.media3.common.util.UnstableApi
import com.digitalsignage.dsnetcoreplayer.MainActivity
import com.digitalsignage.dsnetcoreplayer.R
import com.digitalsignage.dsnetcoreplayer.companion.AppRestarter
import com.digitalsignage.dsnetcoreplayer.constants.LogType
import com.digitalsignage.dsnetcoreplayer.constants.MediaType
import com.digitalsignage.dsnetcoreplayer.constants.WebType
import com.digitalsignage.dsnetcoreplayer.constants.ZoneType
import com.digitalsignage.dsnetcoreplayer.data.WebInteraction
import com.digitalsignage.dsnetcoreplayer.data.player.Media
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.data.player.Playlist
import com.digitalsignage.dsnetcoreplayer.data.player.Zone
import com.digitalsignage.dsnetcoreplayer.services.MainAppPinger
import com.digitalsignage.dsnetcoreplayer.utils.CaptureScreenshot
import com.digitalsignage.dsnetcoreplayer.utils.NetworkUtils
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class ChannalFragment:Fragment() {
    private var stopContentChannalPlaylist: Boolean=false
    private var mPlayer: Player? = null
    private lateinit var mainContainer: RelativeLayout

    private val TAG = "ChannelContent"
    private lateinit var mRunningPlaylist: Playlist
    private var totalPlaylists: Int = 0
    private var isChannalFullScreen:Boolean=false
    private lateinit var testBtn:Button
    private var mediaShowTime=""
    private var mediaTimeHandler: Handler? = null
    private var mediaResumeRunnable: Runnable? = null

    private lateinit var mRunningMediaList: List<Media>
    private var pinger: MainAppPinger? = null


    private var playlistIndex: Int = 0

    private var interaction: String? = ""

    var currentMedia: Media? =null
    var currentMediaIndex = 0
    private var nextMedia: Media? =null
    private var nextMediaIndex = 1
    object WidgetType {
        const val YOUTUBE: String = "21"
    }

    companion object {
        private const val ARG_URL = "playlistIndex"
        private const val ARG_ZONE = "zone"
        private const val ARG_INTERACTION = "interaction"
        private const val ARG_FULL_SCREEN = "full_screen"
        fun newInstance(
            playlistIndex: Int,
            zone: Zone?,
            interaction: String?,
            isFullScreen: Boolean
        ): ChannalFragment {
            val fragment = ChannalFragment()
            val args = Bundle().apply {
                putInt(ARG_URL, playlistIndex)
                putSerializable(ARG_ZONE, zone)
                putString(ARG_INTERACTION, interaction)
                putBoolean(ARG_FULL_SCREEN, isFullScreen)
            }
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistIndex = it.getInt(ChannalFragment.ARG_URL)!!
            interaction= it.getString(ChannalFragment.ARG_INTERACTION)!!
            isChannalFullScreen= it.getBoolean(ChannalFragment.ARG_FULL_SCREEN)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_channel, container, false)
        mainContainer = view.findViewById(R.id.mainContainer)
        testBtn = view.findViewById(R.id.testBtn)
        testBtn.setOnClickListener { myTestButton() }
        testBtn.visibility = View.GONE
        pinger = MainAppPinger(requireContext())
        initializeData()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!stopContentChannalPlaylist){
            checkNullPlaylist()
        }

    }

    private fun checkNullPlaylist() {
        if (mRunningPlaylist != null) {
            restartPlaylist()
        } else {
        }
    }
    private fun initializeData() {
        if(PlayerManager==null || PlayerManager.getPlayer()==null){
            changePage()
            return
        }
        mPlayer = PlayerManager.getPlayer()!!
        mRunningPlaylist = mPlayer!!.playlist[playlistIndex]
        totalPlaylists = mPlayer!!.playlist.size
        if(mRunningPlaylist.media.size==1 && Utilities.getContentChannelLength(mRunningPlaylist.media)<1){
            val pagesOfZeroLEngthMedia = mRunningPlaylist.page
            if(pagesOfZeroLEngthMedia!=null && pagesOfZeroLEngthMedia.size>0){
                setTimerForPageTimeout(pagesOfZeroLEngthMedia.get(0).collectionTimeOut)
                stopContentChannalPlaylist=true
            }
        }else {
            mRunningMediaList = PlayerManager.mergeAllMedia(mPlayer!!.playlist, playlistIndex)!!
        }
    }


    private fun setTimer(media: Media) {
        if(mediaTimeHandler!=null && mediaResumeRunnable!=null) {
            mediaTimeHandler?.removeCallbacks(mediaResumeRunnable!!)
        }
        if (mediaTimeHandler == null) {
            mediaTimeHandler = Handler(Looper.getMainLooper())
        }
        mediaResumeRunnable = Runnable {
            writeProofOfPlayIfAllowed(currentMedia!!)
            mediaShowTime = Utilities.getCurrentDateTime()
            handleTransition()
            pinger!!.sendPing()
        }
        mediaTimeHandler?.postDelayed(mediaResumeRunnable!!, (media.length.toLong())*1000)
    }
    fun setTimerForPageTimeout(time:Long){
        if(mediaTimeHandler!=null && mediaResumeRunnable!=null) {
            mediaTimeHandler?.removeCallbacks(mediaResumeRunnable!!)
        }
        if (mediaTimeHandler == null) {
            mediaTimeHandler = Handler(Looper.getMainLooper())
        }
        mediaResumeRunnable = Runnable {
            pinger!!.sendPing()
            changePage()
        }
        mediaTimeHandler?.postDelayed(mediaResumeRunnable!!, time *1000)
    }
    @OptIn(UnstableApi::class)
    private fun loadMediaPlayIfNeeded(media: Media, playNow: Boolean) {
        var repeatMode = false
        if(nextMedia==null){
            repeatMode=true
        }
        var fragment: Fragment?=null
        if(media!!.type== MediaType.IMAGE ||
            media!!.type== MediaType.VIDEO || media!!.type== MediaType.AUDIO){
            val destinationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DsNetMedia")
            val file = File(destinationDir, media.name)

            if(file.exists()) {
                if(media!!.type== MediaType.IMAGE ) {
                    fragment = ImageFragment().apply {
                        arguments = Bundle().apply {
                            putString("url", media.url)
                            putString("fileName", media.name)
                            putString("interaction", interaction)
                            putBoolean("aspectRatio", media.aspectRatio)
                        }
                    }
                }else {
                    fragment = VideoFragment().apply {
                        arguments = Bundle().apply {
                            putString("video_url", media.url)
                            putString("file_name", media.name)
                            putString("interaction", interaction)
                            putBoolean("aspectRatio", media.aspectRatio)
                            putBoolean("playNow",playNow)
                            putBoolean("repeat",repeatMode)
                        }
                    }

                }
            }
        }else if(media!!.type == MediaType.DOCX ){
            fragment = WordReaderFragment().apply {
                arguments = Bundle().apply {
                    putString("docType", media!!.type)
                    putString("docName", media.name)
                    putString("url", media.url)
                }
            }
        }else if(media!!.type == MediaType.PDF){
            fragment = PDFViewerFragment().apply {
                arguments = Bundle().apply {
                    putString("docType", media!!.type)
                    putString("docName", media.name)
                    putString("url", media.url)
                }
            }
        }
        else if(media!!.type == MediaType.HTML || (media!!.type == MediaType.WIDGET && !media!!.widget.widgetType.equals(WidgetType.YOUTUBE) )){
            var url = media.url
            var cacheAble = media.allowCache
            if(media.type.equals(MediaType.WIDGET)) {
                if(media.widget.widgetURL!=null) {
                    url = media.widget.widgetURL!!
                }else{
                    return
                }
            }
            fragment = WebFragment().apply {
                arguments = Bundle().apply {
                    putInt("playlistIndex", playlistIndex)
                    putInt("webType", WebType.WEB_URL.value)
                    putString("url", url)
                    putString("interaction", interaction)
                    putBoolean("isTransparent", false)
                    putBoolean("cacheable", cacheAble)
                    putBoolean("loadNow", playNow)
                }
            }

        }else if(media!!.type == MediaType.WIDGET && media!!.widget.widgetType.equals(WidgetType.YOUTUBE)){
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
                        putBoolean("playNow",playNow)
                    }
                }
            }
        }
        try {
            addFragment(fragment = fragment,media.mediaID, media.allowCache, media.fullScreen )
        }catch (e:Exception){
        }

    }
    private fun addFragment(fragment: Fragment?, tag: String, allowCache: Boolean, fullScreen: Boolean) {
        var fragmentManager: FragmentManager = childFragmentManager
//        if(fullScreen && !isChannalFullScreen){
        if(fullScreen){
             fragmentManager= requireActivity().supportFragmentManager
        }

        val transaction: FragmentTransaction = fragmentManager!!.beginTransaction()

        if (mainContainer != null && fragment!=null) {
//            if(fullScreen && !isChannalFullScreen) {
                if (fullScreen) {
                    if (fragment is WebFragment && allowCache) {
                        if (fragmentManager.findFragmentByTag(tag) == null) {
                            transaction.add(android.R.id.content, fragment!!, tag).hide(fragment)
                        }
                    } else {
                        transaction.add(android.R.id.content, fragment!!, tag).hide(fragment)
                    }
                    if(nextMedia==null){
                        transaction.show(fragment)
                        transaction.commit()
                    }else {
                        transaction.commitNow()
                    }
                } else {
                    if (fragment is WebFragment && allowCache) {
                        if (fragmentManager.findFragmentByTag(tag) == null) {
                            transaction.add(mainContainer.id, fragment!!, tag).hide(fragment)
                        }
                    } else {
                        transaction.add(mainContainer.id, fragment!!, tag).hide(fragment)
                    }
                    if(nextMedia==null){
                        transaction.show(fragment)
                        transaction.commit()
                    }else {
                        transaction.commitNow()
                    }
                }
//            if(fullScreen && !isChannalFullScreen && !currentMedia!!.fullScreen){
            if(fullScreen && !currentMedia!!.fullScreen ){
                fragmentManager = childFragmentManager
            }
            val currentFragment =fragmentManager.findFragmentByTag(currentMedia!!.mediaID)
            if(currentFragment!=null){
                fragmentManager.beginTransaction()
                    .show(currentFragment)
                    .commitNow()

            }
        }
    }
    private fun removeTopFragment(){
        if(isAdded) {
            var fragmentManager: FragmentManager = childFragmentManager
//        if(currentMedia!!.fullScreen && !isChannalFullScreen){
            if (currentMedia!!.fullScreen) {
                fragmentManager = requireActivity().supportFragmentManager
            }
            val topFragment = fragmentManager.findFragmentByTag(currentMedia!!.mediaID)
            Log.d(TAG, "Fragment-Removed" + currentMedia!!.mediaID)
            val transaction = fragmentManager.beginTransaction()
            if (currentMedia!!.transition != null && currentMedia!!.transition == 1L) {
                transaction.setCustomAnimations(
                    R.anim.blur_in_long,
                    R.anim.blur_out_long  // exit animation
                )
            } else if (currentMedia!!.transition != null && currentMedia!!.transition == 2L) {
                transaction.setCustomAnimations(
                    0,
                    R.anim.slide_out  // exit animation
                )
            } else {
                transaction.setCustomAnimations(
                    R.anim.blur_in,
                    R.anim.blur_out
                )
            }
            if (topFragment != null) {
                if (topFragment is WebFragment && currentMedia!!.allowCache) {
                    transaction.hide(topFragment)
                } else {
                    transaction.remove(topFragment!!)
                }
                transaction.commit()
            }
            if (nextMedia!!.type == MediaType.AUDIO || nextMedia!!.type == MediaType.VIDEO ||
                (nextMedia!!.type == MediaType.HTML || (nextMedia!!.type == MediaType.WIDGET))
            ) {
//            if(currentMedia!!.fullScreen && !isChannalFullScreen && !nextMedia!!.fullScreen){
                if (currentMedia!!.fullScreen && !nextMedia!!.fullScreen) {
                    fragmentManager = childFragmentManager
                }
                val nextFragment = fragmentManager.findFragmentByTag(nextMedia!!.mediaID)
                if (nextFragment is VideoFragment) {
                    nextFragment.playVideo()
                } else if (nextFragment is WebFragment) {
                    nextFragment.loadNow()
                } else if (nextFragment is YoutubeFragment) {
                    nextFragment.loadNow()
                }
            }
        }
    }

    private fun markMediaAsPlayed(media: Media) {
        if(media!=null && media.playlistID!=null && PlayerManager.getPlayer()!=null) {
            val markableMediaPlaylist =
                PlayerManager.getPlayer()!!.getPlaylistById(media.playlistID)
            if (markableMediaPlaylist!=null && markableMediaPlaylist!!.maximumMedia > 0 && markableMediaPlaylist!!.maximumMedia < Utilities.filterMediaList(
                    markableMediaPlaylist.media,
                    Utilities.getCurrentDateTime()
                ).size
            ) {
                PlayerManager.updatePlaylistMedia(markableMediaPlaylist.playlistID, media!!.mediaID)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPageInteraction(event: WebInteraction) {
        setTimer(currentMedia!!)
    }

    private fun handleTransition() {
        if(nextMedia==null){
            return
        }

        val currentMediaPageID = mPlayer!!.getPlaylistById(currentMedia!!.playlistID)!!.pageID
        val nextMediaPageId = mPlayer!!.getPlaylistById(nextMedia!!.playlistID)!!.pageID
        if(mPlayer!=null  && !(currentMediaPageID==null && nextMediaPageId==null)) {
             if ((currentMediaPageID == null && nextMediaPageId != null) || (currentMediaPageID != null && nextMediaPageId == null)
                    || !currentMediaPageID.equals(nextMediaPageId)) {
                 if(currentMediaPageID!=null && nextMediaPageId==null){
                     mainContainer.visibility = View.GONE
                 }else {
                     markMediaAsPlayed(currentMedia!!)
                     changePage()
                     return
                 }

            }
        }

        if(nextMedia!!.fullScreen){
            showAddedNextMediaFullScreen()
        }
        if(!currentMedia!!.playlistID.equals(nextMedia!!.playlistID) ){
            updatePlaylistIndexOnly()
        }
        //Assiming both media will be played in all cases
        markMediaAsPlayed(currentMedia!!)
        markMediaAsPlayed(nextMedia!!)
        removeTopFragment()
        setTimer(nextMedia!!)

        currentMediaIndex=nextMediaIndex
        currentMedia = nextMedia
        nextMediaIndex++
        if(nextMediaIndex>=mRunningMediaList.size){
            mRunningMediaList = PlayerManager.mergeAllMedia(mPlayer!!.playlist,playlistIndex)!!
            nextMedia = mRunningMediaList.get(0)
            nextMediaIndex = 0
        }else {
            nextMedia = mRunningMediaList.get(nextMediaIndex)
        }
        loadMediaPlayIfNeeded(nextMedia!!, false)
        System.gc()
    }

    private fun showAddedNextMediaFullScreen() {
        if(isAdded) {
            val fmamanger = requireActivity().supportFragmentManager
            val fullNextFragment = fmamanger.findFragmentByTag(nextMedia!!.mediaID)
            if (fullNextFragment != null) {
                fmamanger.beginTransaction().show(fullNextFragment).commit()
                if (fullNextFragment is VideoFragment) {
                    fullNextFragment.playVideo()
                }
            }
        }
    }

    private fun changePage() {
        removeCallbacks()
        val mainActivity: MainActivity? = activity as MainActivity?
        // Check if the activity is finishing or destroyed
        if (mainActivity!=null && !mainActivity!!.isFinishing && !mainActivity.isDestroyed) {
            mainActivity.playlistEnded()
            PlayerManager.setChannalContentNextIndex(currentMediaIndex+1)
        }
    }
    fun updatePlaylistIndexOnly(){
        val mainActivity: MainActivity? = activity as MainActivity?
        // Check if the activity is finishing or destroyed
        if (mainActivity !=null && !mainActivity!!.isFinishing && !mainActivity.isDestroyed) {
            playlistIndex = mainActivity.updatePlaylistIndex()
        }
    }

    @OptIn(UnstableApi::class)
    fun restartPlaylist(){
        if(mRunningMediaList.size>0){
            currentMediaIndex=PlayerManager.channalContentNextIndex()
            if(currentMediaIndex>=mRunningMediaList.size){
                currentMediaIndex = 0
                PlayerManager.setChannalContentNextIndex(0)
            }
            currentMedia = mRunningMediaList.get(currentMediaIndex)

            if(mRunningMediaList.size>1 ){

                if( (currentMediaIndex+1)>=mRunningMediaList.size){
                    nextMediaIndex =0
                }else{
                    nextMediaIndex = currentMediaIndex+1
                }
                nextMedia = mRunningMediaList.get(nextMediaIndex)
            }
             if(nextMedia==null){
                loadMediaPlayIfNeeded(currentMedia!!,true)
            }else {
                CoroutineScope(Dispatchers.Main).launch {
                    if(currentMedia!=null) {
                        loadMediaPlayIfNeeded(currentMedia!!, true)
                        delay(100) // 1 second delay
                        loadMediaPlayIfNeeded(nextMedia!!, false)
                        setTimer(currentMedia!!)
                        mediaShowTime = Utilities.getCurrentDateTime()
                    }
                }
            }
        }else{
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        removeCallbacks()
    }


    override fun onDestroy() {
        super.onDestroy()
        removeCallbacks()
        mPlayer = null
        currentMedia = null
        nextMedia=null
    }
    fun removeCallbacks(){
        mediaResumeRunnable?.let { mediaTimeHandler?.removeCallbacks(it) }
        mediaTimeHandler = null
        mediaResumeRunnable = null
    }
    fun myTestButton() {

    }
}

class ScreenshotEvent {

}
