package com.caliarena.domain.user

class AuthenticatedUser(
    val user: User,
    val token: String,
)