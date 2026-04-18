package dev.reynardus.flinkly.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.HouseholdDto
import dev.reynardus.flinkly.data.remote.dto.HouseholdPauseDto
import dev.reynardus.flinkly.data.remote.dto.UserDto
import dev.reynardus.flinkly.data.repository.AuthRepository
import dev.reynardus.flinkly.data.repository.HouseholdRepository
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val api: ApiService,
    private val prefs: PreferencesStore,
) : ViewModel() {

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user

    private val _household = MutableStateFlow<HouseholdDto?>(null)
    val household: StateFlow<HouseholdDto?> = _household

    private val _pauses = MutableStateFlow<List<HouseholdPauseDto>>(emptyList())
    val pauses: StateFlow<List<HouseholdPauseDto>> = _pauses

    var inviteLink by mutableStateOf<String?>(null)
        private set
    var isGeneratingLink by mutableStateOf(false)
        private set
    var userSecret by mutableStateOf<String?>(null)
        private set
    var showSecret by mutableStateOf(false)
        private set

    // Pause dialog state
    var showPauseDialog by mutableStateOf(false)
        private set
    var pauseStartDate by mutableStateOf("")
        private set
    var pauseEndDate by mutableStateOf("")
        private set
    var pauseReason by mutableStateOf("")
        private set
    var isSavingPause by mutableStateOf(false)
        private set
    var pauseError by mutableStateOf<String?>(null)
        private set

    private var householdId: Int? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            runCatching { api.getMe().body() }.getOrNull()?.let { _user.value = it }
            val id = prefs.householdId.first() ?: return@launch
            householdId = id
            runCatching { householdRepository.fetchHousehold(id).getOrNull() }
                .getOrNull()?.let { _household.value = it }
            userSecret = prefs.userSecret.first()
            loadPauses(id)
        }
    }

    private suspend fun loadPauses(id: Int) {
        householdRepository.getPauses(id).getOrNull()?.let { _pauses.value = it }
    }

    fun generateInviteLink() {
        viewModelScope.launch {
            isGeneratingLink = true
            val id = prefs.householdId.first() ?: return@launch
            householdRepository.createInviteLink(id).onSuccess { inviteLink = it }
            isGeneratingLink = false
        }
    }

    fun toggleSecretVisibility() { showSecret = !showSecret }

    fun openPauseDialog() {
        pauseStartDate = ""
        pauseEndDate = ""
        pauseReason = ""
        pauseError = null
        showPauseDialog = true
    }

    fun dismissPauseDialog() { showPauseDialog = false }
    fun onStartDateChange(v: String) { pauseStartDate = v; pauseError = null }
    fun onEndDateChange(v: String) { pauseEndDate = v; pauseError = null }
    fun onReasonChange(v: String) { pauseReason = v }

    fun savePause() {
        val id = householdId ?: return
        viewModelScope.launch {
            isSavingPause = true
            pauseError = null
            householdRepository.createPause(
                householdId = id,
                startDate = pauseStartDate.trim(),
                endDate = pauseEndDate.trim(),
                reason = pauseReason.trim().ifBlank { null },
            )
                .onSuccess {
                    showPauseDialog = false
                    loadPauses(id)
                }
                .onFailure { pauseError = it.message }
            isSavingPause = false
        }
    }

    fun deletePause(pauseId: Int) {
        val id = householdId ?: return
        viewModelScope.launch {
            householdRepository.deletePause(id, pauseId)
                .onSuccess { loadPauses(id) }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
