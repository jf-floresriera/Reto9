package com.example.reto9.data

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("api/interpreter")
    fun getPoi(
        @Query("data") query: String
    ): Call<OverpassResponse>
}
