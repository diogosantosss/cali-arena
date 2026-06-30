package com.caliarena.domain.user

import java.time.Instant

data class User(
    val id: Int,
    val username: String,
    val password: PasswordValidationInfo,
    val role : UserRole,
    val createdAt: Instant,
)
