package com.example.fantasyfootballapp.network

import android.content.Context
import com.example.fantasyfootballapp.data.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "http://10.0.2.2:8080/" // emulator → PC

    private lateinit var tokenStore: TokenStore

    // Call this once from Application or your first Activity
    fun init(context: Context) {
        tokenStore = TokenStore(context.applicationContext)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenStore.getToken()

        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        chain.proceed(request)
    }

    //For 401 exceptions
    private val unauthorizedInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            //Token is invalid/expired/etc.
            tokenStore.clearToken()
        }

        response
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)          // add token to request
            .addInterceptor(unauthorizedInterceptor)  // react to 401 responses
            .addInterceptor(logging)                  // keep logging
            .build()
    }

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}