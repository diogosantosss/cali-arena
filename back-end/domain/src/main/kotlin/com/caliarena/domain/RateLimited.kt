package com.caliarena.domain

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimited(
    val maxRequests: Int,
    val windowSeconds: Int,
)
