package com.example.housefinder.ui.common

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(userId: Int) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun getUserId(): Int? {
        val value = prefs.getInt(KEY_USER_ID, -1)
        return if (value == -1) null else value
    }

    fun getLastAlertCheck(userId: Int): Long =
        prefs.getLong("$KEY_LAST_ALERT_CHECK$userId", System.currentTimeMillis() - DEFAULT_ALERT_LOOKBACK_MS)

    fun setLastAlertCheck(userId: Int, timestamp: Long) {
        prefs.edit().putLong("$KEY_LAST_ALERT_CHECK$userId", timestamp).apply()
    }

    companion object {
        private const val PREFS_NAME = "house_finder_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_ALERT_CHECK = "last_alert_check_"
        private const val DEFAULT_ALERT_LOOKBACK_MS = 3L * 24L * 60L * 60L * 1000L
    }
}
