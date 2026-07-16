package com.caliarena.http.model

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val PROBLEM_URI_PATH = "/problem"

sealed class Problem(
    typeUri: URI,
) {
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header(HttpHeaders.CONTENT_TYPE, MEDIA_TYPE)
            .body(ProblemBody(type, title, status.value()))

    data object InvalidRequestContent : Problem(URI("$PROBLEM_URI_PATH/invalid-request-content"))

    // User related Errors
    data object AlreadyUsedUsername : Problem(URI("$PROBLEM_URI_PATH/already-used-username"))

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object ErrorUpdatingUserRole : Problem(URI("$PROBLEM_URI_PATH/error-updating-user-role"))

    data object UserNotFound : Problem(URI("$PROBLEM_URI_PATH/not-found"))

    data object InvalidRole : Problem(URI("$PROBLEM_URI_PATH/invalid-role"))

    data object UserOrPasswordAreInvalid : Problem(URI("$PROBLEM_URI_PATH/user-or-password-are-invalid"))

    data object NotAuthorized : Problem(URI("$PROBLEM_URI_PATH/not-authorized"))

    private data class ProblemBody(
        val type: String,
        val title: String,
        val status: Int,
    )

    companion object {
        private const val MEDIA_TYPE = "application/problem+json"
    }
}
