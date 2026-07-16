package com.caliarena.http.utils

import com.caliarena.service.Either
import com.caliarena.service.Failure
import com.caliarena.service.Success
import org.springframework.http.ResponseEntity

/**
 * Converts an Either value to a ResponseEntity based on the provided success and error handlers.
 *
 * @param onSuccess A function that takes a success value and returns a ResponseEntity.
 * @param onError A function that takes an error value and returns a ResponseEntity.
 *
 * @return A ResponseEntity representing the result of the Either value that will be returned to the client.
 */
fun <E, T> Either<E, T>.toResponse(
    onSuccess: (T) -> ResponseEntity<Any>,
    onError: (E) -> ResponseEntity<Any>,
): ResponseEntity<Any> =
    when (this) {
        is Success -> onSuccess(value)
        is Failure -> onError(value)
    }
