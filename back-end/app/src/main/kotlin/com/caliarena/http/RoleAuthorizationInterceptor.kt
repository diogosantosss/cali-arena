package com.caliarena.http

import com.caliarena.domain.RequiresRole
import com.caliarena.http.model.PROBLEM_MEDIA_TYPE
import com.caliarena.http.model.PROBLEM_URI_PATH
import com.caliarena.http.model.ProblemBody
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Enforces [RequiresRole] annotations on controller methods.
 *
 * Registered after [AuthenticationInterceptor], so the authenticated user is
 * already available on the request. Unauthenticated calls get 401; calls with a
 * role outside [RequiresRole.allowedRoles] get 403 with the standard RFC 9457
 * problem+json body.
 */
@Component
class RoleAuthorizationInterceptor(
    private val objectMapper: ObjectMapper,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true

        val requiresRole = handler.getMethodAnnotation(RequiresRole::class.java) ?: return true

        val authenticatedUser =
            AuthenticatedUserArgumentResolver.getUserFrom(request)
                ?: return writeUnauthorized(response)

        if (authenticatedUser.user.role !in requiresRole.allowedRoles) {
            writeForbidden(response)
            return false
        }

        return true
    }

    private fun writeUnauthorized(response: HttpServletResponse): Boolean {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
        return false
    }

    private fun writeForbidden(response: HttpServletResponse) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = PROBLEM_MEDIA_TYPE
        objectMapper.writeValue(
            response.writer,
            ProblemBody(
                type = "$PROBLEM_URI_PATH/not-authorized",
                title = "not-authorized",
                status = HttpStatus.FORBIDDEN.value(),
            ),
        )
    }

    private companion object {
        const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
    }
}
