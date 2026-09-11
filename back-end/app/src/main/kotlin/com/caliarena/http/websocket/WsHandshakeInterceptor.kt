package com.caliarena.http.websocket

import com.caliarena.domain.user.UserRole
import com.caliarena.http.RequestTokenProcessor
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.lang.Exception

@Component
class WsHandshakeInterceptor(
    private val tokenProcessor: RequestTokenProcessor,
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: Map<String, Any>,
    ): Boolean {
        val servlet = (request as ServletServerHttpRequest).servletRequest

        val user = tokenProcessor.processAuthorizationHeaderValue("Bearer ${servlet.getParameter("token")}")
        if (user == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        // only logging in dev profile
        logger.debug(
            "user {}",
            user.user,
        )
        if (user.user.role != UserRole.JUDGE && user.user.role != UserRole.ADMIN) {
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        if (exception != null) {
            logger.warn("WS handshake failed for {}: {}", request.uri, exception.message)
        } else {
            logger.debug("WS handshake completed for {}", request.uri)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WsHandshakeInterceptor::class.java)
    }
}
