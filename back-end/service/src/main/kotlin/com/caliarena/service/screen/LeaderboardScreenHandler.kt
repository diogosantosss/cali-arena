package com.caliarena.service.screen

import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.repo.trx.Transaction
import com.caliarena.service.mapper.BracketLeaderboardMapper
import jakarta.inject.Named

@Named
class LeaderboardScreenHandler(
    private val mapper: BracketLeaderboardMapper,
) : ScreenEffectHandler {
    override fun supports(screen: ScreenState) = screen == ScreenState.LEADERBOARD

    override fun handle(
        trx: Transaction,
        tournamentId: Int,
        state: TournamentState,
    ): ScreenEffect {
        val bracketId = state.currentBracketId ?: return ScreenEffect.None
        val leaderboard = mapper.build(trx, bracketId) ?: return ScreenEffect.None
        return ScreenEffect.Leaderboard(leaderboard)
    }
}
