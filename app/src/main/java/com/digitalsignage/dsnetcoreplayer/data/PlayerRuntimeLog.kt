package com.digitalsignage.dsnetcoreplayer.data

import com.google.gson.annotations.SerializedName

data class PlayerRuntimeLog(
    @SerializedName("PlayerID") val playerID: String,
    @SerializedName("StartTime") val startTime: String,
    @SerializedName("EndTime") val endTime: String?,
    @SerializedName("Status") val status: Int?
)