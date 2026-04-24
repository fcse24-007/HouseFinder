package com.example.housefinder.ui.common

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(userId: Int, role: String, name: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_ROLE, role)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun getUserId(): Int? {
        val value = prefs.getInt(KEY_USER_ID, -1)
        return if (value == -1) null else value
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun getDisplayName(): String? = prefs.getString(KEY_NAME, null)

    fun getLastAlertCheck(userId: Int): Long =
        prefs.getLong("$KEY_LAST_ALERT_CHECK$userId", System.currentTimeMillis() - DEFAULT_ALERT_LOOKBACK_MS)

    fun setLastAlertCheck(userId: Int, timestamp: Long) {
        prefs.edit().putLong("$KEY_LAST_ALERT_CHECK$userId", timestamp).apply()
    }

    companion object {
        private const val PREFS_NAME = "house_finder_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
        private const val KEY_NAME = "name"
        private const val KEY_LAST_ALERT_CHECK = "last_alert_check_"
        private const val DEFAULT_ALERT_LOOKBACK_MS = 3L * 24L * 60L * 60L * 1000L
    }
}

