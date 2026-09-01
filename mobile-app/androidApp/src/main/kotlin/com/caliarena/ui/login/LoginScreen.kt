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
import com.caliarena.data.ErrorCode
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLogin: (username: String, password: String) -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (uiState) {
            LoginUiState.Idle, is LoginUiState.Error ->
                LoginForm(
                    uiState = uiState,
                    onLogin = onLogin,
                )

            LoginUiState.Loading -> LoginScreenLoading()

            is LoginUiState.Success -> {
                onLoginSuccess()
                LoginScreenLoading()
            }
        }
    }
}

@Composable
private fun LoginForm(
    uiState: LoginUiState,
    onLogin: (username: String, password: String) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val errorCode =
        when (uiState) {
            is LoginUiState.Error -> uiState.code
            else -> null
        }

    LoginScreenView(
        username = username,
        password = password,
        errorCode = errorCode,
        onUsernameChange = { username = it },
        onPasswordChange = { password = it },
        onSubmit = { onLogin(username, password) },
        onContactAdministrator = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    CaliArenaTheme {
        LoginScreen(
            uiState = LoginUiState.Idle,
            onLogin = { _, _ -> },
            onLoginSuccess = {},
        )
    }
}

@Preview(name = "Login Error", showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenWithErrorPreview() {
    CaliArenaTheme {
        LoginScreen(
            uiState = LoginUiState.Error(ErrorCode.INVALID_CREDENTIALS),
            onLogin = { _, _ -> },
            onLoginSuccess = {},
        )
    }
}
