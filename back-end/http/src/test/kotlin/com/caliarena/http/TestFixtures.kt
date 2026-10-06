package com.caliarena.http

import com.caliarena.domain.user.AuthenticatedUser
import com.caliarena.domain.user.PasswordValidationInfo
import com.caliarena.domain.user.User
import com.caliarena.domain.user.UserRole
import java.time.Instant

internal val TEST_USER_CREATED_AT: Instant = Instant.parse("2025-01-01T00:00:00Z")

internal fun testUser(
    id: Int = 1,
    username: String = "admin",
    role: UserRole = UserRole.ADMIN,
): User =
    User(
        id = id,
        username = username,
        password = PasswordValidationInfo("$username-hash"),
        role = role,
        createdAt = TEST_USER_CREATED_AT,
    )

internal fun authenticatedUser(
    id: Int = 1,
    username: String = "admin",
    role: UserRole = UserRole.ADMIN,
    token: String = "$username-token",
): AuthenticatedUser = AuthenticatedUser(user = testUser(id, username, role), token = token)

internal fun wsPrincipal(
    id: Int = 1,
    username: String = "admin",
    role: UserRole = UserRole.ADMIN,
): WsAuthenticatedPrincipal = WsAuthenticatedPrincipal(authenticatedUser(id, username, role))
