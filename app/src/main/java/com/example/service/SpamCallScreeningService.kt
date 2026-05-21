package com.example.service

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.BlockedCallEntity
import com.example.data.repository.CallBlockerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpamCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val application = applicationContext
        val database = AppDatabase.getDatabase(application)
        val repository = CallBlockerRepository(application, database.blockedCallDao())

        // Default: allow call
        val allowResponse = CallResponse.Builder().build()

        // 1. Check if the overall service is enabled
        if (!repository.isServiceEnabled) {
            respondToCall(callDetails, allowResponse)
            return
        }

        val handle = callDetails.handle
        if (handle == null || handle.scheme != "tel") {
            respondToCall(callDetails, allowResponse)
            return
        }

        val rawNumber = handle.schemeSpecificPart ?: ""
        if (rawNumber.isBlank()) {
            respondToCall(callDetails, allowResponse)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 2. Check local whitelist
                val isWhitelisted = repository.existsInWhitelist(rawNumber)

                // 3. Check system contacts list
                val isInContacts = isNumberInContacts(application, rawNumber)

                val blockNonContacts = repository.isBlockNonContactsEnabled

                if (isInContacts || isWhitelisted) {
                    // Number is allowed! Log the screened call
                    val log = BlockedCallEntity(
                        phoneNumber = rawNumber,
                        timestamp = System.currentTimeMillis(),
                        reason = if (isInContacts) "In Contacts" else "In Whitelist",
                        wasBlocked = false
                    )
                    repository.insertBlockedCall(log)
                    respondToCall(callDetails, allowResponse)
                } else if (blockNonContacts) {
                    // Number not in contacts and not whitelisted -> BLOCK IT!
                    val log = BlockedCallEntity(
                        phoneNumber = rawNumber,
                        timestamp = System.currentTimeMillis(),
                        reason = "Not in contacts (Unknown)",
                        wasBlocked = true
                    )
                    repository.insertBlockedCall(log)

                    val blockResponse = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false) // Keep in system call logs so user knows it happened
                        .setSkipNotification(true) // Silent blocking
                        .build()

                    respondToCall(callDetails, blockResponse)
                } else {
                    // Service is on, but blocking of non-contacts is disabled. Just log and allow.
                    val log = BlockedCallEntity(
                        phoneNumber = rawNumber,
                        timestamp = System.currentTimeMillis(),
                        reason = "Service Active (Allow)",
                        wasBlocked = false
                    )
                    repository.insertBlockedCall(log)
                    respondToCall(callDetails, allowResponse)
                }
            } catch (e: Exception) {
                Log.e("SpamCallScreening", "Error screening call details", e)
                respondToCall(callDetails, allowResponse)
            }
        }
    }

    private fun isNumberInContacts(context: Context, rawNumber: String): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(rawNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.count > 0) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("SpamCallScreening", "Contacts query failed", e)
        }
        return false
    }
}
