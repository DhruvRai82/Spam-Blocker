package com.example.data.repository

import android.content.Context
import com.example.data.database.BlockedCallDao
import com.example.data.database.BlockedCallEntity
import com.example.data.database.WhitelistedNumberEntity
import kotlinx.coroutines.flow.Flow

class CallBlockerRepository(
    private val context: Context,
    private val blockedCallDao: BlockedCallDao
) {
    private val sharedPrefs = context.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BLOCK_NON_CONTACTS = "block_non_contacts"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
    }

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

    val allBlockedCalls: Flow<List<BlockedCallEntity>> = blockedCallDao.getAllBlockedCalls()

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
