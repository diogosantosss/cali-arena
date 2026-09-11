package com.caliarena.http.utils

import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.UserRole

/**
 * Returns true when the authenticated user holds any of the given roles.
 *
 * Role enforcement lives on the HTTP layer: controllers resolve the
 * [AuthenticatedUser] (auth is enforced by the request interceptor) and
 * reject the request with FORBIDDEN when this check fails.
 * WebSocket judge actions are already guarded at the handshake.
 */
fun AuthenticatedUser.hasAnyRole(vararg allowed: UserRole): Boolean = allowed.any { it == user.role }
