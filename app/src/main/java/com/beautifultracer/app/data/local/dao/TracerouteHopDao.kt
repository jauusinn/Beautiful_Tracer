package com.beautifultracer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beautifultracer.app.data.local.entity.TracerouteHopEntity

@Dao
interface TracerouteHopDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hop: TracerouteHopEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hops: List<TracerouteHopEntity>)

    @Query("SELECT * FROM traceroute_hops WHERE sessionId = :sessionId ORDER BY hopNumber ASC")
    suspend fun getHopsBySession(sessionId: String): List<TracerouteHopEntity>

    @Query(
        "UPDATE traceroute_hops SET latencyMs = :latencyMs, packetLoss = :packetLoss, " +
        "packetsSent = :packetsSent, packetsReceived = :packetsReceived " +
        "WHERE id = :hopId"
    )
    suspend fun updatePingStats(
        hopId: Long,
        latencyMs: Float,
        packetLoss: Float,
        packetsSent: Int,
        packetsReceived: Int
    )

    @Query("UPDATE traceroute_hops SET countryCode = :countryCode WHERE id = :hopId")
    suspend fun updateCountryCode(hopId: Long, countryCode: String)

    @Query("DELETE FROM traceroute_hops WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("DELETE FROM traceroute_hops")
    suspend fun clearAll()
}
