package dev.reynardus.flinkly.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.remote.dto.DailyProgressDto
import dev.reynardus.flinkly.data.remote.dto.RecentCompletionDto
import dev.reynardus.flinkly.data.remote.dto.UserDto
import dev.reynardus.flinkly.data.store.PreferencesStore
import dev.reynardus.flinkly.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: ApiService,
    private val prefs: PreferencesStore,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _progress = MutableStateFlow<DailyProgressDto?>(null)
    val progress: StateFlow<DailyProgressDto?> = _progress

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user

    private val _recentCompletions = MutableStateFlow<List<RecentCompletionDto>>(emptyList())
    val recentCompletions: StateFlow<List<RecentCompletionDto>> = _recentCompletions

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _raccoonMood = MutableStateFlow<RaccoonMood>(RaccoonMood.ReadyChecklist)
    val raccoonMood: StateFlow<RaccoonMood> = _raccoonMood

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val householdId = prefs.householdId.first() ?: return@launch
            runCatching { api.getDailyProgress(householdId).body() }.getOrNull()
                ?.let { progress ->
                    _progress.value = progress
                    WidgetUpdater.updatePoints(context, progress.todayPoints)
                }
            val roomNames = runCatching { api.getRooms(householdId).body() }.getOrNull()
                ?.associate { it.id to it.name } ?: emptyMap()
            runCatching { api.getOpenHouseholdTasks(householdId).body() }.getOrNull()
                ?.let { tasks ->
                    val titles = tasks.map { task ->
                        val room = roomNames[task.roomId]
                        if (room != null) "${task.title} ($room)" else task.title
                    }
                    WidgetUpdater.updateTasks(context, tasks.size, titles)
                }
            runCatching { api.getMe().body() }.getOrNull()
                ?.let { _user.value = it }
            runCatching { api.getRecentCompletions(householdId, limit = 10).body() }.getOrNull()
                ?.let { _recentCompletions.value = it }
            _isLoading.value = false
            _raccoonMood.value = determineMood(_progress.value, _recentCompletions.value)
        }
    }
}

// Variant-Auswahl: wechselt stündlich, bleibt innerhalb der Stunde stabil.
private val variantA: Boolean
    get() = (System.currentTimeMillis() / 3_600_000L) % 2L == 0L

internal fun determineMood(
    progress: DailyProgressDto?,
    recentCompletions: List<RecentCompletionDto>,
): RaccoonMood {
    val hour = LocalTime.now().hour

    // 1. Haushaltspause
    if (progress?.isPaused == true) {
        return if (variantA) RaccoonMood.PausedSunglasses else RaccoonMood.PausedHammock
    }

    // 2. Tagesziel bereits erreicht
    if (progress?.goalReached == true) {
        return if (variantA) RaccoonMood.DoneBroom else RaccoonMood.DoneCelebrating
    }

    // 3. Morgens (vor 10 Uhr)
    if (hour < 10) {
        return if (variantA) RaccoonMood.MorningSleepy else RaccoonMood.MorningYawning
    }

    // 4. Gestern keine Aufgaben: Streak = 0 und kein Eintrag in den letzten 24 h
    val cutoff = Instant.now().minusSeconds(86_400)
    val hadRecentCompletion = recentCompletions.any { c ->
        runCatching { OffsetDateTime.parse(c.completedAt).toInstant().isAfter(cutoff) }
            .getOrDefault(false)
    }
    if ((progress?.streak ?: 1) == 0 && !hadRecentCompletion) {
        return if (variantA) RaccoonMood.LazyLaundry else RaccoonMood.LazyDishwasher
    }

    // 5. Guter Fortschritt (≥ 50 % des Tagesziels)
    if ((progress?.percent ?: 0) >= 50) {
        return if (variantA) RaccoonMood.ProgressMotivated else RaccoonMood.ProgressCleaning
    }

    // 6. Standard: bereit für den Tag
    return if (variantA) RaccoonMood.ReadyChecklist else RaccoonMood.ReadySupplies
}
