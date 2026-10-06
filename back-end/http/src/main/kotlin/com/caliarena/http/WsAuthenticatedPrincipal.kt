package com.caliarena.http

import com.caliarena.domain.user.AuthenticatedUser
import java.security.Principal

/**
 * Principal carried from the STOMP handshake to every message of the connection,
 * so judge actions can be authorized against the acting user instead of relying
 * on the coarse handshake role alone.
 */
class WsAuthenticatedPrincipal(
    val authenticatedUser: AuthenticatedUser,
) : Principal {
    override fun getName(): String = authenticatedUser.user.username
}
