package com.caliarena.http.websocket

import com.caliarena.domain.user.UserRole
import com.caliarena.http.RequestTokenProcessor
import com.caliarena.http.WsAuthenticatedPrincipal
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

        // coarse gate only: tournament scoped permissions are checked per message
        if (user.user.role !in ALLOWED_ROLES) {
            response.setStatusCode(HttpStatus.FORBIDDEN)
            return false
        }

        val principal = WsAuthenticatedPrincipal(user)

        @Suppress("UNCHECKED_CAST")
        (attributes as? MutableMap<String, Any>)?.let {
            it[USER_ATTRIBUTE] = principal
            // Spring also looks for a "principal" entry when resolving the session user
            it[PRINCIPAL_ATTRIBUTE] = principal
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
        /** Session attribute holding the [WsAuthenticatedPrincipal] of the connection. */
        const val USER_ATTRIBUTE = "caliarena.authenticatedUser"

        /** Key Spring's handshake handler reads to expose the session principal. */
        const val PRINCIPAL_ATTRIBUTE = "principal"

        private val ALLOWED_ROLES = setOf(UserRole.ADMIN, UserRole.HOST, UserRole.JUDGE)

        private val logger = LoggerFactory.getLogger(WsHandshakeInterceptor::class.java)
    }
}
