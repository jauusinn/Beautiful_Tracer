package com.beautifultracer.app.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Result of a single ping attempt.
 * @property ipAddress The responding IP address.
 * @property latencyMs The measured round-trip time in milliseconds.
 * @property isReachable True if the host responded.
 * @property error Error message if the ping failed.
 */
data class PingResult(
    val ipAddress: String,
    val latencyMs: Float = -1f,
    val isReachable: Boolean = false,
    val error: String? = null
)

/**
 * Result of a single traceroute hop discovery.
 * @property hopNumber The TTL value used for this hop.
 * @property ipAddress The IP address of the router or destination responding.
 * @property hostname The resolved hostname if available.
 * @property latencyMs The measured round-trip time for this hop.
 * @property isReachable True if we received an ICMP response.
 * @property isDestination True if the responding IP is our final target.
 * @property isTimeout True if no response was received (Request Timed Out).
 * @property error Error message if execution failed.
 */
data class TracerouteHopResult(
    val hopNumber: Int,
    val ipAddress: String,
    val hostname: String? = null,
    val latencyMs: Float = -1f,
    val isReachable: Boolean = false,
    val isDestination: Boolean = false,
    val isTimeout: Boolean = false,
    val error: String? = null
)

/**
 * Executes system ping commands via Runtime.exec and parses the text output.
 * This object provides methods to measure latency and discover intermediate network hops.
 */
object PingExecutor {
    private const val TAG = "PingExecutor"

    /**
     * Pings a specific IP address once to measure connectivity and latency.
     * Uses: /system/bin/ping -c 1 -W [timeout] [ipAddress]
     * @param ipAddress The target IP to ping.
     * @param timeoutSeconds How long to wait for a response before timing out.
     * @return A [PingResult] containing the latency and reachability status.
     */
    suspend fun ping(ipAddress: String, timeoutSeconds: Int = 3): PingResult =
        withContext(Dispatchers.IO) {
            try {
                // Command construction: -c 1 (1 packet), -W (timeout in seconds)
                val command = arrayOf("ping", "-c", "1", "-W", timeoutSeconds.toString(), ipAddress)
                val process = Runtime.getRuntime().exec(command)

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }

                val exitCode = process.waitFor()
                val outputStr = output.toString()
                
                if (exitCode == 0) {
                    val latency = parseLatency(outputStr)
                    PingResult(
                        ipAddress = ipAddress,
                        latencyMs = latency ?: -1f,
                        isReachable = true
                    )
                } else {
                    PingResult(
                        ipAddress = ipAddress,
                        latencyMs = -1f,
                        isReachable = false,
                        error = "Exit code $exitCode"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ping failed for $ipAddress", e)
                PingResult(
                    ipAddress = ipAddress,
                    latencyMs = -1f,
                    isReachable = false,
                    error = e.message
                )
            }
        }

    /**
     * Executes a ping with a specific Time To Live (TTL) value.
     * This is used by traceroute to identify the IP of a router at a specific distance.
     * @param target The final destination IP.
     * @param ttl The number of hops before the packet should expire.
     * @param timeoutSeconds How long to wait for the "Time to live exceeded" response.
     * @return A [TracerouteHopResult] containing the IP of the responding router.
     */
    suspend fun pingWithTtl(
        target: String,
        ttl: Int,
        timeoutSeconds: Int = 3
    ): TracerouteHopResult = withContext(Dispatchers.IO) {
        try {
            // Command: -n (numeric output), -c 1 (1 packet), -t (set TTL), -W (timeout)
            val command = arrayOf(
                "ping",
                "-n",
                "-c", "1",
                "-t", ttl.toString(),
                "-W", timeoutSeconds.toString(),
                target
            )
            
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            process.waitFor()
            val outputStr = output.toString()
            
            Log.d(TAG, "TTL $ttl output: $outputStr")

            val respondingIp = parseRespondingIp(outputStr)
            val latency = parseLatency(outputStr)
            val isDestination = respondingIp != null && isDestinationReached(outputStr)

            if (respondingIp != null) {
                TracerouteHopResult(
                    hopNumber = ttl,
                    ipAddress = respondingIp,
                    latencyMs = latency ?: -1f,
                    isReachable = true,
                    isDestination = isDestination,
                    hostname = parseHostname(outputStr)
                )
            } else {
                // If no response, return the custom timeout identifier
                TracerouteHopResult(
                    hopNumber = ttl,
                    ipAddress = "X----X----X",
                    latencyMs = -1f,
                    isReachable = false,
                    isDestination = false,
                    isTimeout = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "pingWithTtl failed for $target TTL $ttl", e)
            TracerouteHopResult(
                hopNumber = ttl,
                ipAddress = "X----X----X",
                latencyMs = -1f,
                isReachable = false,
                isDestination = false,
                isTimeout = true,
                error = e.message
            )
        }
    }

    /**
     * Extracts latency value from the ping command's textual output.
     * Searches for patterns like "time=23.4 ms".
     * @param output The raw output string from the ping command.
     */
    private fun parseLatency(output: String): Float? {
        val regex = Regex("""time[=<](\d+\.?\d*)\s*ms""")
        val match = regex.find(output)
        return match?.groupValues?.get(1)?.toFloatOrNull()
    }

    /**
     * Extracts the IP address of the responding node from the ping output.
     * Correctly handles "From <IP>" for intermediate hops and "bytes from <IP>" for the destination.
     * @param output The raw output string from the ping command.
     */
    private fun parseRespondingIp(output: String): String? {
        // 1. Check for intermediate hop response
        val fromRegex = Regex("""[Ff]rom\s+([\d\.]+)""")
        fromRegex.find(output)?.let { match ->
            val ip = match.groupValues[1].trimEnd('.')
            if (isValidIp(ip)) return ip
        }

        // 2. Check for final destination response
        val bytesFromRegex = Regex("""bytes from\s+([\d\.]+)""")
        bytesFromRegex.find(output)?.let { match ->
            val ip = match.groupValues[1].trimEnd('.')
            if (isValidIp(ip)) return ip
        }

        return null
    }

    /**
     * Validates that a string matches the standard IPv4 pattern.
     */
    private fun isValidIp(ip: String): Boolean {
        return ip.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    }

    /**
     * Attempts to extract a hostname if provided in the ping output.
     * @param output The raw output string.
     */
    private fun parseHostname(output: String): String? {
        val regex = Regex("""[Ff]rom\s+([a-zA-Z0-9\-\.]+)\s+\(([\d\.]+)\)""")
        val match = regex.find(output)
        val host = match?.groupValues?.get(1)
        return if (host != null && host.any { it.isLetter() }) host else null
    }

    /**
     * Determines if the ping response indicates we have reached the final target.
     * True if "bytes from" is present without a "Time to live exceeded" message.
     */
    private fun isDestinationReached(output: String): Boolean {
        return output.contains("bytes from", ignoreCase = true) && 
               !output.contains("Time to live exceeded", ignoreCase = true)
    }
}
