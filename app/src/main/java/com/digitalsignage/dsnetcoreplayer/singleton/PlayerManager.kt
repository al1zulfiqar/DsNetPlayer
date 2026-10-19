package com.digitalsignage.dsnetcoreplayer.data.player

import android.annotation.SuppressLint
import com.digitalsignage.dsnetcoreplayer.data.PlayerRuntimeLog
import com.digitalsignage.dsnetcoreplayer.utils.Utilities
import com.digitalsignage.dsnetcoreplayer.utils.Utilities.Companion.filterMediaList
import java.time.format.DateTimeFormatter

object PlayerManager {
    private val lastPlayedMediaMap = mutableMapOf<String, String>()
    private val lastPlayedIndices = mutableMapOf<String, Int>()
    private var playerInstance: Player? = null
    private val playlistStates = mutableMapOf<String, PlaylistState>()
    private var channalContentNextIndex = 0
    private data class PlaylistState(
        var currentIndex: Int = 0,
        val playedIndices: MutableSet<Int> = mutableSetOf()
    )
    fun setChannalContentNextIndex(index:Int){
        this.channalContentNextIndex = index
    }
    fun channalContentNextIndex():Int{
        return channalContentNextIndex
    }
    fun getNextMediaBatch(
        playlist: Playlist,
        currentDateTime: String,
    ): List<Media> {
        // Get or create playlist state
        val state = playlistStates.getOrPut(playlist.playlistID) { PlaylistState() }

        // Filter out expired media
        val validMedia = filterMediaList(playlist.media, currentDateTime)

        // If no valid media, return empty list
        if (validMedia.isEmpty()) {
            return emptyList()
        }

        // Calculate how many items to return
        val batchSize = minOf(playlist.maximumMedia.toInt(), validMedia.size)
        val result = mutableListOf<Media>()

        while (result.size < batchSize) {
            // Reset played indices if we've played all items
            if (state.playedIndices.size >= validMedia.size) {
                state.playedIndices.clear()
            }

            // Find next unplayed index
            var nextIndex = state.currentIndex
            while (nextIndex in state.playedIndices) {
                nextIndex = (nextIndex + 1) % validMedia.size
            }

            // Add media to result and mark as played
            result.add(validMedia[nextIndex])
            state.playedIndices.add(nextIndex)

            // Update current index for next time
            state.currentIndex = (nextIndex + 1) % validMedia.size
        }

        return if (playlist.shuffle) result.shuffled() else result
    }

    // Reset the state for a specific playlist
    fun resetPlaylistState(playlistId: String) {
        playlistStates.remove(playlistId)
    }

    // Reset all playlist states
    fun resetAllStates() {
        playlistStates.clear()
    }

    fun getPlayer(): Player? {
        return playerInstance
    }

    fun setPlayer(player: Player) {
        playerInstance = player
    }

    fun clearPlayer() {
        playerInstance = null
    }

