package com.digitalsignage.dsnetcoreplayer.prefrences

import android.content.Context

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun addString(key: String, value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    fun addBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun addInt(key: String, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }
    fun addLong(key: String, value: Long) {
        editor.putLong(key, value)
        editor.apply()
    }
    fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
    fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }


}