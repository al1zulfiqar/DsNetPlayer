package com.digitalsignage.dsnetcoreplayer.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.digitalsignage.dsnetcoreplayer.data.player.Player

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: Player)

    @Query("SELECT * FROM player WHERE playerID = :id")
    fun getPlayer(id: String): LiveData<Player?>?

    @Query("SELECT * FROM player WHERE playerID = :id")
    fun getPlayerObject(id: String): Player?

    @Query("DELETE FROM player")
    suspend fun deleteAll()

    @Query("SELECT * FROM player ORDER BY playerID DESC LIMIT 1")
    fun getLatestPlayer(): LiveData<Player>
}