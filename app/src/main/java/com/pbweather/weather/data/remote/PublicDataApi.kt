package com.pbweather.weather.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface PublicDataApi {
    @GET
    suspend fun get(@Url operation: String, @QueryMap parameters: Map<String, String>): Response<ResponseBody>
}
