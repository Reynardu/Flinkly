package dev.reynardus.flinkly.ui.screens.scoreboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.ScoreboardDto
import dev.reynardus.flinkly.data.remote.dto.UserCompletionDto
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScoreboardViewModel @Inject constructor(
    private val api: ApiService,
    private val prefs: PreferencesStore,
) : ViewModel() {

    var selectedPeriod by mutableStateOf("weekly")
        private set

    private val _scoreboard = MutableStateFlow<ScoreboardDto?>(null)
    val scoreboard: StateFlow<ScoreboardDto?> = _scoreboard

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId

    // User-Detail-Dialog
    var selectedUserName by mutableStateOf<String?>(null)
        private set
    private val _selectedUserCompletions = MutableStateFlow<List<UserCompletionDto>>(emptyList())
    val selectedUserCompletions: StateFlow<List<UserCompletionDto>> = _selectedUserCompletions
    var isLoadingUserDetail by mutableStateOf(false)
        private set

    init { load() }

    fun selectPeriod(period: String) {
        if (selectedPeriod == period) return
        selectedPeriod = period
        load()
    }

    fun refresh() = load()

    fun openUserDetail(userId: Int, userName: String) {
        selectedUserName = userName
        _selectedUserCompletions.value = emptyList()
        viewModelScope.launch {
            isLoadingUserDetail = true
            val householdId = prefs.householdId.first() ?: return@launch
            runCatching { api.getUserCompletions(householdId, userId).body() }.getOrNull()
                ?.let { _selectedUserCompletions.value = it }
            isLoadingUserDetail = false
        }
    }

    fun closeUserDetail() {
        selectedUserName = null
        _selectedUserCompletions.value = emptyList()
    }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val householdId = prefs.householdId.first() ?: return@launch
            _currentUserId.value = prefs.userId.first()
            runCatching { api.getScoreboard(householdId, selectedPeriod).body() }.getOrNull()
                ?.let { _scoreboard.value = it }
            _isLoading.value = false
        }
    }
}
