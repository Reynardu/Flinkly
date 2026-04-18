package dev.reynardus.flinkly.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flinkly_prefs")

@Singleton
class PreferencesStore @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_SECRET = stringPreferencesKey("user_secret")
        val USER_ID = intPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val HOUSEHOLD_ID = intPreferencesKey("household_id")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val userSecret: Flow<String?> = context.dataStore.data.map { it[USER_SECRET] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val householdId: Flow<Int?> = context.dataStore.data.map { it[HOUSEHOLD_ID] }

    suspend fun saveServerUrl(url: String) = context.dataStore.edit { it[SERVER_URL] = url.trimEnd('/') }
    suspend fun saveAuthToken(token: String) = context.dataStore.edit { it[AUTH_TOKEN] = token }
    suspend fun saveUserSecret(secret: String) = context.dataStore.edit { it[USER_SECRET] = secret }
    suspend fun saveUserId(id: Int) = context.dataStore.edit { it[USER_ID] = id }
    suspend fun saveUserName(name: String) = context.dataStore.edit { it[USER_NAME] = name }
    suspend fun saveHouseholdId(id: Int) = context.dataStore.edit { it[HOUSEHOLD_ID] = id }

    suspend fun clearSession() = context.dataStore.edit { prefs ->
        prefs.remove(AUTH_TOKEN)
        prefs.remove(USER_SECRET)
        prefs.remove(USER_ID)
        prefs.remove(USER_NAME)
        prefs.remove(HOUSEHOLD_ID)
    }
}
