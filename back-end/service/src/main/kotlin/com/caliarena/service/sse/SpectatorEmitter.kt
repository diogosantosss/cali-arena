package com.caliarena.service.sse

interface SpectatorEmitter {
    fun emit(
        id: Long,
        event: SpectatorEvent,
    )

    fun keepAlive(signal: KeepAliveEvent)

    fun onCompletion(callback: () -> Unit)

    fun onError(callback: (Throwable) -> Unit)
}
