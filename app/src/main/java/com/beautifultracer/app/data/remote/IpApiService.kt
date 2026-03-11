package com.beautifultracer.app.data.remote

import com.beautifultracer.app.data.remote.dto.IpApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface IpApiService {

    @GET("json/{ip}")
    suspend fun getIpInfo(@Path("ip") ip: String): IpApiResponse

    companion object {
        // Reverting to HTTP for free tier compatibility. 
        // Note: android:usesCleartextTraffic="true" must be set in AndroidManifest.xml
        const val BASE_URL = "http://ip-api.com/"
    }
}
