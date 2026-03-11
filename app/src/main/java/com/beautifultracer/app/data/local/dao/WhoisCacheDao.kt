package com.beautifultracer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.beautifultracer.app.data.local.entity.WhoisCacheEntity

@Dao
interface WhoisCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(whois: WhoisCacheEntity)

    @Query("SELECT * FROM whois_cache WHERE ipAddress = :ip LIMIT 1")
    suspend fun getByIp(ip: String): WhoisCacheEntity?

    @Query("DELETE FROM whois_cache WHERE fetchedAt < :expiryTimestamp")
    suspend fun deleteExpired(expiryTimestamp: Long)

    @Query("DELETE FROM whois_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM whois_cache")
    suspend fun getCacheSize(): Int
}
