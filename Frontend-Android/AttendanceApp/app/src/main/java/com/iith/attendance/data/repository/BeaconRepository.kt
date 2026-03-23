package com.iith.attendance.data.repository

import com.iith.attendance.data.models.MinorResponse
import com.iith.attendance.data.models.ValidateResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ── Retrofit interface ────────────────────────────────────────────────────────

interface BeaconApiService {

    /** GET /getMinor?major=100  →  { status: "success", minor: 12345 }
     *  The Flask server actually returns just the int as a plain string;
     *  this interface works if the backend is updated to return JSON.
     *  Fallback handled in repository.
     */
    @GET("getMinor")
    suspend fun getMinor(@Query("major") major: Int): retrofit2.Response<String>

    @GET("validate")
    suspend fun validate(
        @Query("major") major: Int,
        @Query("minor") minor: Int
    ): ValidateResponse
}

// ── Singleton factory ─────────────────────────────────────────────────────────

object ApiClient {

    // Replace with your actual server IP / URL
    private const val BASE_URL = "http://10.8.75.55:4040/"
    
    val service: BeaconApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BeaconApiService::class.java)
    }
}

// ── Repository ────────────────────────────────────────────────────────────────

class BeaconRepository {

    /** Returns the minor value for a given major, or null on failure. */
    suspend fun fetchMinor(major: Int): Int? = runCatching {
        val response = ApiClient.service.getMinor(major)
        if (response.isSuccessful) response.body()?.trim()?.toIntOrNull() else null
    }.getOrNull()

    suspend fun validateMinor(major: Int, minor: Int): Boolean = runCatching {
        ApiClient.service.validate(major, minor).valid
    }.getOrDefault(false)
}
