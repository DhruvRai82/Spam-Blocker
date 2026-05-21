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
import com.example.data.database.SpamReportEntity
import com.example.data.repository.CallBlockerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.ArrayList

data class DeviceContact(
    val name: String,
    val phoneNumber: String,
    val isVerified: Boolean = false,
    val isFavorite: Boolean = false
)

class CallBlockerViewModel(private val repository: CallBlockerRepository) : ViewModel() {

    // General Protection Switchers
    var isServiceEnabled by mutableStateOf(repository.isServiceEnabled)
        private set

    var isBlockNonContactsEnabled by mutableStateOf(repository.isBlockNonContactsEnabled)
        private set

    // Emulator Role Mock Switcher
    var isMockRoleGranted by mutableStateOf(repository.isMockRoleGranted)
        private set

    // Filtering rules
    var isFilterRobocallsEnabled by mutableStateOf(repository.isFilterRobocallsEnabled)
        private set
    var isFilterTelemarketingEnabled by mutableStateOf(repository.isFilterTelemarketingEnabled)
        private set
    var isFilterScamEnabled by mutableStateOf(repository.isFilterScamEnabled)
        private set
    var isFilterUnknownEnabled by mutableStateOf(repository.isFilterUnknownEnabled)
        private set
    var isFilterSilentEnabled by mutableStateOf(repository.isFilterSilentEnabled)
        private set
    var isFilterInternationalEnabled by mutableStateOf(repository.isFilterInternationalEnabled)
        private set

    // DND schedule
    var isDndEnabled by mutableStateOf(repository.isDndEnabled)
        private set
    var dndStartHour by mutableStateOf(repository.dndStartHour)
        private set
    var dndStartMinute by mutableStateOf(repository.dndStartMinute)
        private set
    var dndEndHour by mutableStateOf(repository.dndEndHour)
        private set
    var dndEndMinute by mutableStateOf(repository.dndEndMinute)
        private set

    // Patterns list (parsed comma-separated values)
    var patternsList by mutableStateOf(parsePatterns(repository.blockedPatterns))
        private set

    // System list holder
    var deviceContactCount by mutableStateOf(0)
        private set
    var deviceContacts by mutableStateOf<List<DeviceContact>>(emptyList())
        private set

    // Flows
    val blockedCalls: StateFlow<List<BlockedCallEntity>> = repository.allBlockedCalls
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val whitelistedNumbers: StateFlow<List<WhitelistedNumberEntity>> = repository.whitelistedNumbers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val spamReports: StateFlow<List<SpamReportEntity>> = repository.allSpamReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // General Toggles
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

    fun toggleMockRoleGranted() {
        viewModelScope.launch {
            val newValue = !isMockRoleGranted
            repository.isMockRoleGranted = newValue
            isMockRoleGranted = newValue
        }
    }

    // Advanced block rules modifier
    fun toggleFilterRobocalls() {
        viewModelScope.launch {
            val newValue = !isFilterRobocallsEnabled
            repository.isFilterRobocallsEnabled = newValue
            isFilterRobocallsEnabled = newValue
        }
    }

    fun toggleFilterTelemarketing() {
        viewModelScope.launch {
            val newValue = !isFilterTelemarketingEnabled
            repository.isFilterTelemarketingEnabled = newValue
            isFilterTelemarketingEnabled = newValue
        }
    }

    fun toggleFilterScam() {
        viewModelScope.launch {
            val newValue = !isFilterScamEnabled
            repository.isFilterScamEnabled = newValue
            isFilterScamEnabled = newValue
        }
    }

    fun toggleFilterUnknown() {
        viewModelScope.launch {
            val newValue = !isFilterUnknownEnabled
            repository.isFilterUnknownEnabled = newValue
            isFilterUnknownEnabled = newValue
        }
    }

    fun toggleFilterSilent() {
        viewModelScope.launch {
            val newValue = !isFilterSilentEnabled
            repository.isFilterSilentEnabled = newValue
            isFilterSilentEnabled = newValue
        }
    }

