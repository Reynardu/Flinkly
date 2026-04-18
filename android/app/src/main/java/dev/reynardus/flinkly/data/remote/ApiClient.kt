package dev.reynardus.flinkly.data.remote

import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(private val preferencesStore: PreferencesStore) {

    private var _service: ApiService? = null
    private var _currentBaseUrl: String? = null

    val service: ApiService
        get() = _service ?: buildService(runBlocking { preferencesStore.serverUrl.first() } ?: "http://localhost:8000")

    fun rebuildWithUrl(url: String): ApiService {
        _service = buildService(url)
        _currentBaseUrl = url
        return _service!!
    }

    private fun buildService(baseUrl: String): ApiService {
        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { preferencesStore.authToken.first() }
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
