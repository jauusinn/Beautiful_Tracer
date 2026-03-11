package com.beautifultracer.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to detect root access on the device and provide a
 * toggle for raw ICMP socket mode.
 */
object RootChecker {

    /**
     * Check if the device has root (su binary) available.
     * This does NOT request root permission — just checks availability.
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check common su binary locations
            val paths = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )

            for (path in paths) {
                if (java.io.File(path).exists()) {
                    return@withContext true
                }
            }

            // Alternative: try running su
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            return@withContext exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Request root access by invoking su.
     * Returns true if root was granted.
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream
            outputStream.write("exit\n".toByteArray())
            outputStream.flush()
            outputStream.close()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }
}
