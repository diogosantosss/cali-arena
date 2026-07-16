package com.caliarena.http.model.user

data class UpdateRoleInput(
    val userToUpdateId: Int,
    val role: String,
)
