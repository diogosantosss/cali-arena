package com.caliarena.domain.tournament

import com.caliarena.domain.athlete.GenderType
import java.time.Instant

data class TournamentState(
    val id: Int,
    val tournamentId: Int,
    val currentScreen: ScreenState,
    val currentMatchId: Int?,
    val currentBracketId: Int?,
    val currentGender: GenderType?,
    val updatedAt: Instant,
)
