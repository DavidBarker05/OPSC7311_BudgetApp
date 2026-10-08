package com.example.mybudgettree

import android.content.Context
import java.util.UUID

/**
 * Identifies this install of the app, so each device can keep its own local image paths for the same expense or income
 * (see `imagePaths` on [com.example.mybudgettree.database.entries.Expense])
 *
 * The ID is a random UUID made the first time it's asked for and kept in SharedPreferences. It says nothing about the
 * device or its owner, so it's safe to store in the database. Reinstalling the app or clearing its data creates a new
 * one, which also matches the local image files being lost at that point
 */
object DeviceId {
    private const val PREFS = "device_prefs"
    private const val KEY = "device_id"

    /**
     * Gets this install's ID, creating and saving it the first time
     *
     * @param context Any context, used to reach SharedPreferences
     * @return The device ID
     */
    @Synchronized
    fun get(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
