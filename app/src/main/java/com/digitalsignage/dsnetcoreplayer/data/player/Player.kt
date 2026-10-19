package com.digitalsignage.dsnetcoreplayer.data.player

import androidx.media3.common.Player
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "player")

class Player (
    @PrimaryKey(autoGenerate = false)
    @SerializedName("PlayerID")
    val playerID: String,

    @SerializedName("AccountID")
    val accountID: String,

    @SerializedName("Organization")
    val organization: String,

    @SerializedName("EnableReport")
    val enableReport: Boolean,

    @SerializedName("Name")
    val name: String,

    @SerializedName("Description")
    val description: String,

    @SerializedName("ImgUrl")
    val imgURL: String,

    @SerializedName("ConnectionInterval")
    val connectionInterval: Long,

    @SerializedName("LastContact")
    val lastContact: String,

    @SerializedName("ChannelId")
    val channelID: String,

    @SerializedName("ReleasedChanges")
    val releasedChanges: Boolean,

    @SerializedName("SmileFileLength")
    val smileFileLength: String? = null,

    @SerializedName("Active")
    val active: Boolean,

    @SerializedName("PlayerType")
    val playerType: String,

    @SerializedName("RebootTime")
    val rebootTime: String,

    @SerializedName("RebootTimeString")
    val rebootTimeString: String? = null,

    @SerializedName("PlaylistGenerated")
    val playlistGenerated: String,

    @SerializedName("IsRegistered")
    val isRegistered: String? = null,

    @SerializedName("EmailSendingTime")
    val emailSendingTime: String? = null,

    @SerializedName("TimeZoneId")
    val timeZoneID: String,

    @SerializedName("ErrorEmail")
    val errorEmail: List<String?>,

    @SerializedName("HasError")
    val hasError: Boolean,

    @SerializedName("PlayerVersion")
    val playerVersion: String,

    @SerializedName("PlayerGroupId")
    val playerGroupID: String? = null,

    @SerializedName("Password")
    val password: String? = null,

    @SerializedName("LicenceId")
    val licenceID: String,

    @SerializedName("HEXCode")
    val hexCode: String? = null,

    @SerializedName("IsDeadPlayer")
    val isDeadPlayer: Boolean,

    @SerializedName("PlayerUpTimes")
    val playerUpTimes: List<PlayerUpTime>,

    @SerializedName("GroupList")
    val groupList: List<String?>,

    @SerializedName("Playlist")
    val playlist: List<Playlist>,

    @SerializedName("License")
    val license: String? = null,

    @SerializedName("LicenseFinish")
    val licenseFinish: String,

    @SerializedName("DesignerTextAlignment")
    val designerTextAlignment: Long,

    @SerializedName("IsDeleted")
    val isDeleted: Boolean,

    @SerializedName("ResponseMessage")
    val responseMessage: String? = null,

    @SerializedName("ItemArray")
    val itemArray: List<String?>,

    @SerializedName("ResponseCode")
    val responseCode: Long,

    @SerializedName("UserToken")
    val userToken: String? = null,

    @SerializedName("SoftRestart")
    val softRestart: Boolean,

    @SerializedName("ReportUptime")
    val reportUptime: Boolean? =false


): Serializable {
    fun getPlaylistById(playlistID: String): Playlist? {
        return playlist.find { it.playlistID == playlistID }
    }
}


