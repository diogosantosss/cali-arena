package com.caliarena.http.model

import com.caliarena.service.ApiError
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

const val PROBLEM_MEDIA_TYPE = "application/problem+json"
const val PROBLEM_URI_PATH = "/problem"

data class ProblemBody(
    val type: String,
    val title: String,
    val status: Int,
)

fun ApiError.toResponseEntity(): ResponseEntity<Any> =
    ResponseEntity
        .status(status)
        .header(HttpHeaders.CONTENT_TYPE, PROBLEM_MEDIA_TYPE)
        .body(ProblemBody("$PROBLEM_URI_PATH/$problemType", problemType, status.value()))

fun invalidRequestContent(): ResponseEntity<Any> =
    ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.CONTENT_TYPE, PROBLEM_MEDIA_TYPE)
        .body(ProblemBody("$PROBLEM_URI_PATH/invalid-request-content", "invalid-request-content", HttpStatus.BAD_REQUEST.value()))
