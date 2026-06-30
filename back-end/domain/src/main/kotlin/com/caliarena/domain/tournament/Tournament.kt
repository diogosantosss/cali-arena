package com.caliarena.domain.tournament

import java.time.Instant

data class Tournament(
    val id: Int,
    val name: String,
    val location: String?,
    val startDate: Instant?,
    val endDate: Instant?,
    val status: TournamentStatus,
    val createdAt: Instant,
)
