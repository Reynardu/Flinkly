package dev.reynardus.flinkly.data.repository

import dev.reynardus.flinkly.data.local.dao.RoomDao
import dev.reynardus.flinkly.data.local.entities.RoomEntity
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.RoomCreate
import dev.reynardus.flinkly.data.remote.dto.RoomDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val api: ApiService,
    private val dao: RoomDao,
) {
    fun getRooms(householdId: Int): Flow<List<RoomEntity>> = dao.getRooms(householdId)

    suspend fun syncRooms(householdId: Int): Result<List<RoomDto>> = runCatching {
        val response = api.getRooms(householdId)
        if (!response.isSuccessful) error("Räume konnten nicht geladen werden")
        val dtos = response.body()!!
        dao.deleteByHousehold(householdId)
        dao.upsertAll(dtos.map { it.toEntity() })
        dtos
    }

    suspend fun createRoom(householdId: Int, name: String, icon: String, color: String): Result<RoomDto> = runCatching {
        val response = api.createRoom(householdId, RoomCreate(name, icon, color))
        if (!response.isSuccessful) error("Raum konnte nicht erstellt werden")
        val dto = response.body()!!
        dao.upsertAll(listOf(dto.toEntity()))
        dto
    }

    suspend fun deleteRoom(roomId: Int): Result<Unit> = runCatching {
        val response = api.deleteRoom(roomId)
        if (!response.isSuccessful) error("Raum konnte nicht gelöscht werden")
    }

    suspend fun getRoomSuggestions(): List<Map<String, String>> =
        api.getRoomSuggestions().body() ?: emptyList()

    private fun RoomDto.toEntity() = RoomEntity(
        id = id, householdId = householdId, name = name,
        icon = icon, color = color, taskCount = taskCount,
        openTaskCount = openTaskCount, createdAt = createdAt,
    )
}
