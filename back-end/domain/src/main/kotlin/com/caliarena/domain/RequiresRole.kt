package com.caliarena.domain

import com.caliarena.domain.user.UserRole

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresRole(
    val allowedRoles: Array<UserRole>,
)