package dev.reynardus.flinkly.ui.screens.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.reynardus.flinkly.data.repository.AuthRepository
import dev.reynardus.flinkly.data.store.PreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: PreferencesStore,
) : ViewModel() {

    var isLoginMode by mutableStateOf(true)
        private set
    var displayName by mutableStateOf("")
        private set
    var userSecret by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pendingSecret by mutableStateOf<String?>(null)
        private set

    fun toggleMode() {
        isLoginMode = !isLoginMode
        error = null
    }

    fun onDisplayNameChange(v: String) { displayName = v; error = null }
    fun onSecretChange(v: String) { userSecret = v; error = null }
    fun onPasswordChange(v: String) { password = v; error = null }

    fun submit(onSuccess: (hasHousehold: Boolean) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            if (isLoginMode) {
                authRepository.login(userSecret)
                    .onSuccess { onSuccess(authRepository.hasHousehold()) }
                    .onFailure { error = it.message }
            } else {
                authRepository.register(displayName, password)
                    .onSuccess { pendingSecret = prefs.userSecret.first() }
                    .onFailure { error = it.message }
            }
            isLoading = false
        }
    }

    fun confirmSecretSaved(onSuccess: (hasHousehold: Boolean) -> Unit) {
        viewModelScope.launch {
            pendingSecret = null
            onSuccess(authRepository.hasHousehold())
        }
    }
}
