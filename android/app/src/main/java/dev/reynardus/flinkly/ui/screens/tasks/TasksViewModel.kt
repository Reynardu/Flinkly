package dev.reynardus.flinkly.ui.screens.tasks

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.reynardus.flinkly.data.local.entities.TaskEntity
import dev.reynardus.flinkly.data.remote.dto.CompletionDto
import dev.reynardus.flinkly.data.remote.dto.TaskCreate
import dev.reynardus.flinkly.data.repository.RoomRepository
import dev.reynardus.flinkly.data.repository.TaskRepository
import dev.reynardus.flinkly.data.store.PreferencesStore
import dev.reynardus.flinkly.widget.WidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val roomRepository: RoomRepository,
    private val prefs: PreferencesStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val roomIdFlow = MutableStateFlow(0)

    var showCreateDialog by mutableStateOf(false)
        private set
    var newTitle by mutableStateOf("")
        private set
    var newDescription by mutableStateOf("")
        private set
    var newDifficulty by mutableIntStateOf(2)
        private set
    var newFrequencyType by mutableStateOf("WEEKLY")
        private set
    var newFrequencyValue by mutableStateOf<String?>(null)
        private set
    var newAutoRepeat by mutableStateOf(true)
        private set
    var isCreating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var editingTaskId by mutableStateOf<Int?>(null)
        private set
    var isSaving by mutableStateOf(false)
        private set

    var pendingEarlyCompleteTaskId by mutableStateOf<Int?>(null)
        private set
    var pendingEarlyCompleteDueDate by mutableStateOf<String?>(null)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    private val _completions = MutableStateFlow<Map<Int, List<CompletionDto>>>(emptyMap())
    val completions: StateFlow<Map<Int, List<CompletionDto>>> = _completions.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = roomIdFlow
        .flatMapLatest { id -> taskRepository.getTasks(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun init(roomId: Int) {
        roomIdFlow.value = roomId
        viewModelScope.launch { syncTasks(roomId) }
    }

    fun refresh() {
        val roomId = roomIdFlow.value.takeIf { it != 0 } ?: return
        viewModelScope.launch {
            isRefreshing = true
            syncTasks(roomId)
            isRefreshing = false
        }
    }

    // Nur Tasks laden – kein syncRooms hier, da deleteByHousehold Tasks via CASCADE löscht
    private suspend fun syncTasks(roomId: Int) {
        taskRepository.syncTasks(roomId).onSuccess { dtos ->
            _completions.value = dtos.associate { it.id to it.completions }
            val openTasks = dtos.filter { it.completions.none { c -> isToday(c.completedAt) } }
            val todayPoints = dtos.sumOf { task ->
                task.completions.count { isToday(it.completedAt) } * task.difficulty
            }
            WidgetUpdater.update(
                context = context,
                openCount = openTasks.size,
                titles = openTasks.map { it.title },
                todayPoints = todayPoints,
            )
        }
    }

    private fun isToday(isoDate: String?): Boolean = try {
        val date = java.time.OffsetDateTime.parse(isoDate).toLocalDate()
        date == java.time.LocalDate.now()
    } catch (_: Exception) {
        false
    }

    // Nach Mutationen: Rooms neu laden (cascade löscht Tasks), dann Tasks wiederherstellen
    private suspend fun syncAfterMutation(roomId: Int) {
        prefs.householdId.first()?.let { roomRepository.syncRooms(it) }
        syncTasks(roomId)
    }

    fun openCreateDialog() {
        newTitle = ""
        newDescription = ""
        newDifficulty = 2
        newFrequencyType = "WEEKLY"
        newFrequencyValue = null
        newAutoRepeat = true
        error = null
        showCreateDialog = true
    }

    fun openEditDialog(task: TaskEntity) {
        editingTaskId = task.id
        newTitle = task.title
        newDescription = task.description ?: ""
        newDifficulty = task.difficulty
        newFrequencyType = task.frequencyType
        newFrequencyValue = task.frequencyValue
        newAutoRepeat = task.autoRepeat
        error = null
    }

    fun dismissEditDialog() { editingTaskId = null }

    fun saveTask() {
        val taskId = editingTaskId ?: return
        val roomId = roomIdFlow.value
        viewModelScope.launch {
            isSaving = true
            error = null
            taskRepository.updateTask(
                taskId, TaskCreate(
                    title = newTitle.trim(),
                    description = newDescription.trim().ifBlank { null },
                    difficulty = newDifficulty,
                    frequencyType = newFrequencyType,
                    frequencyValue = newFrequencyValue,
                    autoRepeat = newAutoRepeat,
                )
            )
                .onSuccess { editingTaskId = null; syncAfterMutation(roomId) }
                .onFailure { error = it.message }
            isSaving = false
        }
    }

    fun dismissCreateDialog() { showCreateDialog = false }
    fun onTitleChange(v: String) { newTitle = v }
    fun onDescriptionChange(v: String) { newDescription = v }
    fun onDifficultyChange(v: Int) { newDifficulty = v }
    fun onFrequencyChange(v: String) { newFrequencyType = v; if (v != "WEEKLY") newFrequencyValue = null }
    fun onFrequencyValueChange(v: String?) { newFrequencyValue = v }
    fun onAutoRepeatChange(v: Boolean) { newAutoRepeat = v }

    fun createTask() {
        val roomId = roomIdFlow.value
        viewModelScope.launch {
            isCreating = true
            error = null
            taskRepository.createTask(
                roomId, TaskCreate(
                    title = newTitle.trim(),
                    description = newDescription.trim().ifBlank { null },
                    difficulty = newDifficulty,
                    frequencyType = newFrequencyType,
                    frequencyValue = newFrequencyValue,
                    autoRepeat = newAutoRepeat,
                )
            )
                .onSuccess {
                    showCreateDialog = false
                    syncAfterMutation(roomId)
                }
                .onFailure { error = it.message }
            isCreating = false
        }
    }

    fun completeTask(taskId: Int) {
        val task = tasks.value.find { it.id == taskId } ?: return
        val nextDueAt = task.nextDueAt
        if (nextDueAt != null && isInFuture(nextDueAt)) {
            pendingEarlyCompleteTaskId = taskId
            pendingEarlyCompleteDueDate = nextDueAt.take(10)
            return
        }
        doCompleteTask(taskId)
    }

    fun confirmEarlyCompletion() {
        val taskId = pendingEarlyCompleteTaskId ?: return
        pendingEarlyCompleteTaskId = null
        pendingEarlyCompleteDueDate = null
        doCompleteTask(taskId)
    }

    fun dismissEarlyCompletion() {
        pendingEarlyCompleteTaskId = null
        pendingEarlyCompleteDueDate = null
    }

    private fun doCompleteTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
            syncAfterMutation(roomIdFlow.value)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            syncAfterMutation(roomIdFlow.value)
        }
    }

    private fun isInFuture(isoDate: String): Boolean = try {
        java.time.OffsetDateTime.parse(isoDate).toInstant().isAfter(java.time.Instant.now())
    } catch (_: Exception) {
        false
    }
}