    fun mergeAllMedia(playlists: List<Playlist>, playlistIndex:Int): List<Media> {
        val aggregatedMedia = mutableListOf<Media>()

        for ( playlist in playlists ){
            if(Utilities.canPlay(playlist.schedule, Utilities.getCurrentDateTime())) {
                if (playlist.pageID == null) {
                    playlist.media.forEach { mediaItem ->
                        mediaItem.fullScreen = true
                    }
                }

                //println("Aggregation raw: ${playlist.media.map { it.mediaID }}")
                val noExpireMedia = filterMediaList(playlist.media, Utilities.getCurrentDateTime())
                //println("Aggregation no expires: ${noExpireMedia.map { it.mediaID }}")
                if (playlist.maximumMedia > 0 && playlist.maximumMedia < noExpireMedia.size) {
                    val nextMediaItems = Utilities.getNextMediaItems(
                        noExpireMedia,
                        getCurrentMedia(playlist.playlistID), playlist.maximumMedia.toInt()
                    )
                    if (playlist.shuffle) {
                        val shuffledMediaItems = nextMediaItems.shuffled()
                        aggregatedMedia.addAll(shuffledMediaItems)
                    } else {
                        aggregatedMedia.addAll(nextMediaItems)
                    }
                } else {
                    if (playlist.shuffle) {
                        val shuffledMediaItems = noExpireMedia.shuffled()
                        aggregatedMedia.addAll(shuffledMediaItems)
                    } else {
                        aggregatedMedia.addAll(noExpireMedia)
                    }
                }
            }
        }
        //val prioritizeMedia = prioritizeSingleMediaId(aggregatedMedia,playlists.get(playlistIndex).playlistID)
        println("Aggregation final: ${aggregatedMedia.map { it.mediaID }}")

        return aggregatedMedia
    }
//    fun prioritizeSingleMediaId(mediaItems: List<Media>, targetMediaId: String): List<Media> {
//        return mediaItems.sortedBy { it.playlistID != targetMediaId }
//    }
    fun prioritizeSingleMediaId(mediaItems: List<Media>, targetMediaId: String): List<Media> {
        // Find the index of target playlist
        val targetIndex = mediaItems.indexOfFirst { it.playlistID == targetMediaId }

        // Create a new list starting from target index to end
        val firstPart = mediaItems.subList(targetIndex, mediaItems.size)
        // Create a list from start to target index
        val secondPart = mediaItems.subList(0, targetIndex)

        // Combine both parts
        return firstPart + secondPart
    }
    fun updatePlaylistMedia(playlistId: String, mediaId: String){
        lastPlayedMediaMap[playlistId] = mediaId
    }
    fun getCurrentMedia(playlistId: String): String? {
        return lastPlayedMediaMap[playlistId]
    }
    fun aggregateMediaFromPlaylists(playlists: List<Playlist>): List<Media>? {
        // Check if the list of playlists is empty
        if (playlists.isEmpty()) return null

        // Check if all playlists have no pages
        val allNoPages = playlists.all { it.page.isNullOrEmpty() }

        // Check if all playlists have a similar page (the same first page)
        val firstPage = playlists.first().page?.getOrNull(0)
        val allSimilarPages = playlists.all { it.page?.getOrNull(0) == firstPage }

        // If all have no pages or all have the same first page, aggregate the media lists
        if (allNoPages || allSimilarPages) {
            // Create a mutable list to store aggregated media
            val aggregatedMedia = mutableListOf<Media>()

            // Iterate over each playlist and get the next items
            for (playlist in playlists) {
                var nextItems = playlist.media
                if (playlist.maximumMedia > 0 && playlist.maximumMedia < nextItems.size) {
                    nextItems = getNextPlaylistItems(
                        playlist.playlistID,
                        playlist.media,
                        playlist.maximumMedia,
                    )
                }
                val mediaToAdd = if (playlist.shuffle) {
                    nextItems.shuffled()  // Shuffle using Kotlin's built-in function
                } else {
                    nextItems
                }
                aggregatedMedia.addAll(mediaToAdd)
            }

            return aggregatedMedia
        }

        // If neither condition is met, return null
        return null
    }





    fun getNextPlaylistItems(
        playlistId: String,
        mediaList: List<Media>,
        maximumMedia: Long
    ): List<Media> {
        val lastIndex = lastPlayedIndices[playlistId] ?: -1
        val maxItems =
            if (maximumMedia > 0) minOf(maximumMedia, mediaList.size.toLong()) else mediaList.size

        val startIndex = (lastIndex + 1) % mediaList.size
        val endIndex = ((startIndex.toLong() + maxItems.toLong() - 1) % mediaList.size).toInt()

        val items = if (startIndex <= endIndex) {
            mediaList.subList(startIndex, endIndex + 1)
        } else {
            mediaList.subList(startIndex, mediaList.size) + mediaList.subList(0, endIndex + 1)
        }

        lastPlayedIndices[playlistId] = endIndex
        return items
    }

    fun parseScreenTimes(inputList: List<String>, playerID: String): List<PlayerRuntimeLog> {
        val runtimeLogs = mutableListOf<PlayerRuntimeLog>()

        for (i in 0 until inputList.size step 2) {
            // Skip if not a start time entry
            if (!inputList[i].startsWith("ScreenStartTime")) continue

            val startTimeStr = inputList[i].split(":")[1].trim()+":"+inputList[i].split(":")[2].trim()+":"+inputList[i].split(":")[3].trim()
            val endTimeStr = if (i + 1 < inputList.size && inputList[i + 1].startsWith("ScreenEndTime")) {
                inputList[i + 1].split(":")[1].trim()+":"+inputList[i + 1].split(":")[2].trim()+":"+inputList[i + 1].split(":")[3].trim()
            } else startTimeStr

            runtimeLogs.add(
                PlayerRuntimeLog(
                    playerID = playerID,
                    startTime = startTimeStr,
                    endTime = endTimeStr,
                    status = null
                )
            )
        }

        return runtimeLogs
    }
}
