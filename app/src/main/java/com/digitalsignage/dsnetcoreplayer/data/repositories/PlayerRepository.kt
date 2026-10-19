package com.digitalsignage.dsnetcoreplayer.data.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import com.digitalsignage.dsnetcoreplayer.data.PlayerUpdatedEvent
import com.digitalsignage.dsnetcoreplayer.data.dao.PlayerDao
import com.digitalsignage.dsnetcoreplayer.data.player.Player
import com.digitalsignage.dsnetcoreplayer.network.DsNetApiService
import org.greenrobot.eventbus.EventBus
import retrofit2.HttpException

class PlayerRepository(private val playerDao: PlayerDao, private val apiService: DsNetApiService) {
    val latestPlayer: LiveData<Player> = playerDao.getLatestPlayer()
    suspend fun fetchAndSavePlayer(playerId: String) {

        try {
            val response = apiService.getPlaylist(playerId)
            if (response.isSuccessful) {
                response.body()?.let { player ->

                    insertPlayer(player)
                    EventBus.getDefault().post(PlayerUpdatedEvent(player))
                }
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            // Handle exceptions
            e.printStackTrace()
        }
    }
    private suspend fun insertPlayer(player: Player) {
        playerDao.insert(player)
    }
    fun getPlayer(playerId: String): LiveData<Player?>? {
        return playerDao.getPlayer(playerId)
    }
     fun getPlayerObject(id: String): Player? {
        return playerDao.getPlayerObject(id)
    }
    suspend fun deleteAllPlayers() {
        playerDao.deleteAll()
    }
}
