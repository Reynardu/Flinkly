package dev.reynardus.flinkly.ui.screens.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.repository.HouseholdRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinHouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun join(token: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = householdRepository.joinHousehold(token)
            isLoading = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error = it.message ?: "Beitritt fehlgeschlagen" },
            )
        }
    }
}
