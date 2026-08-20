package com.caliarena.http

import com.caliarena.service.sse.KeepAliveEvent
import com.caliarena.service.sse.SpectatorEmitter
import com.caliarena.service.sse.SpectatorEvent
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseSpectatorEmitterAdapter(
    private val emitter: SseEmitter,
) : SpectatorEmitter {
    override fun emit(
        id: Long,
        event: SpectatorEvent,
    ) {
        emitter.send(
            SseEmitter
                .event()
                .id(id.toString())
                .name(event.action.name)
                .data(event),
        )
    }

    override fun keepAlive(signal: KeepAliveEvent) {
        emitter.send(
            SseEmitter
                .event()
                .name("KEEP_ALIVE")
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
