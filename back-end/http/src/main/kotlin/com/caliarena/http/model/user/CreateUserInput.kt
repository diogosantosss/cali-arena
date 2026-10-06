package com.caliarena.http.model.user

import com.caliarena.domain.user.UserRole

data class CreateUserInput(
    val username: String,
    val password: String,
    val role: UserRole = UserRole.JUDGE,
)
