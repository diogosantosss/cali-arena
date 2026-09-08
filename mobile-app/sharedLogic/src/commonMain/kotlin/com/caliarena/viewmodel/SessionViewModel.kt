package com.caliarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caliarena.data.UserRole
import com.caliarena.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class Authenticated(
        val username: String,
        val role: UserRole?,
    ) : SessionUiState

    data class LoggingOut(
        val username: String,
        val role: UserRole?,
    ) : SessionUiState

    data object LoggedOut : SessionUiState

    data object Invalid : SessionUiState
}

class SessionViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = SessionUiState.Loading
            val session = authRepository.readSession()
            _state.value =
                if (session == null || session.token.isBlank() || session.username.isBlank() || session.role == null) {
                    SessionUiState.Invalid
                } else {
                    SessionUiState.Authenticated(
                        username = session.username,
                        role = session.role,
                    )
                }
        }
    }

    fun logout(onComplete: () -> Unit) {
        if (_state.value is SessionUiState.LoggingOut) return
        viewModelScope.launch {
            val current = _state.value as? SessionUiState.Authenticated
            _state.value =
                SessionUiState.LoggingOut(
                    username = current?.username.orEmpty(),
                    role = current?.role,
                )
            authRepository.logout()
            _state.value = SessionUiState.LoggedOut
            onComplete()
        }
    }
}
