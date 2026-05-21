package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String,
    val wasBlocked: Boolean
)

@Entity(tableName = "whitelisted_numbers")
data class WhitelistedNumberEntity(
    @PrimaryKey val phoneNumber: String,
    val name: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "spam_reports")
data class SpamReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val category: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val receivedCall: Boolean = true
)
