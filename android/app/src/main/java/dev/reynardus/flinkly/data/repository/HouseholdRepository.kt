package dev.reynardus.flinkly.data.repository

import dev.reynardus.flinkly.data.local.dao.HouseholdDao
import dev.reynardus.flinkly.data.local.entities.HouseholdEntity
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.HouseholdCreate
import dev.reynardus.flinkly.data.remote.dto.HouseholdDto
import dev.reynardus.flinkly.data.remote.dto.HouseholdPauseDto
import dev.reynardus.flinkly.data.remote.dto.PauseCreate
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepository @Inject constructor(
    private val api: ApiService,
    private val dao: HouseholdDao,
    private val prefs: PreferencesStore,
) {
    fun getHousehold(id: Int): Flow<HouseholdEntity?> = dao.getHousehold(id)

    suspend fun createHousehold(name: String): Result<HouseholdDto> = runCatching {
        val response = api.createHousehold(HouseholdCreate(name))
        if (!response.isSuccessful) error("Haushalt konnte nicht erstellt werden")
        val dto = response.body()!!
        dao.upsert(dto.toEntity())
        prefs.saveHouseholdId(dto.id)
        dto
    }

    suspend fun fetchHousehold(id: Int): Result<HouseholdDto> = runCatching {
        val response = api.getHousehold(id)
        if (!response.isSuccessful) error("Haushalt nicht gefunden")
        val dto = response.body()!!
        dao.upsert(dto.toEntity())
        dto
    }

    suspend fun createInviteLink(householdId: Int): Result<String> = runCatching {
        val response = api.createInviteLink(householdId)
        if (!response.isSuccessful) error("Einladungslink konnte nicht erstellt werden")
        response.body()!!.inviteUrl
    }

    suspend fun joinHousehold(token: String): Result<HouseholdDto> = runCatching {
        val response = api.joinHousehold(token)
        if (!response.isSuccessful) error("Beitritt fehlgeschlagen — Link ungültig?")
        val dto = response.body()!!
        dao.upsert(dto.toEntity())
        prefs.saveHouseholdId(dto.id)
        dto
    }

    suspend fun getPauses(householdId: Int): Result<List<HouseholdPauseDto>> = runCatching {
        val response = api.getPauses(householdId)
        if (!response.isSuccessful) error("Pausen konnten nicht geladen werden")
        response.body()!!
    }

    suspend fun getActivePause(householdId: Int): HouseholdPauseDto? =
        runCatching { api.getActivePause(householdId).body() }.getOrNull()

    suspend fun createPause(householdId: Int, startDate: String, endDate: String, reason: String?): Result<HouseholdPauseDto> = runCatching {
        val response = api.createPause(householdId, PauseCreate(startDate, endDate, reason))
        if (!response.isSuccessful) error("Pause konnte nicht erstellt werden")
        response.body()!!
    }

    suspend fun deletePause(householdId: Int, pauseId: Int): Result<Unit> = runCatching {
        val response = api.deletePause(householdId, pauseId)
        if (!response.isSuccessful) error("Pause konnte nicht gelöscht werden")
    }

    private fun HouseholdDto.toEntity() = HouseholdEntity(id = id, name = name, createdAt = createdAt)
}
