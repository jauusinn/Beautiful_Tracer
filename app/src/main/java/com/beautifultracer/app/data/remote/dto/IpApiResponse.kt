package com.beautifultracer.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IpApiResponse(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "countryCode") val countryCode: String? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "regionName") val regionName: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "zip") val zip: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "isp") val isp: String? = null,
    @Json(name = "org") val org: String? = null,
    @Json(name = "as") val asInfo: String? = null,
    @Json(name = "query") val query: String? = null
) {
    val isSuccess: Boolean get() = status == "success"
}
