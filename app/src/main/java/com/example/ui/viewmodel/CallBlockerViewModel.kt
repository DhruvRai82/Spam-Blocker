package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BlockedCallEntity
import com.example.data.database.WhitelistedNumberEntity
import com.example.data.repository.CallBlockerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

class CallBlockerViewModel(private val repository: CallBlockerRepository) : ViewModel() {

    // UI State settings
    var isServiceEnabled by mutableStateOf(repository.isServiceEnabled)
        private set

    var isBlockNonContactsEnabled by mutableStateOf(repository.isBlockNonContactsEnabled)
        private set

    var deviceContactCount by mutableStateOf(0)
        private set

    var deviceContacts by mutableStateOf<List<DeviceContact>>(emptyList())
        private set

    // Observe logs
    val blockedCalls: StateFlow<List<BlockedCallEntity>> = repository.allBlockedCalls
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe local whitelist
    val whitelistedNumbers: StateFlow<List<WhitelistedNumberEntity>> = repository.whitelistedNumbers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleService() {
        viewModelScope.launch {
            val newValue = !isServiceEnabled
            repository.isServiceEnabled = newValue
            isServiceEnabled = newValue
        }
    }

    fun toggleBlockNonContacts() {
        viewModelScope.launch {
            val newValue = !isBlockNonContactsEnabled
            repository.isBlockNonContactsEnabled = newValue
            isBlockNonContactsEnabled = newValue
        }
    }

    fun addNumberToWhitelist(phoneNumber: String, name: String) {
        viewModelScope.launch {
            // Basic normalization
            val normalized = phoneNumber.trim().replace(" ", "").replace("-", "")
            if (normalized.isNotBlank()) {
                repository.insertWhitelistedNumber(
                    WhitelistedNumberEntity(
                        phoneNumber = normalized,
                        name = name.ifBlank { "Unknown Whitelist" }
                    )
                )
            }
        }
    }

    fun removeNumberFromWhitelist(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteWhitelistedNumber(phoneNumber)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllBlockedCalls()
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteBlockedCall(id)
        }
    }

    fun updateContactCount(context: Context) {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            deviceContactCount = 0
            deviceContacts = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val list = mutableListOf<DeviceContact>()
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0 && numIdx >= 0) {
                            val name = cursor.getString(nameIdx) ?: "Unnamed"
                            val num = cursor.getString(numIdx) ?: ""
                            if (num.isNotBlank()) {
                                list.add(DeviceContact(name, num))
                            }
                        }
                    }
                }
                val distinctList = list.distinctBy { it.phoneNumber.trim().replace(" ", "").replace("-", "") }
                deviceContacts = distinctList
                deviceContactCount = distinctList.size
            } catch (e: Exception) {
                Log.e("CallBlockerVM", "Failed to query contacts details", e)
                deviceContactCount = 0
                deviceContacts = emptyList()
            }
        }
    }

    fun simulateCall(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            val normalized = phoneNumber.trim().replace(" ", "").replace("-", "")
            if (normalized.isBlank()) return@launch

            val serviceActive = isServiceEnabled
            val blockNonContacts = isBlockNonContactsEnabled
            val isWhitelisted = repository.existsInWhitelist(normalized)
            val isInContacts = checkContactExistsInSystem(context, normalized)

            val wasBlocked: Boolean
            val reason: String

            if (!serviceActive) {
                wasBlocked = false
                reason = "Shield Inactive"
            } else if (isInContacts) {
                wasBlocked = false
                reason = "In Device Contacts"
            } else if (isWhitelisted) {
                wasBlocked = false
                reason = "In Custom Whitelist"
            } else if (blockNonContacts) {
                wasBlocked = true
                reason = "Unknown Number (Blocked)"
            } else {
                wasBlocked = false
                reason = "Allowed (Block Off)"
            }

            repository.insertBlockedCall(
                BlockedCallEntity(
                    phoneNumber = normalized,
                    wasBlocked = wasBlocked,
                    reason = reason,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun checkContactExistsInSystem(context: Context, rawNumber: String): Boolean {
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
            Log.e("CallBlockerVM", "Contacts query failed", e)
        }
        return false
    }

    fun addContactToDeviceBook(context: Context, name: String, phoneNumber: String): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("CallBlockerVM", "Missing WRITE_CONTACTS permission")
            return false
        }
        return try {
            val ops = ArrayList<android.content.ContentProviderOperation>()

            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build())

            // Insert Display Name
            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build())

            // Insert Phone Number
            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build())

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            updateContactCount(context)
            true
        } catch (e: Exception) {
            Log.e("CallBlockerVM", "Failed to add system contact", e)
            false
        }
    }

    fun deleteContactFromDeviceBook(context: Context, phoneNumber: String): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("CallBlockerVM", "Missing WRITE_CONTACTS permission")
            return false
        }
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )
            var contactIdDeleted = false
            cursor?.use { c ->
                val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    if (idIdx >= 0 && numIdx >= 0) {
                        val num = c.getString(numIdx) ?: ""
                        if (num.trim().replace(" ", "").replace("-", "") == phoneNumber.trim().replace(" ", "").replace("-", "")) {
                            val contactId = c.getString(idIdx)
                            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
                            context.contentResolver.delete(uri, null, null)
                            contactIdDeleted = true
                        }
                    }
                }
            }
            updateContactCount(context)
            contactIdDeleted
        } catch (e: Exception) {
            Log.e("CallBlockerVM", "Failed to delete system contact", e)
            false
        }
    }
}

class CallBlockerViewModelFactory(private val repository: CallBlockerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallBlockerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallBlockerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
