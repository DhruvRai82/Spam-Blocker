package com.example.data.repository

import android.content.Context
import com.example.data.database.BlockedCallDao
import com.example.data.database.BlockedCallEntity
import com.example.data.database.WhitelistedNumberEntity
import com.example.data.database.SpamReportEntity
import kotlinx.coroutines.flow.Flow

class CallBlockerRepository(
    private val context: Context,
    private val blockedCallDao: BlockedCallDao
) {
    private val sharedPrefs = context.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCK_NON_CONTACTS = "block_non_contacts"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_MOCK_ROLE_GRANTED = "mock_role_granted"
        
        private const val KEY_FILTER_ROBOCALLS = "filter_robocalls"
        private const val KEY_FILTER_TELEMARKETING = "filter_telemarketing"
        private const val KEY_FILTER_SCAM = "filter_scam"
        private const val KEY_FILTER_UNKNOWN = "filter_unknown"
        private const val KEY_FILTER_SILENT = "filter_silent"
        private const val KEY_FILTER_INTERNATIONAL = "filter_intl"
        
        private const val KEY_DND_ENABLED = "dnd_enabled"
        private const val KEY_DND_START_HOUR = "dnd_start_hour"
        private const val KEY_DND_START_MIN = "dnd_start_min"
        private const val KEY_DND_END_HOUR = "dnd_end_hour"
        private const val KEY_DND_END_MIN = "dnd_end_min"
        
        private const val KEY_BLOCKED_PATTERNS = "blocked_patterns"

        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_AI_SENSITIVITY = "ai_sensitivity"
        private const val KEY_ICON_VARIANT = "icon_variant"
        private const val KEY_TELEMETRY_SHARE = "telemetry_share"
    }

    var activeTheme: String
        get() = sharedPrefs.getString(KEY_APP_THEME, "System") ?: "System"
        set(value) = sharedPrefs.edit().putString(KEY_APP_THEME, value).apply()

    var aiSensitivityVal: Float
        get() = sharedPrefs.getFloat(KEY_AI_SENSITIVITY, 0.75f)
        set(value) = sharedPrefs.edit().putFloat(KEY_AI_SENSITIVITY, value).apply()

    var selectedIconVariant: String
        get() = sharedPrefs.getString(KEY_ICON_VARIANT, "Defending Cobalt") ?: "Defending Cobalt"
        set(value) = sharedPrefs.edit().putString(KEY_ICON_VARIANT, value).apply()

    var isTelemetryShareEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_TELEMETRY_SHARE, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_TELEMETRY_SHARE, value).apply()

    var isBlockNonContactsEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_BLOCK_NON_CONTACTS, true)
        set(value) {
            sharedPrefs.edit().putBoolean(KEY_BLOCK_NON_CONTACTS, value).apply()
        }

    var isServiceEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) {
            sharedPrefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()
        }

    var isMockRoleGranted: Boolean
        get() = sharedPrefs.getBoolean(KEY_MOCK_ROLE_GRANTED, false)
        set(value) {
            sharedPrefs.edit().putBoolean(KEY_MOCK_ROLE_GRANTED, value).apply()
        }

    // Advanced block category properties
    var isFilterRobocallsEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_FILTER_ROBOCALLS, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FILTER_ROBOCALLS, value).apply()

    var isFilterTelemarketingEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_FILTER_TELEMARKETING, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FILTER_TELEMARKETING, value).apply()

    var isFilterScamEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_FILTER_SCAM, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FILTER_SCAM, value).apply()

    var isFilterUnknownEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_FILTER_UNKNOWN, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FILTER_UNKNOWN, value).apply()

    var isFilterSilentEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_FILTER_SILENT, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FILTER_SILENT, value).apply()

    var isFilterInternationalEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_FILTER_INTERNATIONAL, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FILTER_INTERNATIONAL, value).apply()

    // DND
    var isDndEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_DND_ENABLED, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_DND_ENABLED, value).apply()

    var dndStartHour: Int
        get() = sharedPrefs.getInt(KEY_DND_START_HOUR, 22)
        set(value) = sharedPrefs.edit().putInt(KEY_DND_START_HOUR, value).apply()

    var dndStartMinute: Int
        get() = sharedPrefs.getInt(KEY_DND_START_MIN, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_DND_START_MIN, value).apply()

    var dndEndHour: Int
        get() = sharedPrefs.getInt(KEY_DND_END_HOUR, 7)
        set(value) = sharedPrefs.edit().putInt(KEY_DND_END_HOUR, value).apply()

    var dndEndMinute: Int
        get() = sharedPrefs.getInt(KEY_DND_END_MIN, 0)
        set(value) = sharedPrefs.edit().putInt(KEY_DND_END_MIN, value).apply()

    // Patterns list (comma-separated strings)
    var blockedPatterns: String
        get() = sharedPrefs.getString(KEY_BLOCKED_PATTERNS, "+91-140,+1-800") ?: ""
        set(value) = sharedPrefs.edit().putString(KEY_BLOCKED_PATTERNS, value).apply()

    val allBlockedCalls: Flow<List<BlockedCallEntity>> = blockedCallDao.getAllBlockedCalls()

    val allSpamReports: Flow<List<SpamReportEntity>> = blockedCallDao.getAllSpamReports()

    suspend fun insertSpamReport(report: SpamReportEntity) {
        blockedCallDao.insertSpamReport(report)
    }

    suspend fun clearAllSpamReports() {
        blockedCallDao.clearAllSpamReports()
    }

    suspend fun insertBlockedCall(call: BlockedCallEntity) {
        blockedCallDao.insertBlockedCall(call)
    }

    suspend fun clearAllBlockedCalls() {
        blockedCallDao.clearAllBlockedCalls()
    }

    suspend fun deleteBlockedCall(id: Int) {
        blockedCallDao.deleteBlockedCall(id)
    }

    val whitelistedNumbers: Flow<List<WhitelistedNumberEntity>> = blockedCallDao.getAllWhitelistedNumbers()

    suspend fun insertWhitelistedNumber(number: WhitelistedNumberEntity) {
        blockedCallDao.insertWhitelistedNumber(number)
    }

    suspend fun deleteWhitelistedNumber(number: String) {
        blockedCallDao.deleteWhitelistedNumber(number)
    }

    suspend fun existsInWhitelist(number: String): Boolean {
        return blockedCallDao.existsInWhitelist(number)
    }
}
