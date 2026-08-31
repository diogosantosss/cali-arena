package com.caliarena.ui.login

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.caliarena.R
import com.caliarena.ui.theme.CaliArenaTheme

/**
 * Screen do Login.
 *
 * Recebe o [LoginState] (do ViewModel ou de um estado local) e um callback de
 * eventos, e decide qual view mostrar consoante o estado:
 *  - [LoginScreenLoading] quando a sessão está a ser iniciada
 *  - [LoginScreenError] quando ocorre um erro
 *  - [LoginScreenView] caso contrário (formulário normal)
 */
@Composable
fun LoginScreen(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            state.isLoading -> LoginScreenLoading()
            state.errorMessage != null -> LoginScreenError(
                message = state.errorMessage,
                onRetry = { onEvent(LoginEvent.LoginSubmitted) },
            )
            else -> LoginScreenView(
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

/**
 * Construtor de teste/preview que mantém o estado localmente,
 * simulando a parte do ViewModel. Quando existir ViewModel real,
 * este helper deixa de ser usado.
 */
@Composable
fun rememberLoginState(): Pair<LoginState, (LoginEvent) -> Unit> {
    var state by rememberSaveable { mutableStateOf(LoginState()) }

    fun onEvent(event: LoginEvent) {
        state = when (event) {
            is LoginEvent.UsernameChanged -> state.copy(username = event.username)
            is LoginEvent.PasswordChanged -> state.copy(password = event.password)
            is LoginEvent.TogglePasswordVisibility -> state.copy(passwordVisible = !state.passwordVisible)
            LoginEvent.LoginSubmitted -> state.copy(isLoading = true)
            LoginEvent.ContactAdministrator -> state
        }
    }

    return state to ::onEvent
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    CaliArenaTheme {
        val (state, onEvent) = rememberLoginState()
        LoginScreen(state = state, onEvent = onEvent)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenWithErrorPreview() {
    CaliArenaTheme {
        LoginScreen(
            state = LoginState(errorMessage = R.string.error_login_invalid),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenLoadingPreview() {
    CaliArenaTheme {
        LoginScreen(
            state = LoginState(isLoading = true),
            onEvent = {},
        )
    }
}
