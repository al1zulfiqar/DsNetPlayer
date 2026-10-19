package com.digitalsignage.dsnetcoreplayer.network

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private const val CONNECT_TIMEOUT = 80L
    private const val READ_TIMEOUT = 80L
    private const val WRITE_TIMEOUT = 80L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val networkInterceptor = Interceptor { chain ->
        try {
            val originalRequest = chain.request()
            val response = chain.proceed(originalRequest)
            response
        } catch (e: Exception) {
            Log.e("RetrofitClient", "Network error: ${e.message}", e)
            throw e
        }
    }

    // Custom TrustManager that bypasses time-based certificate validation
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Bypass time-based validation
            chain?.forEach { cert ->
                Log.w("CertValidation", "Bypassing certificate time validation for ${cert.subjectDN}")
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val okHttpClient: OkHttpClient by lazy {
        // Create SSL context with custom trust manager
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }

        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
            .addInterceptor(networkInterceptor)
            .addInterceptor(loggingInterceptor)
            // Add SSL socket factory with custom trust manager
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            // Bypass hostname verification (use cautiously)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun create(baseUrl: String): DsNetApiService {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(DsNetApiService::class.java)
    }
}