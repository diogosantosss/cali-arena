package com.caliarena.repository

import com.caliarena.auth.TokenStorage
import com.caliarena.data.UserInfoOutput
import com.caliarena.data.UserLoginInput
import com.caliarena.network.CaliApiClient
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val api: CaliApiClient,
    private val tokenStorage: TokenStorage,
) {
    val token: Flow<String?> = tokenStorage.token

    val username: Flow<String?> = tokenStorage.username

    suspend fun login(
        username: String,
        password: String,
    ): Result<Unit> {
        val result = api.login(UserLoginInput(username, password))
        val token = result.getOrNull()?.token ?: return Result.failure(result.exceptionOrNull()!!)
        tokenStorage.saveSession(token, username)
        return Result.success(Unit)
    }

    suspend fun me(): Result<UserInfoOutput> {
        val token =
            tokenStorage.readToken()
                ?: return Result.failure(NotAuthenticatedException())
        return api.me(token)
    }

    suspend fun logout() {
        tokenStorage.readToken()?.let { runCatching { api.logout(it) } }
        tokenStorage.clear()
    }
}

class NotAuthenticatedException : Exception("No session available")
