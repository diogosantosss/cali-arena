package com.caliarena

import com.caliarena.http.model.invalidRequestContent
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.info("Handling MethodArgumentNotValidException: {}", ex.message)
        return invalidRequestContent()
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        log.info("Handling HttpMessageNotReadableException: {}", ex.message)
        return invalidRequestContent()
    }

    @ExceptionHandler(
        Exception::class,
    )
    fun handleAll(
        req: HttpServletRequest,
        ex: Exception,
    ): ResponseEntity<Unit> {
        log.error("Request: ${req.requestURL} raised $ex")
        return ResponseEntity.status(500).build()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }
}
