package com.caliarena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.caliarena.data.ErrorCode
import com.caliarena.ui.home.HomeScreen
import com.caliarena.ui.login.LoginScreen
import com.caliarena.ui.match.MatchScreen
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.viewmodel.LoginViewModel
import com.caliarena.viewmodel.MatchViewModel
import com.caliarena.viewmodel.MatchesViewModel
import com.caliarena.viewmodel.SessionUiState
import com.caliarena.viewmodel.SessionViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val MATCH = "match/{matchId}"
    const val MATCH_ARG = "matchId"

    const val SESSION_ERROR_ARG = "session_error"
    const val LOGIN_SESSION_ERROR = "$LOGIN?$SESSION_ERROR_ARG=true"

    fun matchRoute(matchId: Int) = "match/$matchId"
}

@Composable
fun CaliArenaApp() {
    CaliArenaTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
        ) {
            composable(
                route = Routes.LOGIN,
                arguments =
                    listOf(
                        navArgument(Routes.SESSION_ERROR_ARG) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
            ) { backStackEntry ->
                val viewModel = koinViewModel<LoginViewModel>()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val hasSessionError = backStackEntry.arguments?.getBoolean(Routes.SESSION_ERROR_ARG) == true

                LoginScreen(
                    uiState = uiState,
                    sessionError = if (hasSessionError) ErrorCode.SESSION_INVALID else null,
                    onLogin = viewModel::login,
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                val sessionViewModel = koinViewModel<SessionViewModel>()
                val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
                val matchesViewModel = koinViewModel<MatchesViewModel>()
                val matchesUiState by matchesViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(sessionState) {
                    when (sessionState) {
                        SessionUiState.Invalid ->
                            navController.navigate(Routes.LOGIN_SESSION_ERROR) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }

                        SessionUiState.LoggedOut ->
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }

                        else -> Unit
                    }
                }

                when (val state = sessionState) {
                    SessionUiState.Loading -> SessionLoadingScreen()

                    is SessionUiState.Authenticated ->
                        HomeScreen(
                            username = state.username,
                            role = state.role,
                            matchesUiState = matchesUiState,
                            onMatchClick = { item -> navController.navigate(Routes.matchRoute(item.match.id)) },
                            onRetry = matchesViewModel::load,
                            onRefresh = matchesViewModel::load,
                            loggingOut = false,
                            onLogout = {
                                sessionViewModel.logout {
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            },
                        )

                    is SessionUiState.LoggingOut ->
                        HomeScreen(
                            username = state.username,
                            role = state.role,
                            matchesUiState = matchesUiState,
                            onMatchClick = {},
                            onRetry = {},
                            onRefresh = {},
                            loggingOut = true,
                            onLogout = {},
                        )

                    SessionUiState.LoggedOut, SessionUiState.Invalid -> Unit
                }
            }

            composable(
                route = Routes.MATCH,
                arguments = listOf(navArgument(Routes.MATCH_ARG) { type = NavType.IntType }),
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getInt(Routes.MATCH_ARG) ?: return@composable
                val viewModel = koinViewModel<MatchViewModel>(parameters = { parametersOf(matchId) })
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                MatchScreen(
                    matchId = matchId,
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onRetry = viewModel::load,
                    onReconnect = viewModel::reconnect,
                    onAdjust = viewModel::adjust,
                    onFinish = viewModel::finish,
                )
            }
        }
    }
}

@Composable
private fun SessionLoadingScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
