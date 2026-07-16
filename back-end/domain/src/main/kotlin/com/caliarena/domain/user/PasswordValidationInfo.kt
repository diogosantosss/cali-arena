package com.caliarena.domain.user

data class PasswordValidationInfo(
    val validationInfo: String,
) {
    companion object {
        fun isSafePassword(password: String): Boolean =
            password.length > 4 && password.any { it.isDigit() } && password.any { it.isUpperCase() }
    }
}