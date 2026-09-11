package com.caliarena.network

import com.caliarena.data.JudgeActionInput
import com.caliarena.data.JudgeActionType
import com.caliarena.data.JudgeWsEvent
import com.caliarena.data.MatchConnectionLost
import com.caliarena.data.RepSide
import com.caliarena.data.parseJudgeEvent
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
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders

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
                    .collect { frame -> _events.emit(parseJudgeEvent(json, frame.bodyAsText)) }
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
        }.map {}

    suspend fun close() {
        runCatching { session.disconnect() }
        scope.cancel()
    }
}
