package dev.reynardus.flinkly.ui.screens.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.local.entities.TaskEntity
import dev.reynardus.flinkly.data.remote.dto.TaskCreate
import dev.reynardus.flinkly.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
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
    var newAutoRepeat by mutableStateOf(true)
        private set
    var isCreating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = roomIdFlow
        .flatMapLatest { id -> taskRepository.getTasks(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun init(roomId: Int) {
        if (roomIdFlow.value == roomId) return
        roomIdFlow.value = roomId
        viewModelScope.launch {
            taskRepository.syncTasks(roomId)
        }
    }

    fun openCreateDialog() {
        newTitle = ""
        newDescription = ""
        newDifficulty = 2
        newFrequencyType = "WEEKLY"
        newAutoRepeat = true
        error = null
        showCreateDialog = true
    }

    fun dismissCreateDialog() { showCreateDialog = false }
    fun onTitleChange(v: String) { newTitle = v }
    fun onDescriptionChange(v: String) { newDescription = v }
    fun onDifficultyChange(v: Int) { newDifficulty = v }
    fun onFrequencyChange(v: String) { newFrequencyType = v }
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
                    autoRepeat = newAutoRepeat,
                )
            )
                .onSuccess { showCreateDialog = false }
                .onFailure { error = it.message }
            isCreating = false
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
            taskRepository.syncTasks(roomIdFlow.value)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }
}
