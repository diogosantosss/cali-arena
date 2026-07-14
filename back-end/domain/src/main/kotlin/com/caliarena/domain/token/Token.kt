package com.caliarena.domain.token

import com.caliarena.domain.user.UsersDomainConfig
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64.getUrlDecoder
import java.util.Base64.getUrlEncoder

data class Token(
    val tokenValidationInfo: TokenValidationInfo,
    val userId: Int,
    val createdAt: Instant,
    val lastUsedAt: Instant,
) {
    fun isTimeValid(clock: Clock, config: UsersDomainConfig): Boolean {
        val now = clock.instant()
        return createdAt <= now &&
                Duration.between(now, createdAt) <= config.tokenTtl &&
                Duration.between(now, lastUsedAt) <= config.tokenRollingTtl
    }

    fun expiration(config: UsersDomainConfig): Instant {
        val absoluteExpiration = createdAt + config.tokenTtl
        val rollingExpiration = lastUsedAt + config.tokenRollingTtl
        return minOf(absoluteExpiration, rollingExpiration)
    }

    companion object {
        fun generateTokenValue(config: UsersDomainConfig): String =
            ByteArray(config.tokenSizeInBytes).let { byteArray ->
                SecureRandom.getInstanceStrong().nextBytes(byteArray)
                getUrlEncoder().encodeToString(byteArray)
            }

        fun canBeToken(token: String, config: UsersDomainConfig): Boolean =
            try {
                getUrlDecoder().decode(token).size == config.tokenSizeInBytes
            } catch (ex: IllegalArgumentException) {
                false
            }
    }
}