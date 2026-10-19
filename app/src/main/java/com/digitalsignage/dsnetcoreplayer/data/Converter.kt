package com.digitalsignage.dsnetcoreplayer.data


import androidx.room.TypeConverter
import com.digitalsignage.dsnetcoreplayer.data.player.PlayerUpTime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.digitalsignage.dsnetcoreplayer.data.player.Playlist

class Converters {
    private val gson = Gson()
    @TypeConverter
    fun fromStringList(value: String?): List<String?> {
        if (value == null) {
            return emptyList()
        }
        val type = object : TypeToken<List<String?>>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun toStringList(list: List<String?>): String {
        return Gson().toJson(list)
    }
    @TypeConverter
    fun fromPlaylistList(value: String?): List<Playlist> {
        if (value == null) {
            return emptyList()
        }
        val type = object : TypeToken<List<Playlist>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toPlaylistList(list: List<Playlist>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromPlayerUptimelistList(value: String?): List<PlayerUpTime> {
        if (value == null) {
            return emptyList()
        }
        val type = object : TypeToken<List<PlayerUpTime>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun toplayerUptimeList(list: List<PlayerUpTime>): String {
        return gson.toJson(list)
    }

}
