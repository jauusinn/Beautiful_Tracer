package com.beautifultracer.app.network

/**
 * Lightweight offline IP-to-Country resolver.
 * Uses Unicode regional indicator symbols to generate flag emojis.
 *
 * For the initial version, country codes are obtained from the ip-api.com
 * response. This class provides the flag emoji conversion utility.
 *
 * A future enhancement would load a compressed offline IP→Country CSV
 * from assets for instant, offline lookups.
 */
object GeoIpResolver {

    /**
     * Convert a 2-letter ISO country code (e.g., "US", "DE", "JP")
     * to a flag emoji (e.g., 🇺🇸, 🇩🇪, 🇯🇵).
     *
     * Uses Unicode Regional Indicator Symbols:
     * Each letter is offset by 0x1F1E6 - 'A' to map to the regional indicator range.
     */
    fun countryCodeToFlag(countryCode: String?): String {
        if (countryCode.isNullOrBlank() || countryCode.length != 2) return "🌐"

        val code = countryCode.uppercase()
        return try {
            val firstChar = Character.toChars(0x1F1E6 + (code[0] - 'A'))
            val secondChar = Character.toChars(0x1F1E6 + (code[1] - 'A'))
            String(firstChar) + String(secondChar)
        } catch (_: Exception) {
            "🌐"
        }
    }

    /**
     * Check if the given string is a valid IP address (basic check).
     */
    fun isValidIp(ip: String): Boolean {
        return ip.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    }

    /**
     * Check if the IP is a private/reserved address.
     * These IPs won't have geolocation data.
     */
    fun isPrivateIp(ip: String): Boolean {
        if (!isValidIp(ip)) return false
        val parts = ip.split(".").map { it.toIntOrNull() ?: return false }

        return when {
            // 10.0.0.0/8
            parts[0] == 10 -> true
            // 172.16.0.0/12
            parts[0] == 172 && parts[1] in 16..31 -> true
            // 192.168.0.0/16
            parts[0] == 192 && parts[1] == 168 -> true
            // 127.0.0.0/8 (loopback)
            parts[0] == 127 -> true
            // 169.254.0.0/16 (link-local)
            parts[0] == 169 && parts[1] == 254 -> true
            else -> false
        }
    }
}
