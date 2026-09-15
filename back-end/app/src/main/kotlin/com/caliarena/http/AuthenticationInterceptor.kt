package com.caliarena.http

import com.caliarena.domain.RequiresRole
import com.caliarena.domain.user.AuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthenticationInterceptor(
    private val authorizationHeaderProcessor: RequestTokenProcessor,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val requiresAuth =
            handler is HandlerMethod &&
                (
                    handler.methodParameters.any { it.parameterType == AuthenticatedUser::class.java } ||
                        handler.getMethodAnnotation(RequiresRole::class.java) != null
                )

        if (requiresAuth) {
            // enforce authentication
            val user =
                authorizationHeaderProcessor.processAuthorizationHeaderValue(request.getHeader(NAME_AUTHORIZATION_HEADER))
            return if (user == null) {
                response.status = 401
                response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
                false
            } else {
                AuthenticatedUserArgumentResolver.Companion.addUserTo(user, request)
                true
            }
        }

        return true
    }

    companion object {
        const val NAME_AUTHORIZATION_HEADER = "Authorization"
        private const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
    }
}
