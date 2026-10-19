package com.digitalsignage.dsnetcoreplayer.fragment

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.digitalsignage.dsnetcoreplayer.R
import com.digitalsignage.dsnetcoreplayer.constants.LogType
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.listeners.CustomGestureListener
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import java.io.File

class VideoFragment : Fragment() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var videoUrl: String
    private lateinit var fileName: String
    private lateinit var interaction:String
    private var aspectRatio:Boolean=false
    private var mediaId:String =""
    private var playlistId:String=""
    private var reportPlayBack:Boolean=false
    private var videoShowTime=""
    private var isProofOfPlaySaved = false
    private var playNow:Boolean = true
    private var repeatVideos:Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoUrl = it.getString(ARG_VIDEO_URL)!!
            fileName = it.getString(ARG_FILE_NAME)!!
            interaction= it.getString(ARG_INTERACTION)!!
            aspectRatio= it.getBoolean(ARG_ASPECT_RATIO)
            mediaId = it.getString(MEDIA_ID) ?: ""
            playlistId = it.getString(PLAYLIST_ID) ?: ""
            reportPlayBack = it.getBoolean(REPORT_PLAYBACK)?:false
            playNow = it.getBoolean(PLAY_NOW)?:true
            repeatVideos = it.getBoolean(REPEAT)?:true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playerView = view.findViewById(R.id.player_view)
        if(aspectRatio){
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }else{
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
        setupPlayer()
        setupInteraction()
    }
    private fun setupInteraction() {
        if(interaction!=null && !interaction.isEmpty()){
            val interactionData = interaction.split(",")
            val gestureDetector = GestureDetector(requireContext(), CustomGestureListener(interactionData))
            playerView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
    }
    private fun setupPlayer() {
        player = ExoPlayer.Builder(requireContext()).build()
        playerView.player = player
        setVideoPath()
        if(repeatVideos) {
            player?.repeatMode = Player.REPEAT_MODE_ONE
        }
        player?.playWhenReady = true
        if(!playNow){
           player!!.pause()
        }
    }

    private fun setVideoPath() {
        player!!.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player Error:Video-fragment ${error.message}")
                // Handle the error here (e.g., show a message to the user)
            }
        })
        val destinationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DsNetMedia")
        val file = File(destinationDir, fileName)

        if (file.exists()) {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            player?.setMediaItem(mediaItem)
            player?.prepare()
            Log.d(TAG, "loaded-file")
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        Log.d(TAG, "onDestroy")
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
    override fun onResume() {
        super.onResume()
        if(reportPlayBack) {
            videoShowTime = Utilities.getCurrentTimeForServer()
        }
    }
    override fun onPause() {
        super.onPause()
        reportProofOfPlayIfAllowed()
        Log.d(TAG, "POP Saved 1")
        isProofOfPlaySaved=true
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "POP Saved 2")
        reportProofOfPlayIfAllowed()
        isProofOfPlaySaved=true

    }
    private fun reportProofOfPlayIfAllowed() {
        if(reportPlayBack && !isProofOfPlaySaved) {
            WriteText.writeProofOfPlay(
                context= requireContext(),
                playlistId = playlistId,
                contentId = mediaId,
                startTime = videoShowTime,
                endTime = Utilities.getCurrentDateTime(),
                logType = LogType.PROOF_OF_PLAY.value,
                playerId = PlayerManager.getPlayer()!!.playerID
            )
        }
    }
    companion object {
        private const val ARG_VIDEO_URL = "video_url"
        private const val ARG_FILE_NAME = "file_name"
        private const val TAG = "VideoFragmentNew"
        private const val ARG_INTERACTION = "interaction"
        private const val ARG_ASPECT_RATIO = "aspectRatio"
        private const val MEDIA_ID = "mediaID"
        private const val PLAYLIST_ID = "playlistId"
        private const val REPORT_PLAYBACK = "reportPlayback"
        private const val PLAY_NOW = "playNow"
        private const val REPEAT = "repeat"

    }
    fun playVideo(){
        player!!.seekTo(0)
        player!!.play()
    }

}
