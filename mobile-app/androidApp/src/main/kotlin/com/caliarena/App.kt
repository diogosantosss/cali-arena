package com.caliarena

import androidx.compose.runtime.Composable
import com.caliarena.ui.login.LoginScreen
import com.caliarena.ui.login.rememberLoginState
import com.caliarena.ui.theme.CaliArenaTheme

@Composable
fun CaliArenaApp() {
    CaliArenaTheme {
        val (loginState, onLoginEvent) = rememberLoginState()
        LoginScreen(state = loginState, onEvent = onLoginEvent)
    }
}
