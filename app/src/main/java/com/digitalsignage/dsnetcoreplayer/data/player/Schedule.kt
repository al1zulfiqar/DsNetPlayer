package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Schedule(
    @SerializedName("ScheduleID")
    val scheduleId: String,
    @SerializedName("Start")
    val start: String,
    @SerializedName("End")
    val end: String,
    @SerializedName("StartString")
    val startString: Any?,
    @SerializedName("EndString")
    val endString: Any?,
    @SerializedName("Monday")
    val monday: Boolean,
    @SerializedName("Tuesday")
    val tuesday: Boolean,
    @SerializedName("Wednesday")
    val wednesday: Boolean,
    @SerializedName("Thursday")
    val thursday: Boolean,
    @SerializedName("Friday")
    val friday: Boolean,
    @SerializedName("Saturday")
    val saturday: Boolean,
    @SerializedName("Sunday")
    val sunday: Boolean,
    @SerializedName("DateRange")
    val dateRange: Boolean,
    @SerializedName("TimeRange")
    val timeRange: Boolean,
    @SerializedName("DaysRange")
    val daysRange: Boolean,
    @SerializedName("Override")
    val override: Boolean,
)