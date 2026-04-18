package dev.reynardus.flinkly.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.AchievementDto
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val api: ApiService,
    private val prefs: PreferencesStore,
) : ViewModel() {

    private val _achievements = MutableStateFlow<List<AchievementDto>>(emptyList())
    val achievements: StateFlow<List<AchievementDto>> = _achievements

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = prefs.userId.first() ?: return@launch
            runCatching { api.getAchievements(userId).body() }.getOrNull()
                ?.let { _achievements.value = it }
            _isLoading.value = false
        }
    }
}
