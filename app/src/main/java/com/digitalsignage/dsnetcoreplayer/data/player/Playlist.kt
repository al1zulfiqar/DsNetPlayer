package com.digitalsignage.dsnetcoreplayer.data.player


import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class Playlist (
    @SerializedName("ChannelPlaylistID")
    val channelPlaylistID: String,

    @SerializedName("Order")
    val order: Long,

    @SerializedName("ChannelID")
    val channelID: String,

    @SerializedName("Shuffle")
    val shuffle: Boolean,

    @SerializedName("Length")
    val length: Long,

    @SerializedName("MaximumMedia")
    val maximumMedia: Long,

    @SerializedName("Schedule")
    val schedule: Schedule? = null,

    @SerializedName("Page")
    val page: List<Page>?,

    @SerializedName("PageID")
    val pageID: String,

    @SerializedName("playerGroupPlaylistId")
    val playerGroupPlaylistID: String? = null,

    @SerializedName("PlayerGroupName")
    val playerGroupName: String? = null,

    @SerializedName("CollectionTimeOut")
    val collectionTimeOut: Long,

    val plygrpModel: String? = null,

    @SerializedName("PlaylistID")
    val playlistID: String,

    @SerializedName("Name")
    val name: String,

    @SerializedName("AccountID")
    val accountID: String,

    @SerializedName("GroupID")
    val groupID: String? = null,

    @SerializedName("PlaylistLenght")
    val playlistLenght: String,

    @SerializedName("ImgUrl")
    val imgURL: String,

    @SerializedName("DateCreated")
    val dateCreated: String,

    @SerializedName("Media")
    val media: List<Media>,

    @SerializedName("IsDeleted")
    val isDeleted: Boolean,

    @SerializedName("ResponseMessage")
    val responseMessage: String? = null,

    @SerializedName("ItemArray")
    val itemArray: List<String?>,

    @SerializedName("ResponseCode")
    val responseCode: Long,

    @SerializedName("UserToken")
    val userToken: String? = null
): Serializable
