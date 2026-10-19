package com.digitalsignage.dsnetcoreplayer.services

import com.digitalsignage.dsnetcoreplayer.network.DsNetApiService
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class OfficeToPdfConverter {
    private val BASE_URL = "https://api.cloudmersive.com/"
    private val API_KEY = "90980765-5a33-4646-8115-748400d613e5"

    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val okHttpClient = OkHttpClient.Builder().apply {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        hostnameVerifier { _, _ -> true }
        connectTimeout(50, TimeUnit.SECONDS)
        readTimeout(50, TimeUnit.SECONDS)
        writeTimeout(50, TimeUnit.SECONDS)
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(DsNetApiService::class.java)

    fun convertDocxToPdf(url:String ,officeFile: File, onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        val requestFile = RequestBody.create(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document".toMediaTypeOrNull(),
            officeFile
        )
        val body = MultipartBody.Part.createFormData("inputFile", officeFile.name, requestFile)
            api.convertDocxToPdf(API_KEY, body).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        onSuccess(savePDFVersion(response, officeFile))
                    } else {
                        onError("Conversion failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onError("Network error: ${t.message}")
                }
            })
    }
}

    private fun savePDFVersion(response: Response<ResponseBody>, officeFile: File): File {
        val pdfFile = File(officeFile.parent, "${officeFile.nameWithoutExtension}.pdf")
        // Ensure the response body is not null
        val responseBody = response.body() ?: throw IllegalStateException("Response body is null")

        pdfFile.writeBytes(responseBody.bytes())
        return pdfFile
    }
