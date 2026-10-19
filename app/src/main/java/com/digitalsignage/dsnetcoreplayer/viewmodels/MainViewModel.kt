package com.digitalsignage.dsnetcoreplayer.viewmodels

import WriteText
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalsignage.dsnetcoreplayer.constants.Constants
import com.digitalsignage.dsnetcoreplayer.data.PlayerRuntimeLog
import com.digitalsignage.dsnetcoreplayer.data.player.Media
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.data.player.Playlist
import com.digitalsignage.dsnetcoreplayer.data.repositories.PlayerRepository
import com.digitalsignage.dsnetcoreplayer.data.requestdata.DeviceDetails
import com.digitalsignage.dsnetcoreplayer.network.RetrofitClient
import com.digitalsignage.dsnetcoreplayer.utils.NetworkUtils
import com.digitalsignage.dsnetcoreplayer.utils.ScreenshotRequest
import com.digitalsignage.dsnetcoreplayer.utils.Utilities

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody


class MainViewModel(private val app: Application, private val repository: PlayerRepository) : ViewModel() {
    private val TAG = "MainViewModal"
    private val _contactServerResponse = MutableLiveData<String>()
    val responseString: LiveData<String> get() = _contactServerResponse
    val latestPlayer: LiveData<Player> = repository.latestPlayer

    private val lastPlayedIndices = mutableMapOf<String, Int>()
    private val proofOfPlayJob: Job

    init {
        proofOfPlayJob = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // 5 minutes
                if(NetworkUtils.isInternetAvailable(app)) {
                    sendProofOfPlayData()
                }
            }
        }
    }
    fun uploadDeviceDetails(deviceDetails: DeviceDetails){
        val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
        if(NetworkUtils.isInternetAvailable(app)) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = apiService.postDeviceDetails(deviceDetails)
                    if (response.isSuccessful) {
                    } else {
                    }
                } catch (e: Exception) {
                }
            }
        }
    }
    fun contactServer(playerId: String, version: String) {
        val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
        if(NetworkUtils.isInternetAvailable(app)) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = apiService.contactServer(playerId, version)
                    if (response.isSuccessful) {
                        _contactServerResponse.postValue(response.body())
                    } else {
                        _contactServerResponse.postValue("Error: ${response.message()}")
                    }
                } catch (e: Exception) {
                    _contactServerResponse.postValue("Error: ${e}")

                }
            }
        }else{

        }
    }
    public fun sendProofOfPlayData() {
        viewModelScope.launch(Dispatchers.IO) {
            val proofOfPlayData = WriteText.readAndClearProofOfPlay(app)
            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                proofOfPlayData.toString()
            )

            if (proofOfPlayData.length() > 0) {
                Log.e(TAG, "Returned Proof of Play Data: ${proofOfPlayData.toString()}")
                val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
                try {
                    val response = apiService.sendProofOfPlay(requestBody)
                    handleProofOfPlayResponse(response)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending proof of play data", e)
                }
            }
        }
    }

    private fun handleProofOfPlayResponse(response: retrofit2.Response<String>) {
        if (response.isSuccessful) {
            WriteText.clearProofOfPlayFile(app)
//            EventBus.getDefault().post(ClearPrroOfPlay(true))
        } else {
            Log.e(TAG, "Failed to send proof of play data: ${response.errorBody()?.string()}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        proofOfPlayJob.cancel()
    }
    fun refreshPlaylist(playerId: String) {
        viewModelScope.launch {
            if (NetworkUtils.isInternetAvailable(app)) {
                try {
                    repository.fetchAndSavePlayer(playerId)
                } catch (e: Exception) {
                    // Handle exceptions or propagate error to the UI
                    e.printStackTrace()
                }
            }
        }
    }
    fun getPlayer(playerId:String):LiveData<Player?>?{
        return repository.getPlayer(playerId)
    }

    suspend fun getPlayerById(playerId: String): Player? {
        return withContext(Dispatchers.IO) {
            repository.getPlayerObject(playerId)
        }
    }


    fun getCurrentPlaylistIndex(playlistId: String): Int {
        return lastPlayedIndices[playlistId] ?: -1
    }



    fun deleteAllPlayers() {
        viewModelScope.launch {
            repository.deleteAllPlayers()
        }
    }

    fun postRunTimeData(playerRunTimeLogs: List<PlayerRuntimeLog>, addNewTime:Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.e(TAG, "PostingScreenRun time data")
            val apiService = RetrofitClient.create(Constants.DSNET_BASE_URL)
            try {
                if (playerRunTimeLogs.size > 0) {
                    for(item in playerRunTimeLogs){
                        Log.e(TAG, "PostingScreenRun time: "+item.startTime+" : "+item.endTime)
                    }

                    val response = apiService.submitPlayerRuntimeLogs(playerRunTimeLogs)
                    if (response.isSuccessful) {
                        Log.e(TAG, "PostingScreenRun Success")
                        //val currentDataInFile = WriteText.readFromTextFile("screen_time_"+Utilities.getAppVersion(app)+ ".txt", app)
                        //rewriteNewDataInFile(currentDataInFile)
                        WriteText.clearTextFile("screen_time_" + Utilities.getAppVersion(app) + ".txt", app)
                        if(addNewTime) {
                            val lastEndTime = playerRunTimeLogs.lastOrNull()?.endTime
                            WriteText.writeToTextFile(
                                "screen_time_" + Utilities.getAppVersion(app) + ".txt",
                                app,
                                "ScreenStartTime:",
                                lastEndTime!!,
                                false
                            )
                            WriteText.writeToTextFile(
                                "screen_time_" + Utilities.getAppVersion(app) + ".txt",
                                app,
                                "ScreenEndTime:",
                                lastEndTime!!,
                                false
                            )
                        }
                    } else{
                        Log.e(TAG, "PostingScreenRun Failed")
                            //handleUploadFailure(addNewTime, playerRunTimeLogs)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending proof of play data", e)
            }
        }
    }

    private fun handleUploadFailure(addNewTime: Boolean, playerRunTimeLogs: List<PlayerRuntimeLog>) {
        val currentDataInFile = WriteText.readFromTextFile("screen_time_"+Utilities.getAppVersion(app)+ ".txt", app)
        //WriteText.clearTextFile("screen_time_" + Utilities.getAppVersion(app) + ".txt")
        for (item in playerRunTimeLogs){

        }
        rewriteNewDataInFile(currentDataInFile)
    }

    private fun rewriteNewDataInFile(currentDataInFile: List<String>) {
        for (item in currentDataInFile){
            if(item.contains("ScreenStartTime")){

            }else{

            }
        }
    }
}



class PlayerViewModelFactory(private val application: Application,private val repository: PlayerRepository,
                             ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application,repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
