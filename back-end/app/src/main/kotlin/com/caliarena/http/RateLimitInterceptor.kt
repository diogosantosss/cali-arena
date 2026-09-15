package com.caliarena.http

import com.caliarena.domain.RateLimited
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.PROBLEM_URI_PATH
import com.caliarena.http.model.ProblemBody
import com.caliarena.service.RedisRateLimiter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Enforces [RateLimited] annotations on controller methods.
 *
 * Must run after [AuthenticationInterceptor] so the authenticated user is already
 * available in the request. Identity is the user id for authenticated calls and
 * the client IP for public endpoints. Exceeding the quota returns HTTP 429 with
 * the same RFC 9457 problem+json body as every other API error.
 */
@Component
class RateLimitInterceptor(
    private val rateLimiter: RedisRateLimiter,
    private val objectMapper: ObjectMapper,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true

        val rateLimited = handler.getMethodAnnotation(RateLimited::class.java) ?: return true

        // Authenticated → per user; public endpoints → per IP.
        val key =
            AuthenticatedUserArgumentResolver
                .getUserFrom(request)
                ?.user
                ?.id
                ?.let { "user:$it" }
                ?: "ip:${clientIp(request)}"

        if (!rateLimiter.isAllowed(listOf(key), rateLimited.maxRequests, rateLimited.windowSeconds)) {
            errorOutput(response)
            return false
        }

        return true
    }

    /**
     * Client IP behind the nginx reverse proxy.
     *
     * Without this header every request looks like it comes from the proxy
     * address, which would collapse all clients into a single bucket.
     */
    private fun clientIp(request: HttpServletRequest): String =
        request
            .getHeader("X-Forwarded-For")
            ?.substringBefore(",")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr

    private fun errorOutput(response: HttpServletResponse) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = PROBLEM_MEDIA_TYPE

        objectMapper.writeValue(
            response.writer,
            ProblemBody(
                type = "$PROBLEM_URI_PATH/too-many-requests",
                title = "too-many-requests",
                status = HttpStatus.TOO_MANY_REQUESTS.value(),
            ),
        )
    }
}
