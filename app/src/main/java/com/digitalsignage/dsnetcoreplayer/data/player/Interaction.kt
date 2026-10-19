package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Interaction(
    @SerializedName("InteractionID")
    val interactionId: String? = null,

    @SerializedName("Gesture")
    val gesture: String? = null,

    @SerializedName("Action")
    val action: String? = null,

    @SerializedName("TargetPageID")
    val targetPageId: String? = null,

    @SerializedName("ZoneID")
    val zoneId: String? = null
) : Serializable
