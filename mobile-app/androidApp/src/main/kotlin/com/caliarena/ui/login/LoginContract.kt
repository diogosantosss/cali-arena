package com.caliarena.ui.login

import androidx.annotation.StringRes

sealed interface LoginEvent {
    data object LoginSubmitted : LoginEvent
    data class UsernameChanged(val username: String) : LoginEvent
    data class PasswordChanged(val password: String) : LoginEvent
    data object TogglePasswordVisibility : LoginEvent
    data object ContactAdministrator : LoginEvent
}

data class LoginState(
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !isLoading
}
