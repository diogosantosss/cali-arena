package com.caliarena.domain.club

import java.time.Instant

data class Club(
    val id: Int,
    val name: String,
    val shortName: String?,
    val createdAt: Instant,
)
