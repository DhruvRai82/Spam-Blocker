package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {
    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC")
    fun getAllBlockedCalls(): Flow<List<BlockedCallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedCall(call: BlockedCallEntity)

    @Query("DELETE FROM blocked_calls")
    suspend fun clearAllBlockedCalls()

    @Query("DELETE FROM blocked_calls WHERE id = :id")
    suspend fun deleteBlockedCall(id: Int)

    @Query("SELECT * FROM whitelisted_numbers ORDER BY name ASC")
    fun getAllWhitelistedNumbers(): Flow<List<WhitelistedNumberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWhitelistedNumber(number: WhitelistedNumberEntity)

    @Query("DELETE FROM whitelisted_numbers WHERE phoneNumber = :number")
    suspend fun deleteWhitelistedNumber(number: String)

    @Query("SELECT EXISTS(SELECT 1 FROM whitelisted_numbers WHERE phoneNumber = :number LIMIT 1)")
    suspend fun existsInWhitelist(number: String): Boolean
}
