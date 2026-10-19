package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Zone(
    @SerializedName("PageZoneID")
    val pageZoneId: String,

    @SerializedName("PageID")
    val pageId: String,

    @SerializedName("X")
    val x: Double,

    @SerializedName("Y")
    val y: Double,

    @SerializedName("Width")
    val width: Double,

    @SerializedName("Height")
    val height: Double,

    @SerializedName("ZIndex")
    val zindex: Int,

    @SerializedName("Name")
    val name: String,

    @SerializedName("ZoneType")
    val zoneType: Int,

    @SerializedName("Widget")
    val widget: Widget,

    @SerializedName("Playlist")
    val playlist: Playlist,

    @SerializedName("MediaID")
    val mediaId: String?, // Changed from Any? to String?

    @SerializedName("PlaylistID")
    val playlistId: String?, // Changed from Any? to String?

    @SerializedName("imgUrl")
    val imgUrl: String,

    @SerializedName("ItemID")
    val itemId: String?,

    @SerializedName("Animation")
    val animation: String,

    @SerializedName("TextFor_Edior")
    val textForEdior: String?, // Changed from Any? to String?

    @SerializedName("ZoneDataName")
    val zoneDataName: String?,

    @SerializedName("AllowCache")
    val allowCache: Boolean?,

    @SerializedName("ReportPlayback")
    val reportPlayback: Boolean?,

    @SerializedName("SimpleText")
    val simpleText: String?, // Changed from Any? to String?

    @SerializedName("SimpleTextAndroid")
    val simpleTextAndroid: String?, // Changed from Any? to String?

    @SerializedName("Interaction")
    val interaction: List<Interaction?>?, // Changed from List<Any?> to List<String?>

    @SerializedName("Lock")
    val lock: Boolean,

    @SerializedName("IsDeleted")
    val isDeleted: Boolean,

    @SerializedName("ResponseMessage")
    val responseMessage: String?, // Changed from Any? to String?

    @SerializedName("ItemArray")
    val itemArray: List<String?>, // Changed from List<Any?> to List<String?>

    @SerializedName("ResponseCode")
    val responseCode: Long,

    @SerializedName("UserToken")
    val userToken: String? // Changed from Any? to String?
):Serializable
