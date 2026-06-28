package com.caliarena.domain.athlete

import java.time.Instant

data class Athlete(
    val id: Int,
    val name: String,
    val gender: GenderType,
    val clubId: Int,
    val createdAt: Instant,
)
