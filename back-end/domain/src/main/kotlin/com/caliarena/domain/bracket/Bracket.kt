package com.caliarena.domain.bracket

import com.caliarena.domain.athlete.GenderType
import java.time.Instant

data class Bracket(
    val id: Int,
    val tournamentId: Int,
    val gender: GenderType,
    val stage: BracketStage,
    val createdAt: Instant,
)
