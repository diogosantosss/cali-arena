package com.caliarena.domain.user

import java.time.Duration


/**
 * Configuration for the user domain's login-token policy.
 *
 * A token is valid until the earlier of two deadlines: its absolute lifetime
 * ([tokenTtl]) or its inactivity timeout ([tokenRollingTtl]).
 *
 * @param tokenSizeInBytes length of the raw random token value, in bytes
 * (e.g. 32 for a 256-bit token).
 * @param tokenTtl absolute time-to-live of a token from its creation.
 * @param tokenRollingTtl inactivity timeout that renews whenever the token
 * is used.
 * @param maxTokensPerUser how many active tokens a single user may hold at
 * once; creating more deletes the oldest ones.
 */
data class UsersDomainConfig(
    val tokenSizeInBytes: Int,
    val tokenTtl: Duration,
    val tokenRollingTtl: Duration,
    val maxTokensPerUser: Int,
) {
    init {
        require(tokenSizeInBytes > 0)
        require(tokenTtl.isPositive)
        require(tokenRollingTtl.isPositive)
        require(maxTokensPerUser > 0)
    }
}