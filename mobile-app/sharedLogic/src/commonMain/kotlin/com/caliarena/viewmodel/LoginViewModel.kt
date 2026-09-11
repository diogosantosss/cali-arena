package com.caliarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caliarena.data.ErrorCode
import com.caliarena.network.CaliApiException
import com.caliarena.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState

    data object Loading : LoginUiState

    data class Error(
        val code: ErrorCode,
    ) : LoginUiState

    data class Success(
        val username: String,
    ) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(
        username: String,
        password: String,
    ) {
        val trimmedUsername = username.trim()
        when {
            trimmedUsername.isEmpty() -> {
                _uiState.value = LoginUiState.Error(ErrorCode.USERNAME_REQUIRED)
            }

            password.isEmpty() -> {
                _uiState.value = LoginUiState.Error(ErrorCode.PASSWORD_REQUIRED)
            }

            else -> submitLogin(trimmedUsername, password)
        }
    }

    private fun submitLogin(
        username: String,
        password: String,
    ) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.login(username, password).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success(username)
                },
                onFailure = { error ->
                    val apiError = error as? CaliApiException
                    _uiState.value =
                        LoginUiState.Error(
                            code = apiError?.code ?: ErrorCode.UNKNOWN_ERROR,
                        )
                },
            )
        }
    }
}
