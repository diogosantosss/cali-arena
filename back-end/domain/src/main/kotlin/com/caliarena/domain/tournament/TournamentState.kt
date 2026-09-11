package com.caliarena.domain.tournament

import java.time.Instant

data class TournamentState(
    val id: Int,
    val tournamentId: Int,
    val currentScreen: ScreenState,
    val currentMatchId: Int?,
    val currentBracketId: Int?,
    val currentDivision: String?,
    val updatedAt: Instant,
)
