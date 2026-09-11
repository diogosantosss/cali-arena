package com.caliarena.network

import com.caliarena.auth.TokenStorage
import com.caliarena.repository.NotAuthenticatedException
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

class MatchWsClient(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {
    private val wsBaseUrl: String = baseUrl.trimEnd('/').toWsBase()

    suspend fun open(matchId: Int): Result<MatchWsSession> {
        val token =
            tokenStorage.readSession()?.token
                ?: return Result.failure(NotAuthenticatedException())
        return runWs {
            StompClient(KtorWebSocketClient(httpClient)).connect("$wsBaseUrl/ws?token=$token")
        }.map { MatchWsSession(matchId, it, json) }
    }
}

private fun String.toWsBase(): String =
    when {
        startsWith("https://") -> replaceFirst("https://", "wss://")
        startsWith("http://") -> replaceFirst("http://", "ws://")
        else -> this
    }
