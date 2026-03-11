package com.beautifultracer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traceroute_hops")
data class TracerouteHopEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val hopNumber: Int,
    val ipAddress: String,
    val hostname: String?,
    val latencyMs: Float = -1f,
    val packetLoss: Float = 0f,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val countryCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
