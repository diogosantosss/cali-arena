package com.caliarena.service.screen

import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.service.sse.ScreenRoutinesEvent

sealed interface ScreenEffect {
    data class Leaderboard(
        val leaderboard: BracketLeaderboard,
    ) : ScreenEffect

    data class Brackets(
        val summary: TournamentBracketsResponse,
    ) : ScreenEffect

    data class Routines(
        val events: List<ScreenRoutinesEvent>,
    ) : ScreenEffect

    data object None : ScreenEffect
}
