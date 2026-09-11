package com.caliarena.network

import com.caliarena.data.ErrorCode
import kotlinx.coroutines.CancellationException
import org.hildan.krossbow.stomp.ConnectionException
import org.hildan.krossbow.websocket.WebSocketConnectionException

internal suspend fun <T> runWs(block: suspend () -> T): Result<T> =
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
