package com.caliarena.http

import com.caliarena.service.sse.KeepAliveEvent
import com.caliarena.service.sse.ScreenRoutinesUpdatedEvent
import com.caliarena.service.sse.SpectatorEmitter
import com.caliarena.service.sse.SpectatorEvent
import com.caliarena.service.sse.TournamentStateUpdatedEvent
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

private enum class SpectatorEventType {
    TOURNAMENT_STATE_UPDATED,
    SCREEN_ROUTINES_UPDATED,
    KEEP_ALIVE,
}

class SseSpectatorEmitterAdapter(
    private val emitter: SseEmitter,
) : SpectatorEmitter {
    override fun emit(
        id: Long,
        event: SpectatorEvent,
    ) {
        val type =
            when (event) {
                is TournamentStateUpdatedEvent ->
                    SpectatorEventType.TOURNAMENT_STATE_UPDATED
                is ScreenRoutinesUpdatedEvent ->
                    SpectatorEventType.SCREEN_ROUTINES_UPDATED
            }

        emitter.send(
            SseEmitter
                .event()
                .id(id.toString())
                .name(type.name)
                .data(event),
        )
    }

    override fun keepAlive(signal: KeepAliveEvent) {
        emitter.send(
            SseEmitter
                .event()
                .name(SpectatorEventType.KEEP_ALIVE.name)
                .data(signal),
        )
    }

    override fun onCompletion(callback: () -> Unit) {
        emitter.onCompletion(callback)
    }

    override fun onError(callback: (Throwable) -> Unit) {
        emitter.onError(callback)
    }
}
