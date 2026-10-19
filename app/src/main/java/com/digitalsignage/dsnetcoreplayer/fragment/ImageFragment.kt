package com.digitalsignage.dsnetcoreplayer.fragment



import WriteText
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils.replace
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.digitalsignage.dsnetcoreplayer.R
import java.io.File
import com.bumptech.glide.request.target.Target
import com.digitalsignage.dsnetcoreplayer.constants.LogType
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerManager
import com.digitalsignage.dsnetcoreplayer.listeners.CustomGestureListener
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import java.io.InputStream
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.LazyHeaders
import com.digitalsignage.dsnetcoreplayer.utils.RootUtil
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ImageFragment : Fragment() {
    private val TAG = "ImageFragment"
    private var imageShowTime=""
    private var isProofOfPlaySaved = false
    companion object {
        private const val ARG_URL = "url"
        private const val ARG_FILENAME = "fileName"
        private const val ARG_INTERACTION = "interaction"
        private const val ARG_ASPECT_RATIO = "aspectRatio"
        private const val MEDIA_ID = "mediaID"
        private const val PLAYLIST_ID = "playlistId"
        private const val REPORT_PLAYBACK = "reportPlayback"
    }
    private lateinit var url: String
    private lateinit var fileName:String
    private lateinit var imageView:ImageView
    private lateinit var file:File
    private lateinit var progressBar: ProgressBar
    private  var interaction:String?=null
    private var aspectRatio:Boolean=false
    private var mediaId:String =""
    private var playlistId:String=""
    private var reportPlayBack:Boolean=false
    private lateinit var testBtn:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(ARG_URL)!!
            fileName = it.getString(ARG_FILENAME)!!
            interaction= it.getString(ARG_INTERACTION)
            aspectRatio= it.getBoolean(ARG_ASPECT_RATIO)
            mediaId = it.getString(MEDIA_ID) ?: ""
            playlistId = it.getString(PLAYLIST_ID) ?: ""
            reportPlayBack = it.getBoolean(REPORT_PLAYBACK)?:false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_image, container, false)

        imageView = view.findViewById<ImageView>(R.id.imageView)
        progressBar = view.findViewById(R.id.progressBar)
//        testBtn = view.findViewById(R.id.testBtn)
//        testBtn.setOnClickListener {
//            EventBus.getDefault().post(ScreenshotEvent())
//        }
        val destinationDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DsNetMedia")

        file = File(destinationDir, fileName)
        //file = File(requireContext().filesDir, fileName)
        return view
    }

    private fun reboot() {
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

    private fun setupInteraction() {
        if(interaction!=null && !interaction!!.isEmpty()){
            val interactionData = interaction!!.split(",")
            val gestureDetector = GestureDetector(requireContext(), CustomGestureListener(interactionData))
            imageView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
    }

    private fun loadImage() {
        if(file.name.equals("expired.png")){
            Glide.with(requireContext())
                .load(R.drawable.expired)
                .into(imageView)
        }
        else if(file.exists()){
            Glide.with(requireContext())
                .load(file)
                .into(imageView)
        }else {
            progressBar.visibility = View.VISIBLE
            Glide.with(this)
                .load(url)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide ProgressBar on load failure
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hide ProgressBar on load success
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageView)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInteraction()
        if(aspectRatio) {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }else{
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        }
        loadImage()
    }

    override fun onResume() {
        super.onResume()
        if(reportPlayBack) {
            isProofOfPlaySaved=false
            imageShowTime = Utilities.getCurrentTimeForServer()
        }
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
        if(reportPlayBack && !isProofOfPlaySaved && PlayerManager.getPlayer()!=null && PlayerManager.getPlayer()!!.playerID!=null) {
            WriteText.writeProofOfPlay(
                context= requireContext(),
                playlistId = playlistId,
                contentId = mediaId,
                startTime = imageShowTime,
                endTime = Utilities.getCurrentDateTime(),
                logType = LogType.PROOF_OF_PLAY.value,
                playerId = PlayerManager.getPlayer()!!.playerID
            )
        }
    }
    fun loadImageWithBypassedSSL(context: Context, imageUrl: String): RequestBuilder<Drawable> {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

        // Create a Glide-compatible URL with custom headers
        val glideUrl = GlideUrl(
            imageUrl,
            LazyHeaders.Builder()
                .addHeader("Connection", "close")
                .build()
        )

        // Load the image with Glide
        return Glide.with(context)
            .load(glideUrl)
    }
    override fun onDestroy() {
        super.onDestroy()
    }
}