    fun toggleFilterInternational() {
        viewModelScope.launch {
            val newValue = !isFilterInternationalEnabled
            repository.isFilterInternationalEnabled = newValue
            isFilterInternationalEnabled = newValue
        }
    }

    // DND
    fun toggleDndEnabled() {
        viewModelScope.launch {
            val newValue = !isDndEnabled
            repository.isDndEnabled = newValue
            isDndEnabled = newValue
        }
    }

    fun updateDndRange(startH: Int, startM: Int, endH: Int, endM: Int) {
        viewModelScope.launch {
            repository.dndStartHour = startH
            repository.dndStartMinute = startM
            repository.dndEndHour = endH
            repository.dndEndMinute = endM
            dndStartHour = startH
            dndStartMinute = startM
            dndEndHour = endH
            dndEndMinute = endM
        }
    }

    // Patterns
    private fun parsePatterns(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addPattern(pattern: String) {
        viewModelScope.launch {
            val current = patternsList.toMutableList()
            val trimmed = pattern.trim()
            if (trimmed.isNotEmpty() && !current.contains(trimmed)) {
                current.add(trimmed)
                val raw = current.joinToString(",")
                repository.blockedPatterns = raw
                patternsList = current
            }
        }
    }

    fun removePattern(pattern: String) {
        viewModelScope.launch {
            val current = patternsList.toMutableList()
            if (current.remove(pattern)) {
                val raw = current.joinToString(",")
                repository.blockedPatterns = raw
                patternsList = current
            }
        }
    }

    // Whitelist operators
    fun addNumberToWhitelist(phoneNumber: String, name: String) {
        viewModelScope.launch {
            val normalized = phoneNumber.trim().replace(" ", "").replace("-", "")
            if (normalized.isNotBlank()) {
                repository.insertWhitelistedNumber(
                    WhitelistedNumberEntity(
                        phoneNumber = normalized,
                        name = name.ifBlank { "Smart Whitelist Pass" }
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

    // Logs operators
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

    // Spam report operators
    fun addSpamReport(phoneNumber: String, category: String, description: String, receivedCall: Boolean) {
        viewModelScope.launch {
            val normalized = phoneNumber.trim().replace(" ", "").replace("-", "")
            if (normalized.isNotBlank()) {
                repository.insertSpamReport(
                    SpamReportEntity(
                        phoneNumber = normalized,
                        category = category,
                        description = description,
                        receivedCall = receivedCall
                    )
                )
            }
        }
    }

    fun clearSpamReports() {
        viewModelScope.launch {
            repository.clearAllSpamReports()
        }
    }

    // System Contact Reader
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
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED
                    ),
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val starredIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    while (cursor.moveToNext()) {
                        if (nameIdx >= 0 && numIdx >= 0) {
                            val name = cursor.getString(nameIdx) ?: "Unnamed"
                            val num = cursor.getString(numIdx) ?: ""
                            val starred = if (starredIdx >= 0) cursor.getInt(starredIdx) == 1 else false
                            if (num.isNotBlank()) {
                                // Add verification simulation to contacts
                                val isVip = starred || name.contains("Family") || name.contains("Mom") || name.contains("Work")
                                list.add(DeviceContact(
                                    name = name, 
                                    phoneNumber = num,
                                    isVerified = name.length % 2 == 0, // Mock verified badging
                                    isFavorite = starred
                                ))
                            }
                        }
                    }
                }
                val distinctList = list.distinctBy { it.phoneNumber.trim().replace(" ", "").replace("-", "") }
                deviceContacts = distinctList
                deviceContactCount = distinctList.size
            } catch (e: Exception) {
                Log.e("CallBlockerVM", "Failed to query system contacts", e)
                deviceContactCount = 0
                deviceContacts = emptyList()
            }
        }
    }

    // Smart Call Block Simulator
    fun simulateCall(phoneNumber: String, context: Context, forceCategory: String? = null) {
        viewModelScope.launch {
            val normalized = phoneNumber.trim().replace(" ", "").replace("-", "")
            if (normalized.isBlank()) return@launch

            val serviceActive = isServiceEnabled
            val blockNonContacts = isBlockNonContactsEnabled
            val isWhitelisted = repository.existsInWhitelist(normalized)
            val isInContacts = checkContactExistsInSystem(context, normalized)

            // Evaluate patterns blocking
            val matchesPattern = patternsList.isNotEmpty() && patternsList.any { normalized.startsWith(it) }

            // Evaluate category specific blocks
            val category = forceCategory ?: detectSpamCategory(normalized)
            val isSpamCategory = category == "Robocall" || category == "Telemarketer" || category == "Scam Likely" || category == "Fraud Alert"

            val isRoboBlocked = isSpamCategory && category == "Robocall" && isFilterRobocallsEnabled
            val isTeleBlocked = isSpamCategory && category == "Telemarketer" && isFilterTelemarketingEnabled
            val isScamBlocked = isSpamCategory && (category == "Scam Likely" || category == "Fraud Alert") && isFilterScamEnabled
            val isUnknownBlocked = !isInContacts && !isWhitelisted && matchesRuleOrSettings(blockNonContacts, isFilterUnknownEnabled)

            val wasBlocked: Boolean
            val reason: String

            if (!serviceActive) {
                wasBlocked = false
                reason = "Shield Inactive"
            } else if (isWhitelisted) {
                wasBlocked = false
                reason = "In Custom Whitelist"
            } else if (isInContacts) {
                wasBlocked = false
                reason = "In Contacts Directory"
            } else if (matchesPattern) {
                wasBlocked = true
                reason = "Pattern Blocker Match ($normalized)"
            } else if (isRoboBlocked) {
                wasBlocked = true
                reason = "Auto-Blocked: Robocall threat"
            } else if (isTeleBlocked) {
                wasBlocked = true
                reason = "Auto-Blocked: Telemarketing"
            } else if (isScamBlocked) {
                wasBlocked = true
                reason = "Auto-Blocked: Scam/Fraud Alert"
            } else if (isUnknownBlocked) {
                wasBlocked = true
                reason = "Unknown Number Blocked"
            } else {
                wasBlocked = false
                reason = "Allowed Caller"
            }

            repository.insertBlockedCall(
                BlockedCallEntity(
                    phoneNumber = phoneNumber,
                    wasBlocked = wasBlocked,
                    reason = reason,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun matchesRuleOrSettings(block1: Boolean, block2: Boolean): Boolean {
        return block1 || block2
    }

    private fun detectSpamCategory(number: String): String {
        return when {
            number.contains("140") || number.startsWith("140") -> "Telemarketer"
            number.startsWith("1800") || number.contains("800") -> "Robocall"
            number.length == 7 || number.all { it == '9' } -> "Scam Likely"
            number.startsWith("+99") -> "Fraud Alert"
            else -> "Unknown Caller"
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
            Log.e("CallBlockerVM", "Contacts database match failed", e)
        }
        return false
    }

    fun addContactToDeviceBook(context: Context, name: String, phoneNumber: String): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("CallBlockerVM", "No WRITE_CONTACTS clearance")
            return false
        }
        return try {
            val ops = ArrayList<android.content.ContentProviderOperation>()
            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build())

            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build())

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
            Log.e("CallBlockerVM", "Batch Contacts write failed", e)
            false
        }
    }

    fun deleteContactFromDeviceBook(context: Context, phoneNumber: String): Boolean {
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("CallBlockerVM", "No WRITE_CONTACTS clearance")
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
            Log.e("CallBlockerVM", "Contacts delete failed", e)
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
        throw IllegalArgumentException("Unknown ViewModel class descriptor")
    }
}
