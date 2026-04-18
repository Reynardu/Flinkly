package dev.reynardus.flinkly.ui.screens.rooms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.local.entities.RoomEntity
import dev.reynardus.flinkly.data.repository.RoomRepository
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val prefs: PreferencesStore,
) : ViewModel() {

    var showCreateDialog by mutableStateOf(false)
        private set
    var newRoomName by mutableStateOf("")
        private set
    var newRoomIcon by mutableStateOf("🏠")
        private set
    var newRoomColor by mutableStateOf("#4CAF50")
        private set
    var isCreating by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var householdId: Int? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val rooms: StateFlow<List<RoomEntity>> = prefs.householdId
        .filterNotNull()
        .flatMapLatest { id ->
            householdId = id
            roomRepository.getRooms(id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            val id = prefs.householdId.first() ?: return@launch
            householdId = id
            roomRepository.syncRooms(id)
        }
    }

    fun openCreateDialog() {
        newRoomName = ""
        newRoomIcon = "🏠"
        newRoomColor = "#4CAF50"
        error = null
        showCreateDialog = true
    }

    fun dismissCreateDialog() { showCreateDialog = false }
    fun onNameChange(v: String) { newRoomName = v }
    fun onIconChange(v: String) { newRoomIcon = v }
    fun onColorChange(v: String) { newRoomColor = v }

    fun createRoom() {
        val id = householdId ?: return
        viewModelScope.launch {
            isCreating = true
            error = null
            roomRepository.createRoom(id, newRoomName.trim(), newRoomIcon, newRoomColor)
                .onSuccess { showCreateDialog = false }
                .onFailure { error = it.message }
            isCreating = false
        }
    }

    fun deleteRoom(roomId: Int) {
        viewModelScope.launch {
            roomRepository.deleteRoom(roomId)
            householdId?.let { roomRepository.syncRooms(it) }
        }
    }
}
