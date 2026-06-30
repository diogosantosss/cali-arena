package com.caliarena.domain.routine

import java.time.Instant

data class EnduranceRoutine(
    val id: Int,
    val name: String,
    val timeCapSeconds: Int?,
    val createdAt: Instant,
)
