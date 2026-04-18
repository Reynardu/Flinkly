package dev.reynardus.flinkly.data.repository

import dev.reynardus.flinkly.data.local.dao.TaskDao
import dev.reynardus.flinkly.data.local.entities.TaskEntity
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.CompletionCreate
import dev.reynardus.flinkly.data.remote.dto.CompletionDto
import dev.reynardus.flinkly.data.remote.dto.TaskCreate
import dev.reynardus.flinkly.data.remote.dto.TaskDto
import dev.reynardus.flinkly.data.remote.dto.TaskSuggestionDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: ApiService,
    private val dao: TaskDao,
) {
    fun getTasks(roomId: Int): Flow<List<TaskEntity>> = dao.getTasks(roomId)

    suspend fun syncTasks(roomId: Int): Result<List<TaskDto>> = runCatching {
        val response = api.getTasks(roomId)
        if (!response.isSuccessful) error("Aufgaben konnten nicht geladen werden")
        val dtos = response.body()!!
        dao.deleteByRoom(roomId)
        dao.upsertAll(dtos.map { it.toEntity() })
        dtos
    }

    suspend fun createTask(roomId: Int, task: TaskCreate): Result<TaskDto> = runCatching {
        val response = api.createTask(roomId, task)
        if (!response.isSuccessful) error("Aufgabe konnte nicht erstellt werden")
        val dto = response.body()!!
        dao.upsertAll(listOf(dto.toEntity()))
        dto
    }

    suspend fun completeTask(taskId: Int, photoUrl: String? = null, note: String? = null): Result<CompletionDto> = runCatching {
        val response = api.completeTask(taskId, CompletionCreate(photoUrl, note))
        if (!response.isSuccessful) error("Aufgabe konnte nicht erledigt werden")
        response.body()!!
    }

    suspend fun deleteTask(taskId: Int): Result<Unit> = runCatching {
        val response = api.deleteTask(taskId)
        if (!response.isSuccessful) error("Aufgabe konnte nicht gelöscht werden")
        dao.deleteById(taskId)
    }

    suspend fun getSuggestions(householdId: Int): List<TaskSuggestionDto> =
        api.getTaskSuggestions(householdId).body() ?: emptyList()

    private fun TaskDto.toEntity() = TaskEntity(
        id = id, roomId = roomId, title = title, description = description,
        difficulty = difficulty, frequencyType = frequencyType, frequencyValue = frequencyValue,
        dueDate = dueDate, autoRepeat = autoRepeat, points = points, photoUrl = photoUrl,
        isSuggestion = isSuggestion, assignedToUserId = assignedToUserId,
        nextDueAt = nextDueAt, completionCount = completionCount, createdAt = createdAt,
    )
}
