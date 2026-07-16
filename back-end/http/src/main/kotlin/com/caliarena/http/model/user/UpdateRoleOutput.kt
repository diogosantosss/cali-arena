package com.caliarena.http.model.user

import com.caliarena.domain.user.UserRole

data class UpdateRoleOutput(
    val userId: Int,
    val role: UserRole,
)
