package com.digitalsignage.dsnetcoreplayer.data.requestdata

import com.google.gson.annotations.SerializedName

data class DeviceDetails(
    val playerID: String,
    @SerializedName("DeviceModel")
    val deviceModel: String,
    @SerializedName("AndroidVersion")
    val androidVersion: String,
    @SerializedName("MAC")
    val mac: String,
    @SerializedName("TimesZone")
    val timeZone: String,
    @SerializedName("ScreenResolution")
    val screenResolution: String,
    @SerializedName("LastIP")
    val lastIP: String
)