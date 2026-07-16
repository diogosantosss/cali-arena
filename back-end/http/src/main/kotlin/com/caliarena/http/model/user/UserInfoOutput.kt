package com.caliarena.http.model.user

import com.caliarena.domain.user.UserRole
import java.time.Instant

data class UserInfoOutput(
    val id: Int,
    val username: String,
    val role: UserRole,
    val createdAt: Instant,
)
