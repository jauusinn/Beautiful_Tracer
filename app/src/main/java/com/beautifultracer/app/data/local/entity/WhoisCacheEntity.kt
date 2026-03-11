package com.beautifultracer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whois_cache")
data class WhoisCacheEntity(
    @PrimaryKey
    val ipAddress: String,
    val isp: String? = null,
    val org: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val asn: String? = null,
    val asName: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val fetchedAt: Long = System.currentTimeMillis()
)
