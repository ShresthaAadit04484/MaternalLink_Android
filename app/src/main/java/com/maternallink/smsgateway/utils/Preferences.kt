package com.maternallink.smsgateway.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Preferences {
    private const val PREFS_NAME = "sms_gateway_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_SECRET_TOKEN = "secret_token"
    private const val KEY_SYNC_INTERVAL = "sync_interval"
    private const val KEY_MAX_MESSAGES = "max_messages"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val KEY_BATTERY_OPTIMIZATION = "battery_optimization"

    // Default values
    private const val DEFAULT_SERVER_URL = "http://192.168.1.100:8000/"
    private const val DEFAULT_SECRET_TOKEN = "test-secret-123-change-in-production"
    private const val DEFAULT_SYNC_INTERVAL = 30 // seconds
    private const val DEFAULT_MAX_MESSAGES = 10

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(context: Context, url: String) {
        getPrefs(context).edit { putString(KEY_SERVER_URL, url) }
    }

    fun getSecretToken(context: Context): String {
        return getPrefs(context).getString(KEY_SECRET_TOKEN, DEFAULT_SECRET_TOKEN) ?: DEFAULT_SECRET_TOKEN
    }

    fun setSecretToken(context: Context, token: String) {
        getPrefs(context).edit { putString(KEY_SECRET_TOKEN, token) }
    }

    fun getSyncInterval(context: Context): Int {
        return getPrefs(context).getInt(KEY_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL)
    }

    fun setSyncInterval(context: Context, interval: Int) {
        getPrefs(context).edit { putInt(KEY_SYNC_INTERVAL, interval) }
    }

    fun getMaxMessages(context: Context): Int {
        return getPrefs(context).getInt(KEY_MAX_MESSAGES, DEFAULT_MAX_MESSAGES)
    }

    fun setMaxMessages(context: Context, max: Int) {
        getPrefs(context).edit { putInt(KEY_MAX_MESSAGES, max) }
    }

    fun isServiceEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SERVICE_ENABLED, true)
    }

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SERVICE_ENABLED, enabled) }
    }

    fun getLastSync(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC, 0)
    }

    fun setLastSync(context: Context, timestamp: Long) {
        getPrefs(context).edit { putLong(KEY_LAST_SYNC, timestamp) }
    }

    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_OPTIMIZATION, false)
    }

    fun setBatteryOptimizationDisabled(context: Context, disabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BATTERY_OPTIMIZATION, disabled) }
    }

    fun validateSettings(context: Context): Pair<Boolean, String> {
        val url = getServerUrl(context)
        val secret = getSecretToken(context)

        return when {
            url.isBlank() -> Pair(false, "Server URL is required")
            !url.startsWith("http") -> Pair(false, "Invalid server URL")
            secret.isBlank() -> Pair(false, "Secret token is required")
            secret == DEFAULT_SECRET_TOKEN -> Pair(true, "Warning: Using default secret token")
            else -> Pair(true, "Settings are valid")
        }
    }
}