package com.caliarena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.caliarena.ui.home.HomeScreen
import com.caliarena.ui.login.LoginScreen
import com.caliarena.ui.theme.CaliArenaTheme
import org.koin.compose.viewmodel.koinViewModel

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun CaliArenaApp() {
    CaliArenaTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
        ) {
            composable(Routes.LOGIN) {
                val viewModel = koinViewModel<com.caliarena.viewmodel.LoginViewModel>()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LoginScreen(
                    uiState = uiState,
                    onLogin = viewModel::login,
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                HomeScreen()
            }
        }
    }
}
