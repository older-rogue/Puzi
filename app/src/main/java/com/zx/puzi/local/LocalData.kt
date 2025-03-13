package com.zx.puzi.local

import android.content.Context

object LocalData {
    private const val PREF_NAME = "local_data"
    private const val KEY_IS_FIRST = "key_is_first"

    fun getIsFirst(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isFirst = sharedPreferences.getBoolean(KEY_IS_FIRST, true)
        return isFirst
    }


    fun setIsFirst(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean(KEY_IS_FIRST, false)
            .apply()
    }
} 