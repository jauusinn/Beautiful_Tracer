package com.beautifultracer.app.domain.model

data class WhoisInfo(
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
    val isCached: Boolean = false,
    val fetchedAt: Long = 0
)
