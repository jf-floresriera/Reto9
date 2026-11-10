package com.example.reto9.data

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit para la Overpass API de OpenStreetMap.
 *
 * El servidor público principal (overpass-api.de) se satura y a veces rechaza las
 * peticiones ("Failed to connect"). Por eso se ofrecen VARIOS servidores (mirrors)
 * y timeouts amplios. Si uno falla, MainActivity reintenta con el siguiente.
 */
object RetrofitClient {

    // Mirrors de Overpass ordenados por fiabilidad. Si el primero falla, se usa el siguiente.
    val servers = listOf(
        "https://overpass.kumi.systems/",
        "https://overpass-api.de/",
        "https://maps.mail.ru/osm/tools/overpass/"
    )

    // Cliente HTTP con timeouts amplios (Overpass puede tardar) y User-Agent válido.
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Reto9-Android/1.0 (academic project)")
                .build()
            chain.proceed(request)
        })
        .build()

    /** Crea una instancia de la API apuntando al servidor indicado. */
    fun apiFor(baseUrl: String): OverpassApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)
    }

    /** Instancia por defecto (primer servidor). */
    val instance: OverpassApi by lazy { apiFor(servers.first()) }
}
