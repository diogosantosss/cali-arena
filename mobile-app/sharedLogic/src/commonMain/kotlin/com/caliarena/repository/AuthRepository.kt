package com.caliarena.repository

import com.caliarena.auth.StoredSession
import com.caliarena.auth.TokenStorage
import com.caliarena.data.UserInfoOutput
import com.caliarena.data.UserLoginInput
import com.caliarena.data.UserRole
import com.caliarena.network.CaliApiClient

class AuthRepository(
    private val api: CaliApiClient,
    private val tokenStorage: TokenStorage,
) {
    suspend fun login(
        username: String,
        password: String,
    ): Result<Unit> {
        val result = api.login(UserLoginInput(username, password))
        val output = result.getOrNull() ?: return Result.failure(result.exceptionOrNull()!!)
        val role = fetchRole(output.token)
        tokenStorage.save(StoredSession(output.token, username, role))
        return Result.success(Unit)
    }

    suspend fun readSession(): StoredSession? = tokenStorage.readSession()

    suspend fun me(): Result<UserInfoOutput> {
        val token =
            tokenStorage.readSession()?.token
                ?: return Result.failure(NotAuthenticatedException())
        return api.me(token)
    }

    suspend fun logout() {
        tokenStorage.readSession()?.token?.let { runCatching { api.logout(it) } }
        tokenStorage.clear()
    }

    private suspend fun fetchRole(token: String): UserRole? = api.me(token).getOrNull()?.role
}

class NotAuthenticatedException : Exception("No session available")
