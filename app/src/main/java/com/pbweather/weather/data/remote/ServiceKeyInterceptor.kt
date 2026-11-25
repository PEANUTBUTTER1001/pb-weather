package com.pbweather.weather.data.remote

import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import okhttp3.Interceptor
import okhttp3.Response

class KeyConfigurationException : IOException("KEY_CONFIGURATION")
class ServiceKeyInterceptor(
    private val kmaKey: String,
    private val airKey: String,
    private val expectedHost: String = "apis.data.go.kr",
    private val requireHttps: Boolean = true
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != expectedHost || (requireHttps && !request.url.isHttps)) throw IOException("UNTRUSTED_ENDPOINT")
        val configured = if (request.url.encodedPath.startsWith("/B552584/")) airKey else kmaKey
        val key = if (Regex("%[0-9a-fA-F]{2}").containsMatchIn(configured)) {
            URLDecoder.decode(configured, StandardCharsets.UTF_8.name())
        } else {
            configured
        }
        if (key.isBlank() || key.startsWith("<")) throw KeyConfigurationException()
        val url = request.url.newBuilder().removeAllQueryParameters("serviceKey").removeAllQueryParameters("ServiceKey")
            .addQueryParameter("serviceKey", key).build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
