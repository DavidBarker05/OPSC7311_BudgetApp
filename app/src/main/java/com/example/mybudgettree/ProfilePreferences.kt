package com.example.mybudgettree

import android.content.Context

object ProfilePreferences {
    private const val PREFS = "profile_prefs"

    fun isPushEnabled(context: Context, uid: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(pushKey(uid), true)

    fun setPushEnabled(context: Context, uid: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(pushKey(uid), enabled)
            .apply()
    }

    private fun pushKey(uid: String) = "push_$uid"
}
