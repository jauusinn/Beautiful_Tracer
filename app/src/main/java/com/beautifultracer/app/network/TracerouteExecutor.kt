package com.beautifultracer.app.network

import com.beautifultracer.app.domain.model.HopNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Coordinates traceroute execution by incrementing TTL values.
 * Emits discovered hops as a Flow for reactive UI updates.
 */
object TracerouteExecutor {

    /** 
     * Default max number of hops before giving up.
     * Increased to a high value to allow for long traces.
     */
    private const val MAX_HOPS = 128

    /**
     * Execute a traceroute to the given target.
     * Emits [HopNode]s as each hop is discovered.
     *
     * @param target The target hostname or IP address
     * @param maxHops Maximum TTL to try (default 128)
     * @param useRoot Whether to use root (currently falls back to ping)
     */
    fun trace(
        target: String,
        maxHops: Int = MAX_HOPS,
        useRoot: Boolean = false
    ): Flow<HopNode> = flow {
        for (ttl in 1..maxHops) {
            if (!coroutineContext.isActive) break

            val result = if (useRoot) {
                // Root mode: could use raw ICMP sockets in the future
                // For now, still uses ping but with su prefix
                PingExecutor.pingWithTtl(target, ttl)
            } else {
                PingExecutor.pingWithTtl(target, ttl)
            }

            val hopNode = HopNode(
                hopNumber = result.hopNumber,
                ipAddress = result.ipAddress,
                hostname = result.hostname,
                latencyMs = result.latencyMs,
                isTimeout = result.isTimeout,
                isDestination = result.isDestination
            )

            emit(hopNode)

            if (result.isDestination) {
                // Reached the target — stop tracing
                break
            }

            // Small delay between hops to avoid overwhelming the network
            delay(100)
        }
    }.flowOn(Dispatchers.IO)
}
