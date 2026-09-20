package com.caliarena.service

/**
 * Discriminated union representing either a [Left] (failure) or a [Right] (success).
 *
 * Used as the return type of service operations: a computation produces a
 * [Right] with the successful value or a [Left] with an error, instead of
 * throwing. Callers combine results with [success] and [failure].
 *
 * Both type parameters are covariant (`out`), so `Either<Nothing, T>` and
 * `Either<T, Nothing>` are assignable to any `Either<L, R>`.
 *
 * @param L the error type carried by [Left].
 * @param R the success value type carried by [Right].
 */
sealed class Either<out L, out R> {
    // Failure case, carrying the error value [value].
    data class Left<out L>(
        val value: L,
    ) : Either<L, Nothing>()

    // Success case, carrying the result value [value].
    data class Right<out R>(
        val value: R,
    ) : Either<Nothing, R>()
}

// Wraps [value] as a successful [Either.Right].
fun <R> success(value: R) = Either.Right(value)

// Wraps [error] as a failed [Either.Left].
fun <L> failure(error: L) = Either.Left(error)

// Alias for the success [Either.Right].
typealias Success<S> = Either.Right<S>

// Alias for the failure [Either.Left].
typealias Failure<F> = Either.Left<F>
