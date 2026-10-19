package com.digitalsignage.dsnetcoreplayer.fragment

import WriteText
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.digitalsignage.dsnetcoreplayer.R
import com.digitalsignage.dsnetcoreplayer.constants.LogType
import com.digitalsignage.dsnetcoreplayer.constants.WebType
import com.digitalsignage.dsnetcoreplayer.data.WebInteraction
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.listeners.CustomGestureListener
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import org.greenrobot.eventbus.EventBus


class WebFragment : Fragment() {
    private var alredyShown: Boolean = false
    private val handler = Handler()
    private var refreshRunnable: Runnable? = null

    private val TAG = "WebFragment"
    private var pageShowTime=""
    private var isProofOfPlaySaved = false
    private var playlistIndex: Int = 0
    private var webType:Int=0
    private lateinit var mWebView:WebView
    private val ARG_PLAYLIST_INDEX = "playlistIndex"
    private val ARG_URL = "url"
    private val ARG_WEB_TYPE = "webType"
    private val ARG_INTERACTION = "interaction"
    private val ARG_IS_TRANSPARENT = "isTransparent"
    private val ARG_CACHEABLE = "cacheable"
    private val MEDIA_ID = "mediaID"
    private val PLAYLIST_ID = "playlistId"
    private val REPORT_PLAYBACK = "reportPlayback"
    private var LOAD_NOW = "loadNow"
    private var interaction:String?=null
    private lateinit var url:String
    private var isTransparent:Boolean = false
    private var isCacheable:Boolean=false
    private var mediaId:String =""
    private var playlistId:String=""
    private var reportPlayBack:Boolean=false
    private var loadNow:Boolean=true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistIndex = it.getInt(ARG_PLAYLIST_INDEX)!!
            webType = it.getInt(ARG_WEB_TYPE)
            url= it.getString (ARG_URL)!!
            interaction= it.getString(ARG_INTERACTION)
            isTransparent= it.getBoolean(ARG_IS_TRANSPARENT)
            isCacheable= it.getBoolean(ARG_CACHEABLE)
            mediaId = it.getString(MEDIA_ID) ?: ""
            playlistId = it.getString(PLAYLIST_ID) ?: ""
            reportPlayBack = it.getBoolean(REPORT_PLAYBACK)?:false
            loadNow = it.getBoolean(LOAD_NOW)?:true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_web, container, false)
        mWebView = view.findViewById<WebView>(R.id.webView)



        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //startTransitionTimer()
        setupWebview()
        startRefreshing()
      setupInteraction()

    }
    private fun setupInteraction() {
        if(interaction!=null && !interaction!!.isEmpty()){
            val interactionData = interaction!!.split(",")
            val gestureDetector = GestureDetector(requireContext(), CustomGestureListener(interactionData))
            mWebView.setOnTouchListener { _, event ->
                EventBus.getDefault().post(WebInteraction())
                val handled = gestureDetector.onTouchEvent(event)
                handled

            }
        }else{
            mWebView.setOnTouchListener { _, event ->
                EventBus.getDefault().post(WebInteraction())
                false
            }

        }
    }
    private fun setupWebview() {
        setTeansparancyAndTextZoom()
        mWebView.setInitialScale(100)
        val webSettings = mWebView.settings
        webSettings.mediaPlaybackRequiresUserGesture = false
        mWebView.webChromeClient = WebChromeClient()
        webSettings.javaScriptEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.domStorageEnabled = true
        if(isCacheable){
            webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        mWebView.isVerticalScrollBarEnabled = false
        mWebView.isHorizontalScrollBarEnabled = false
        loadWebPage()
    }

    private fun setTeansparancyAndTextZoom() {
        if(isTransparent){
            mWebView.setBackgroundColor(Color.TRANSPARENT)
            if (webType == WebType.HTML.value) {
                val mPlayer = PlayerManager.getPlayer()
                mWebView.settings.textZoom = (mPlayer?.designerTextAlignment ?: 75L).toInt()
            }
        }
    }

    private fun loadWebPage() {
        if (webType == WebType.HTML.value) {
            val base64version = Base64.encodeToString(url.toByteArray(), Base64.DEFAULT)
            mWebView.loadData(base64version, "text/html; charset=UTF-8", "base64")

        } else {
            mWebView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    view.setInitialScale(1)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                }
            }
            if(webType.equals(WebType.WEB_URL)) {
                if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                    url = "http://$url"
                }
            }else if (webType.equals(WebType.WEB_PAGE)){
                url  = "file:///"+url
            }
            if(loadNow){
                mWebView.loadUrl(url)
            }


        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRefreshing()
        if(mWebView!=null) {
            mWebView.clearCache(true)
        }
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
    private fun startRefreshing() {
        if(refreshRunnable!=null){
            stopRefreshing()
        }
        refreshRunnable = Runnable {
            mWebView.clearCache(true)
            mWebView.reload()
            handler!!.postDelayed(refreshRunnable!!, 3600000) // 60 minutes in milliseconds
        }
        handler!!.post(refreshRunnable!!)
    }
    private fun stopRefreshing() {
        handler!!.removeCallbacks(refreshRunnable!!)

    }

    fun updateContent(url: String) {
        if(mWebView!=null && !this.url.equals(url)){
            mWebView.loadUrl(url)
        }
    }
    fun loadNow() {
        if(!isCacheable){
            mWebView.loadUrl(url)
        } else if(!alredyShown) {
            alredyShown=true
            mWebView.loadUrl(url)
        }
    }


}
