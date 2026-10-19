package com.digitalsignage.dsnetcoreplayer.data.player
import com.digitalsignage.dsnetcoreplayer.data.player.Schedule
import com.digitalsignage.dsnetcoreplayer.data.player.Widget
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Media (
    @SerializedName("MediaPlaylistID")
    val mediaPlaylistID: String,

    @SerializedName("PlaylistID")
    val playlistID: String,

    @SerializedName("Order")
    val order: Long,

    @SerializedName("AspectRatio")
    val aspectRatio: Boolean,

    @SerializedName("AllowCache")
    val allowCache: Boolean,

    @SerializedName("FullScreen")
    var fullScreen: Boolean,

    @SerializedName("Length")
    val length: String,

    val expiryTime: String,

    @SerializedName("FlagExpireTime")
    val flagExpireTime: String,

    val expiryTimeString: String? = null,

    @SerializedName("Schedule")
    val schedule: Schedule? = null,

    @SerializedName("Transition")
    val transition: Long,

    @SerializedName("FlagScaling")
    val flagScaling: String,

    @SerializedName("LoadingTime")
    val loadingTime: String? = null,

    @SerializedName("MediaID")
    var mediaID: String,

    @SerializedName("Name")
    private val _name: String, // Make this private

    @SerializedName("URL")
    val url: String,

    @SerializedName("ThumbnailUrl")
    val thumbnailURL: String,

    @SerializedName("Type")
    val type: String,

    @SerializedName("AccountID")
    val accountID: String? = null,

    @SerializedName("UserID")
    val userID: String? = null,

    @SerializedName("CreatedDate")
    val createdDate: String,

    @SerializedName("Lenght")
    val lenght: String,

    @SerializedName("Volume")
    val volume: Long,

    @SerializedName("CategoryID")
    val categoryID: String? = null,

    @SerializedName("ReportPlayback")
    val reportPlayback: Boolean,

    @SerializedName("Extension")
    val extension: String? = null,

    @SerializedName("EncodingVideo")
    val encodingVideo: Boolean,

    @SerializedName("PreviewThumbnailURL")
    val previewThumbnailURL: String? = null,

    @SerializedName("Size")
    val size: Long,

    @SerializedName("VideoThumbnail")
    val videoThumbnail: String? = null,

    @SerializedName("Widget")
    val widget: Widget,

    @SerializedName("FileSize")
    val fileSize: Long,

    @SerializedName("FolderID")
    val folderID: String? = null,

    @SerializedName("ParentFolderType")
    val parentFolderType: String? = null,

    @SerializedName("TemplateMediaID")
    val templateMediaID: String? = null,

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
) : Serializable {

    // Custom getter that formats the name as MediaID-Name
    val name: String
        get() = "$mediaID-$_name"

    // If you need the original name for any reason
    val originalName: String
        get() = _name
}