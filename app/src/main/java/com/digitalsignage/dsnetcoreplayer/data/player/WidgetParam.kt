package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WidgetParam(
    @SerializedName("WidgetParamID")
    val widgetParamId: String,
    @SerializedName("WidgetID")
    val widgetId: String,
    @SerializedName("ParamName")
    val paramName: String,
    @SerializedName("Value")
    val value: String,
): Serializable