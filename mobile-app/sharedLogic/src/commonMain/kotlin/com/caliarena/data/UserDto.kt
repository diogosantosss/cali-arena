package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginInput(
    val username: String,
    val password: String,
)

@Serializable
data class UserLoginOutput(
    val token: String,
)

@Serializable
data class UserInfoOutput(
    val id: Int,
    val username: String,
    val role: UserRole,
    val createdAt: String,
)

@Serializable
enum class UserRole {
    ADMIN,
    JUDGE,
}
