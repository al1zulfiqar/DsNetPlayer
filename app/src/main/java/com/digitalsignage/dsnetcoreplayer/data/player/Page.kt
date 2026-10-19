package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Page(
    @SerializedName("PageID")
    val pageId: String,
    @SerializedName("Name")
    val name: String,
    @SerializedName("TemplateId")
    val templateId: Any?,
    @SerializedName("AccountID")
    val accountId: String,
    @SerializedName("Theme")
    val theme: Any?,
    @SerializedName("OverLayMediaId")
    val overLayMediaId: Any?,
    @SerializedName("Resolution")
    val resolution: String,
    @SerializedName("Orientation")
    val orientation: String,
    @SerializedName("Height")
    val height: Long,
    @SerializedName("Width")
    val width: Long,
    @SerializedName("Order")
    val order: Long,
    @SerializedName("BackgroundColor")
    val backgroundColor: String,
    @SerializedName("BackgroundImageMed")
    val backgroundImageMed: String?,
    @SerializedName("BackUrl")
    val backUrl: String?,
    @SerializedName("Zone")
    val zone: List<Zone>,
    @SerializedName("ParentID")
    val parentId: Any?,
    @SerializedName("CollectionFlag")
    val collectionFlag: Boolean,
    val editPageFlag: Boolean,
    @SerializedName("CollectionTimeOut")
    val collectionTimeOut: Long,
    @SerializedName("IsTemplate")
    val isTemplate: Any?,
    @SerializedName("ThumbnailUrl")
    val thumbnailUrl: Any?,
    @SerializedName("CategoryID")
    val categoryId: Any?,
    @SerializedName("FolderID")
    val folderId: Any?,
    @SerializedName("Base64TemplatePage")
    val base64TemplatePage: Any?,
    @SerializedName("CreatedDate")
    val createdDate: Any?,
    @SerializedName("IsDeleted")
    val isDeleted: Boolean,
    @SerializedName("ResponseMessage")
    val responseMessage: Any?,
    @SerializedName("ItemArray")
    val itemArray: List<Any?>,
    @SerializedName("ResponseCode")
    val responseCode: Long,
    @SerializedName("UserToken")
    val userToken: Any?,
): Serializable
