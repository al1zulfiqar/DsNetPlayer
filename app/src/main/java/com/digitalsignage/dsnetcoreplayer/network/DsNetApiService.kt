package com.digitalsignage.dsnetcoreplayer.network

import com.digitalsignage.dsnetcoreplayer.data.PlayerRuntimeLog
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.data.requestdata.DeviceDetails
import com.digitalsignage.dsnetcoreplayer.utils.ScreenshotRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query


interface DsNetApiService {

    @GET("api/Player/ContactServer")
    suspend fun contactServer(
        @Query("id") id: String?,
        @Query("version") version: String?
    ): Response<String>

    @GET("api/Player/Playlist")
    suspend fun getPlaylist(
        @Query("id") id: String,
    ): Response<Player>

    @POST("api/Player/SaveScreenShots")
    suspend fun uploadScreenshot(@Body request: ScreenshotRequest?): Response<String>

    @POST("api/Player/ProofOfPlay")
    suspend fun sendProofOfPlay(@Body jsonBody: RequestBody): Response<String>

    @POST("api/player/SaveNetworkHealth") // Replace with your actual endpoint
    suspend fun postDeviceDetails(@Body deviceDetails: DeviceDetails): Response<SaveNetworkHealthResponse>

    @POST("api/Player/PlayerRuntimeLogs")
    suspend fun submitPlayerRuntimeLogs(@Body logs: List<PlayerRuntimeLog>): Response<String>

    @Multipart
    @POST("convert/docx/to/pdf")
    fun convertDocxToPdf(
        @Header("Apikey") apiKey: String,
        @Part inputFile: MultipartBody.Part
    ): Call<ResponseBody>
    @Multipart
    @POST("convert/xlsx/to/pdf")
    fun convertExcelToPdf(
        @Header("Apikey") apiKey: String,
        @Part inputFile: MultipartBody.Part
    ): Call<ResponseBody>
}

