package com.digitalsignage.dsnetcoreplayer.network

data class SaveNetworkHealthResponse(
    val playerID: String,
    val DeviceModel: String,
    val MAC: String,
    val TimesZone: String,
    val ScreenResolution: String,
    val LastIP: String,
    val LastLocation: String?,
    val AndroidVersion: String,
    val IsDeleted: Boolean,
    val ResponseMessage: String,
    val ItemArray: List<Any>,
    val ResponseCode: Int,
    val UserToken: String?
)