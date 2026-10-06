package com.caliarena.http.websocket

import com.caliarena.http.WsAuthenticatedPrincipal
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

/**
 * Copies the principal stored during the handshake into every STOMP message, so
 * `@MessageMapping` handlers can receive the acting user and authorize the
 * requested resource against it.
 */
@Component
class WsUserChannelInterceptor : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
                ?: return message

        val sessionAttributes = (accessor as? SimpMessageHeaderAccessor)?.sessionAttributes

        val principal =
            sessionAttributes?.get(WsHandshakeInterceptor.USER_ATTRIBUTE)
                ?: accessor.user

        if (principal !is WsAuthenticatedPrincipal || accessor.user === principal) {
            return message
        }

        accessor.user = principal

        return MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
    }
}
