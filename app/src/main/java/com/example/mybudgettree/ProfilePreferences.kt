package com.example.mybudgettree

import android.content.Context

object ProfilePreferences {
    private const val PREFS = "profile_prefs"

    fun isPushEnabled(context: Context, username: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(pushKey(username), true)

    fun setPushEnabled(context: Context, username: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(pushKey(username), enabled)
            .apply()
    }

    private fun pushKey(username: String) = "push_$username"
}
