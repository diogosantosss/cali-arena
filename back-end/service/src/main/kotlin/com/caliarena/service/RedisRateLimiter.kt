package com.caliarena.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Fixed-window rate limiter backed by Redis.
 *
 * Every request increments a per-key counter; the first one in the window sets
 * the key TTL, so windows reset automatically and need no cleanup job.
 *
 * Access fails open: when Redis is unreachable the request is allowed and the
 * failure is logged, so a cache outage never blocks a live event.
 */
@Service
class RedisRateLimiter(
    private val redisTemplate: StringRedisTemplate,
) {
    /**
     * Returns `true` only if every [keys] counter is still within [maxRequests]
     * inside a [windowSeconds] window. Use multiple keys to combine independent
     * limits in a single call (e.g. IP + username on login).
     */
    fun isAllowed(
        keys: List<String>,
        maxRequests: Int,
        windowSeconds: Int,
    ): Boolean = keys.all { allowSingle("rate_limit:$it", maxRequests, windowSeconds) }

    private fun allowSingle(
        key: String,
        maxRequests: Int,
        windowSeconds: Int,
    ): Boolean {
        val count =
            try {
                val current = redisTemplate.opsForValue().increment(key) ?: return false

                // First hit of the window arms the TTL so the key cleans itself up.
                if (current == 1L) {
                    redisTemplate.expire(key, Duration.ofSeconds(windowSeconds.toLong()))
                }

                current
            } catch (e: RedisConnectionFailureException) {
                log.warn("Redis unavailable, allowing request (key: {})", key)
                return true
            }

        return count <= maxRequests.toLong()
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RedisRateLimiter::class.java)
    }
}
