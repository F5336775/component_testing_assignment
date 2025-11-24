package main.java.dev.ntziks.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import main.java.dev.ntziks.login.LoginUiState
import main.java.dev.ntziks.login.NetworkMonitor
import main.java.dev.ntziks.login.repository.AuthRepository

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChanged(value: String) {
        _state.update { it.copy(username = value, errorMessage = null, navigateToHome = false) }
    }

    fun onPasswordChanged(value: String) {
        _state.update { it.copy(password = value, errorMessage = null, navigateToHome = false) }
    }

    fun onRememberMeChanged(checked: Boolean) {
        _state.update { it.copy(isRememberMe = checked) }
    }

    fun refreshOnlineStatus() {
        _state.update { it.copy(isOnline = networkMonitor.isOnline()) }
    }

    fun login() {
        val current = _state.value

        if (current.isLockedOut) {
            _state.update { it.copy(errorMessage = "Account locked") }
            return
        }

        if (!networkMonitor.isOnline()) {
            _state.update { it.copy(isOnline = false, errorMessage = "You are offline") }
            return
        }

        if (!current.isLoginEnabled) {
            _state.update { it.copy(errorMessage = "Invalid credentials") }
            return
        }

        viewModelScope.launch(dispatcher) {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(current.username, current.password)

            result.onSuccess { token ->
                // success → navigation
                _state.update {
                    it.copy(
                        isLoading = false,
                        token = token,
                        navigateToHome = true,
                        failureCount = 0
                    )
                }
                // remember-me persistence could be handled by AuthRepository internally
            }.onFailure {
                val newFailures = current.failureCount + 1
                val locked = newFailures >= 3
                _state.update {
                    it.copy(
                        isLoading = false,
                        failureCount = newFailures,
                        isLockedOut = locked,
                        errorMessage = if (locked) "Too many attempts" else "Login failed"
                    )
                }
            }
        }
    }
}
