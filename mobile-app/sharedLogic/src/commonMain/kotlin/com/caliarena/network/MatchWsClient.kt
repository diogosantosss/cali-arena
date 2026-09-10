package com.caliarena.network

import com.caliarena.auth.TokenStorage
import com.caliarena.data.ErrorCode
import com.caliarena.data.JudgeActionInput
import com.caliarena.data.JudgeActionType
import com.caliarena.data.JudgeWsEvent
import com.caliarena.data.MatchConnectionLost
import com.caliarena.data.RepSide
import com.caliarena.data.parseJudgeEvent
import com.caliarena.repository.NotAuthenticatedException
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.ConnectionException
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.websocket.WebSocketConnectionException
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

class MatchWsClient(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private val json: Json,
) {
    private val wsBase: String = baseUrl.trimEnd('/').toWsBase()

    suspend fun open(matchId: Int): Result<MatchWsSession> {
        val token =
            tokenStorage.readSession()?.token
                ?: return Result.failure(NotAuthenticatedException())
        return runWs {
            StompClient(KtorWebSocketClient(httpClient)).connect("$wsBase/ws?token=$token")
        }.map { MatchWsSession(matchId, it, json) }
    }

    private fun String.toWsBase(): String =
        when {
            startsWith("https://") -> replaceFirst("https://", "wss://")
            startsWith("http://") -> replaceFirst("http://", "ws://")
            else -> this
        }
}

private suspend fun <T> runWs(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(CaliApiException(code = e.toWsCode()))
    }

private fun Throwable.toWsCode(): ErrorCode =
    when (this) {
        is WebSocketConnectionException ->
            if (httpStatusCode == 401 || httpStatusCode == 403) {
                ErrorCode.SESSION_INVALID
            } else {
                ErrorCode.NO_CONNECTION
            }

        is ConnectionException -> ErrorCode.NO_CONNECTION
        else -> ErrorCode.UNKNOWN_ERROR
    }

class MatchWsSession(
    private val matchId: Int,
    private val session: StompSession,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events = MutableSharedFlow<JudgeWsEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<JudgeWsEvent> = _events.asSharedFlow()

    init {
        scope.launch {
            try {
                session
                    .subscribe(StompSubscribeHeaders("/topic/matches/$matchId"))
                    .collect { frame ->
                        _events.emit(parseJudgeEvent(json, frame.bodyAsText))
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _events.emit(MatchConnectionLost)
            }
        }
    }

    suspend fun adjustSide(
        side: RepSide,
        reps: Int,
    ): Result<Unit> = sendAction(JudgeActionInput(JudgeActionType.ADJUST, side, reps))

    suspend fun finishSide(side: RepSide): Result<Unit> = sendAction(JudgeActionInput(JudgeActionType.FINISH, side))

    private suspend fun sendAction(input: JudgeActionInput): Result<Unit> =
        runWs {
            session.send(
                headers = StompSendHeaders("/app/matches/$matchId/actions"),
                body = FrameBody.Text(json.encodeToString(input)),
            )
        }.map { }

    suspend fun close() {
        runCatching { session.disconnect() }
        scope.cancel()
    }
}
