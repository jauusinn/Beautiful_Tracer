package com.beautifultracer.app.domain.model

/**
 * Domain model representing a single hop in the traceroute.
 */
data class HopNode(
    val id: Long = 0,
    val hopNumber: Int,
    val ipAddress: String,
    val hostname: String? = null,
    val latencyMs: Float = -1f,
    val packetLoss: Float = 0f,
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val countryCode: String? = null,
    val flagEmoji: String = "",
    val isTimeout: Boolean = false,
    val isPinging: Boolean = false,
    val isDestination: Boolean = false,
    val displayAddress: String? = null
)
