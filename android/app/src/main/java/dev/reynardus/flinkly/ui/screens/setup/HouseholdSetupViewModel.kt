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
class HouseholdSetupViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository,
) : ViewModel() {

    var isCreateMode by mutableStateOf(true)
        private set
    var householdName by mutableStateOf("")
        private set
    var inviteInput by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun toggleMode() { isCreateMode = !isCreateMode; error = null }
    fun onNameChange(v: String) { householdName = v; error = null }
    fun onInviteChange(v: String) { inviteInput = v; error = null }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            if (isCreateMode) {
                householdRepository.createHousehold(householdName)
                    .onSuccess { onSuccess() }
                    .onFailure { error = it.message }
            } else {
                val token = inviteInput.substringAfterLast("/").trim()
                householdRepository.joinHousehold(token)
                    .onSuccess { onSuccess() }
                    .onFailure { error = it.message }
            }
            isLoading = false
        }
    }
}
