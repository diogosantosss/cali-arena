package com.caliarena.domain.bracket

import java.time.Instant

data class Bracket(
    val id: Int,
    val tournamentId: Int,
    val division: String,
    val stage: BracketStage,
    val createdAt: Instant,
)
