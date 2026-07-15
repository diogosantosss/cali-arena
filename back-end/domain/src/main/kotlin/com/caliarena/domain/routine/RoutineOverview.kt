package com.caliarena.domain.routine

import java.math.BigDecimal
import java.time.Instant

data class RoutineOverview(
    val name: String,
    val timeCapSeconds: Int?,
    val createdAt: Instant,
    val exercises: List<Exercise>
)