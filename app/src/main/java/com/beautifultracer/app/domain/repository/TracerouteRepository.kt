package com.beautifultracer.app.domain.repository

import com.beautifultracer.app.data.local.dao.WhoisCacheDao
import com.beautifultracer.app.data.local.entity.WhoisCacheEntity
import com.beautifultracer.app.data.remote.IpApiService
import com.beautifultracer.app.domain.model.WhoisInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TracerouteRepository — the "TracerouteManager" that coordinates:
 *   1. Local Room DB cache check for WHOIS data
 *   2. Online ip-api.com fallback if cache miss or expired
 *   3. Automatic persistence of fetched data for future offline use
 */
class TracerouteRepository(
    private val whoisCacheDao: WhoisCacheDao,
    private val ipApiService: IpApiService
) {
    companion object {
        /** Cache TTL: 7 days in milliseconds */
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Get WHOIS/IP info for a given IP address.
     * Strategy: Local DB first → Online fallback → Persist to DB.
     *
     * @param ipAddress The IP to look up
     * @return [WhoisInfo] with IP ownership details, or null on failure
     */
    suspend fun getWhoisInfo(ipAddress: String): Result<WhoisInfo> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Check local Room DB cache
            val cached = whoisCacheDao.getByIp(ipAddress)
            if (cached != null && !isCacheExpired(cached.fetchedAt)) {
                return@withContext Result.success(cached.toDomainModel(isCached = true))
            }

            // Step 2: Cache miss or expired — fetch from ip-api.com
            val response = ipApiService.getIpInfo(ipAddress)
            if (!response.isSuccess) {
                // If the API returned a failure but we have stale cache, return it
                if (cached != null) {
                    return@withContext Result.success(cached.toDomainModel(isCached = true))
                }
                return@withContext Result.failure(
                    Exception("IP lookup failed: ${response.message ?: "Unknown error"}")
                )
            }

            // Step 3: Map response → entity and persist to Room DB
            val entity = WhoisCacheEntity(
                ipAddress = ipAddress,
                isp = response.isp,
                org = response.org,
                city = response.city,
                region = response.regionName,
                country = response.country,
                countryCode = response.countryCode,
                asn = extractAsn(response.asInfo),
                asName = extractAsName(response.asInfo),
                lat = response.lat,
                lon = response.lon,
                fetchedAt = System.currentTimeMillis()
            )
            whoisCacheDao.insert(entity)

            Result.success(entity.toDomainModel(isCached = false))
        } catch (e: Exception) {
            // Network error — try returning stale cache if available
            val staleCache = try {
                whoisCacheDao.getByIp(ipAddress)
            } catch (_: Exception) {
                null
            }
            if (staleCache != null) {
                Result.success(staleCache.toDomainModel(isCached = true))
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Clean up expired cache entries.
     */
    suspend fun cleanExpiredCache() = withContext(Dispatchers.IO) {
        val expiryTimestamp = System.currentTimeMillis() - CACHE_TTL_MS
        whoisCacheDao.deleteExpired(expiryTimestamp)
    }

    /**
     * Get total number of cached entries.
     */
    suspend fun getCacheSize(): Int = withContext(Dispatchers.IO) {
        whoisCacheDao.getCacheSize()
    }

    private fun isCacheExpired(fetchedAt: Long): Boolean {
        return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS
    }

    /**
     * Extract ASN number from ip-api's "as" field.
     * Format is typically: "AS15169 Google LLC"
     */
    private fun extractAsn(asInfo: String?): String? {
        if (asInfo.isNullOrBlank()) return null
        return asInfo.split(" ").firstOrNull()
    }

    /**
     * Extract AS organization name from ip-api's "as" field.
     */
    private fun extractAsName(asInfo: String?): String? {
        if (asInfo.isNullOrBlank()) return null
        val parts = asInfo.split(" ", limit = 2)
        return if (parts.size > 1) parts[1] else null
    }

    /**
     * Extension to convert a [WhoisCacheEntity] to the UI-facing [WhoisInfo].
     */
    private fun WhoisCacheEntity.toDomainModel(isCached: Boolean): WhoisInfo {
        return WhoisInfo(
            ipAddress = this.ipAddress,
            isp = this.isp,
            org = this.org,
            city = this.city,
            region = this.region,
            country = this.country,
            countryCode = this.countryCode,
            asn = this.asn,
            asName = this.asName,
            lat = this.lat,
            lon = this.lon,
            isCached = isCached,
            fetchedAt = this.fetchedAt
        )
    }
}
