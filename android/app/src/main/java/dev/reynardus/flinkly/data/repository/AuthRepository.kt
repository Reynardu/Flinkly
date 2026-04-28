package dev.reynardus.flinkly.data.repository

import dev.reynardus.flinkly.data.remote.ApiClient
import dev.reynardus.flinkly.data.remote.dto.LoginRequest
import dev.reynardus.flinkly.data.remote.dto.RegisterRequest
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val prefs: PreferencesStore,
) {
    suspend fun checkServerHealth(url: String): Result<Unit> = runCatching {
        val service = apiClient.rebuildWithUrl(url)
        val response = service.health()
        if (!response.isSuccessful) error("Server nicht erreichbar (${response.code()})")
        prefs.saveServerUrl(url)
    }

    suspend fun register(displayName: String, password: String): Result<Unit> = runCatching {
        val response = apiClient.service.register(RegisterRequest(displayName, password))
        if (!response.isSuccessful) error(response.errorBody()?.string() ?: "Registrierung fehlgeschlagen")
        val body = response.body()!!
        prefs.saveAuthToken(body.token)
        prefs.saveUserSecret(body.userSecret)
        prefs.saveUserId(body.user.id)
        prefs.saveUserName(body.user.displayName)
    }

    suspend fun login(userSecret: String): Result<Unit> = runCatching {
        val response = apiClient.service.login(LoginRequest(userSecret))
        if (!response.isSuccessful) error("Unbekannter Nutzer oder falscher Code")
        val body = response.body()!!
        prefs.saveAuthToken(body.token)
        prefs.saveUserSecret(body.userSecret)
        prefs.saveUserId(body.user.id)
        prefs.saveUserName(body.user.displayName)
    }

    suspend fun isLoggedIn(): Boolean {
        val token = prefs.authToken.first()
        val url = prefs.serverUrl.first()
        return token != null && url != null
    }

    suspend fun hasHousehold(): Boolean = prefs.householdId.first() != null

    suspend fun logout() = prefs.clearSession()
}
