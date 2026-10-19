package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Widget (
    @SerializedName("WidgetID")
    val widgetID: String? = null, // Changed from Any? to String?

    @SerializedName("Name")
    val name: String? = null, // Changed from Any? to String?

    @SerializedName("WidgetUrl")
    val widgetURL: String? = null,

    @SerializedName("IsPublic")
    val isPublic: Boolean,

    @SerializedName("WidgetType")
    val widgetType: String? = null, // Changed from Any? to String?

    @SerializedName("Category")
    val category: String? = null, // Changed from Any? to String?

    @SerializedName("AccountID")
    val accountID: String? = null,
    @SerializedName("ImageUrl")
    val imageURL: String? = null,

    @SerializedName("WidgetParam")
    val widgetParam: List<WidgetParam>? = null
): Serializable
