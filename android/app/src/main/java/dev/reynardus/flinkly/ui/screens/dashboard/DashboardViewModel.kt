package dev.reynardus.flinkly.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.DailyProgressDto
import dev.reynardus.flinkly.data.remote.dto.UserDto
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: ApiService,
    private val prefs: PreferencesStore,
) : ViewModel() {

    private val _progress = MutableStateFlow<DailyProgressDto?>(null)
    val progress: StateFlow<DailyProgressDto?> = _progress

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val householdId = prefs.householdId.first() ?: return@launch
            runCatching { api.getDailyProgress(householdId).body() }.getOrNull()
                ?.let { _progress.value = it }
            runCatching { api.getMe().body() }.getOrNull()
                ?.let { _user.value = it }
            _isLoading.value = false
        }
    }
}
