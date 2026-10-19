package com.digitalsignage.dsnetcoreplayer.data.player

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PlayerUpTime(
    @SerializedName("PlayerUpTimeId")
    val playerUpTimeId: String? = null,

    @SerializedName("PlayerID")
    val playerID: String? = null,

    @SerializedName("UpTimeStart")
    val upTimeStart: String? = null,

    @SerializedName("UpTimeEnd")
    val upTimeEnd: String? = null,

    @SerializedName("UpTimeStartString")
    val upTimeStartString: String? = null,

    @SerializedName("UpTimeEndString")
    val upTimeEndString: String? = null,

    @SerializedName("Day")
    val day: Int = 0,

    @SerializedName("IsUp")
    val isUp: Boolean = false,

    @SerializedName("IsDeleted")
    val isDeleted: Boolean = false,

    @SerializedName("ConfirmDelete")
    val confirmDelete: Boolean = false,

    @SerializedName("ResponseMessage")
    val responseMessage: String? = null,

    @SerializedName("ItemArray")
    val itemArray: List<Any>? = null,

    @SerializedName("ResponseCode")
    val responseCode: Int = 0,

    @SerializedName("UserToken")
    val userToken: String? = null
): Serializable
object PlayerScheduleChecker {

    /**
     * Checks if the player should be running based on current day and time
     * @param playerUpTimes List of player uptime configurations
     * @param dayOfWeek Current day of week (Calendar.SUNDAY=1, Calendar.MONDAY=2, etc.)
     * @param currentTime Current time as Calendar instance
     * @return True if player should be running, false otherwise
     */
    fun isPlayerAllowedToPlay(
        playerUpTimes: List<PlayerUpTime>?,
        dayOfWeek: Int,
        currentTime: Calendar
    ): Boolean {
        // Convert Calendar day to our format (Sunday=0, Monday=1, etc.)
        val dayIndex = when (dayOfWeek) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> return false
        }

        // Find the configuration for today
        val todaySchedule = playerUpTimes?.find { it.day == dayIndex }
            ?: return false

        // No need to check isDeleted as it's not used in logic

        // Parse the start and end time strings
        val dateFormat = SimpleDateFormat("M/d/yyyy h:mm:ss a", Locale.US)

        val startTime = try {
            todaySchedule.upTimeStartString?.let { dateFormat.parse(it) }
        } catch (e: Exception) {
            null
        } ?: return false

        val endTime = try {
            todaySchedule.upTimeEndString?.let { dateFormat.parse(it) }
        } catch (e: Exception) {
            null
        } ?: return false

        // Check if the dates are from an old year (indicating day is not selected)
        val startCalendar = Calendar.getInstance().apply { time = startTime }
        val endCalendar = Calendar.getInstance().apply { time = endTime }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        if (startCalendar.get(Calendar.YEAR) < currentYear - 1 ||
            endCalendar.get(Calendar.YEAR) < currentYear - 1) {
            return false // Day is not selected (old date indicates inactive)
        }

        // Extract only the time portion for comparison
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentSecond = currentTime.get(Calendar.SECOND)
        val currentTimeInSeconds = currentHour * 3600 + currentMinute * 60 + currentSecond

        val startHour = startCalendar.get(Calendar.HOUR_OF_DAY)
        val startMinute = startCalendar.get(Calendar.MINUTE)
        val startSecond = startCalendar.get(Calendar.SECOND)
        val startTimeInSeconds = startHour * 3600 + startMinute * 60 + startSecond

        val endHour = endCalendar.get(Calendar.HOUR_OF_DAY)
        val endMinute = endCalendar.get(Calendar.MINUTE)
        val endSecond = endCalendar.get(Calendar.SECOND)
        val endTimeInSeconds = endHour * 3600 + endMinute * 60 + endSecond

        // Special case: if start and end times are the same (like 12:00:00 AM to 12:00:00 AM)
        // This typically means the player runs all day
        if (startTimeInSeconds == endTimeInSeconds) {
            // If both are midnight (00:00), it means all day
            return if (startTimeInSeconds == 0) {
                true
            } else {
                // If they're the same but not midnight, treat as no play time
                false
            }
        }

        // Check if start time is after end time (invalid range)
        if (startTimeInSeconds > endTimeInSeconds) {
            return false // Invalid time range
        }

        // Normal time span within the same day
        return currentTimeInSeconds >= startTimeInSeconds && currentTimeInSeconds <= endTimeInSeconds
    }
    fun isPlayerAllowedToPlayNow(playerUpTimes: List<PlayerUpTime>?): Boolean {
        val currentTime = Calendar.getInstance()
        val currentDayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK)
        return isPlayerAllowedToPlay(playerUpTimes, currentDayOfWeek, currentTime)
    }
}