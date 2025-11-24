package main.java.dev.ntziks.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isRememberMe: Boolean = false,
    val isOnline: Boolean = true,
    val failureCount: Int = 0,
    val isLockedOut: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val token: String? = null,
    val navigateToHome: Boolean = false
) {
    val isLoginEnabled: Boolean
        get() = username.isNotBlank() &&
                password.isNotBlank() &&
                !isLockedOut &&
                isOnline &&
                !isLoading
}

