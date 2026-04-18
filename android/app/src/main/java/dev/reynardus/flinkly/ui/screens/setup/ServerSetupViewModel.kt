package dev.reynardus.flinkly.ui.screens.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var serverUrl by mutableStateOf("https://flinkly.ut.reynardus.dev")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun onUrlChange(value: String) {
        serverUrl = value
        error = null
    }

    fun checkServer(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            authRepository.checkServerHealth(serverUrl)
                .onSuccess { onSuccess() }
                .onFailure { error = it.message }
            isLoading = false
        }
    }
}
