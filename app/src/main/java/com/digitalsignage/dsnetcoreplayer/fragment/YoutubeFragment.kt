package com.digitalsignage.dsnetcoreplayer.fragment

import androidx.fragment.app.Fragment
import com.digitalsignage.dsnetcoreplayer.R

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.Nullable
import com.digitalsignage.dsnetcoreplayer.constants.LogType
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.listeners.CustomGestureListener
import com.digitalsignage.dsnetcoreplayer.utils.Utilities

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class YoutubeFragment  : Fragment(){
    private val TAG = "YoutubeFragment"
    private var mYoutubeVideoId: String? = null
    private var mYoutubeHasCC: String? = null
    private lateinit var mWebView: WebView
    private var pageShowTime=""
    private var isProofOfPlaySaved = false
    private  var interaction:String?=null
    private var mediaId:String =""
    private var playlistId:String=""
    private var reportPlayBack:Boolean=false
    private var loadNow:Boolean=true
    private var LOAD_NOW = "playNow"
    companion object {
        private const val YOUTUBE_VIDEO_ID = "youtube_video_id"
        private const val YOUTUBE_CAN_CC = "youtube_can_cc"
        private  val ARG_INTERACTION = "interaction"
        private const val MEDIA_ID = "mediaID"
        private const val PLAYLIST_ID = "playlistId"
        private const val REPORT_PLAYBACK = "reportPlayback"
    }

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_youtube_new, container, false)
        mWebView = root.findViewById(R.id.youtube)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteraction()
    }
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            interaction= it.getString(ARG_INTERACTION)
            mYoutubeVideoId = it.getString(YOUTUBE_VIDEO_ID)
            mYoutubeHasCC = it.getString(YOUTUBE_CAN_CC, "0")
            mediaId = it.getString(MEDIA_ID) ?: ""
            playlistId = it.getString(PLAYLIST_ID) ?: ""
            reportPlayBack = it.getBoolean(REPORT_PLAYBACK)?:false
            loadNow = it.getBoolean(LOAD_NOW)?:true
        } ?: throw UnsupportedOperationException("${javaClass.simpleName} did not receive args in onCreate()")

        Log.i("ClickIssue**", "youtube**")
    }
    private fun setupInteraction() {
        if(interaction!=null && !interaction!!.isEmpty()){
            val interactionData = interaction!!.split(",")
            val gestureDetector = GestureDetector(requireContext(), CustomGestureListener(interactionData))
            mWebView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
    }
    override fun onResume() {
        super.onResume()

        loadContent()
    }

    private fun loadContent() {
        mWebView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            displayZoomControls = false
            builtInZoomControls = false
            mediaPlaybackRequiresUserGesture = false
        }
        if(loadNow) {
            mWebView.loadDataWithBaseURL(
                "http://www.youtube.com",
                getAssetPage(),
                "text/html",
                "utf-8",
                null
            )
        }
        }

    private fun getAssetPage(): String {
        val data = StringBuilder()
        try {
            val isStream: InputStream = requireActivity().assets.open("youtube_frame.html")
            BufferedReader(InputStreamReader(isStream)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    line?.let {
                        data.append(it.replace("[VIDEO_ID]", mYoutubeVideoId ?: "")
                            .replace("[CC_LOAD]", mYoutubeHasCC ?: ""))
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return data.toString()
    }
    override fun onPause() {
        super.onPause()
        reportProofOfPlayIfAllowed()
        isProofOfPlaySaved=true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        reportProofOfPlayIfAllowed()
        isProofOfPlaySaved=true

    }
    private fun reportProofOfPlayIfAllowed() {
        if(reportPlayBack && !isProofOfPlaySaved) {
            WriteText.writeProofOfPlay(
                context= requireContext(),
                playlistId = playlistId,
                contentId = mediaId,
                startTime = pageShowTime,
                endTime = Utilities.getCurrentDateTime(),
                logType = LogType.PROOF_OF_PLAY.value,
                playerId = PlayerManager.getPlayer()!!.playerID
            )
        }
    }
    fun loadNow() {
        mWebView.loadDataWithBaseURL(
            "http://www.youtube.com",
            getAssetPage(),
            "text/html",
            "utf-8",
            null
        )
    }
    fun updateContent(cc: String?, videoId:String?) {
        mYoutubeHasCC = cc
        mYoutubeVideoId = videoId
        loadContent()
    }
    fun getCurrentVideoId():String?{
        return mYoutubeVideoId
    }
}
