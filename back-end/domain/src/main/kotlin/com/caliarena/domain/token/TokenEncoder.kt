package com.caliarena.domain.token

interface TokenEncoder {
    fun createValidationInformation(token: String): TokenValidationInfo
}